package com.concordium.wallet.ui.passphrase.recoverprocess

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.AppConfig
import com.concordium.wallet.core.backend.BackendRequest
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.RecipientRepository
import com.concordium.wallet.data.backend.repository.IdentityProviderRepository
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.cryptolib.CreateCredentialInputV1
import com.concordium.wallet.data.cryptolib.CreateCredentialOutputV1
import com.concordium.wallet.data.cryptolib.GenerateRecoveryRequestInput
import com.concordium.wallet.data.cryptolib.StorageAccountData
import com.concordium.wallet.data.model.*
import com.concordium.wallet.data.preferences.AuthPreferences
import com.concordium.wallet.data.room.*
import com.concordium.wallet.ui.common.BackendErrorHandler
import com.concordium.wallet.ui.passphrase.recoverprocess.retrofit.IdentityProviderApiInstance
import com.concordium.wallet.util.DateTimeUtil
import com.concordium.wallet.util.increase
import com.google.gson.JsonArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.Serializable

data class RecoverProcessData(
    var identitiesWithAccounts: List<IdentityWithAccounts> = mutableListOf(),
    var noResponseFrom: MutableSet<String> = mutableSetOf()
): Serializable

class RecoverProcessViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        const val RECOVER_PROCESS_DATA = "RECOVER_PROCESS_DATA"
        const val STATUS_DONE = 1
        private const val IDENTITY_GAP_MAX = 20
        private const val ACCOUNT_GAP_MAX = 20
    }

    private val identityWithAccountsFound = mutableListOf<IdentityWithAccounts>()
    private var stop = false
    private var identityGaps: HashMap<String, Int> = HashMap()
    private var accountGaps: HashMap<Int, Int> = HashMap()
    private var password = ""
    private val net = AppConfig.net
    private val seed = AuthPreferences(getApplication()).getSeedPhrase()
    private var globalParamsRequest: BackendRequest<GlobalParamsWrapper>? = null
    private var identityProvidersRequest: BackendRequest<ArrayList<IdentityProvider>>? = null
    private var globalInfo: GlobalParamsWrapper? = null
    private var identityProviders: ArrayList<IdentityProvider>? = null

    val statusChanged: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val waiting: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val progressIdentities: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val progressAccounts: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val errorLiveData: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val recoverProcessData = RecoverProcessData()

    fun recoverIdentitiesAndAccounts(password: String) {
        this.password = password
        waiting.postValue(true)
        identityGaps = HashMap()
        getGlobalInfo()
    }

    private fun getGlobalInfo() {
        globalParamsRequest = ProxyRepository().getIGlobalInfo(
            { globalInfo ->
                this.globalInfo = globalInfo
                getIdentityProviderInfo()
            },
            {
                handleBackendError(it)
            }
        )
    }

    private fun getIdentityProviderInfo() {
        identityProvidersRequest = IdentityProviderRepository().getIdentityProviderInfo(
            { identityProviders ->
                this.identityProviders = identityProviders
                getIdentities()
            },
            {
                handleBackendError(it)
            }
        )
    }

    private fun getIdentities() {
        globalInfo?.let { globalInfo ->
            identityProviders?.forEach { identityProvider ->
                identityGaps[identityProvider.ipInfo.ipDescription.url] = 0
                if (!stop) {
                    viewModelScope.launch {
                        getIdentityFromProvider(identityProvider, globalInfo, 0)
                    }
                }
            }
        }
    }

    private suspend fun getIdentityFromProvider(identityProvider: IdentityProvider, globalInfo: GlobalParamsWrapper, identityIndex: Int) {
        if ((identityGaps[identityProvider.ipInfo.ipDescription.url] ?: IDENTITY_GAP_MAX) >= IDENTITY_GAP_MAX) {
            checkAllDone()
            return
        }

        val recoverRequestUrl = getRecoverRequestUrl(identityProvider, globalInfo, identityIndex)
        if (recoverRequestUrl != null) {
            val recoverResponsePair = IdentityProviderApiInstance.safeRecoverCall(recoverRequestUrl)
            if (recoverResponsePair.first && recoverResponsePair.second != null && recoverResponsePair.second!!.value != null) {
                val identity = saveIdentity(recoverResponsePair.second!!.value!!, identityProvider, identityIndex)
                CoroutineScope(Dispatchers.IO).launch {
                    getAccounts(identity)
                }
            }
            else {
                if (!recoverResponsePair.first)
                    recoverProcessData.noResponseFrom.add(identityProvider.ipInfo.ipDescription.name)
                identityGaps.increase(identityProvider.ipInfo.ipDescription.url)
            }
        } else {
            identityGaps.increase(identityProvider.ipInfo.ipDescription.url)
        }

        checkAllDone()

        if (!stop)
            getIdentityFromProvider(identityProvider, globalInfo, identityIndex + 1)
    }

    private suspend fun getAccounts(identity: Identity) {
        globalInfo?.let { globalInfo ->
            accountGaps[identity.id] = 0
            if (!stop) {
                recoverAccount(identity, globalInfo, 0)
            }
        }
    }

    private fun handleBackendError(throwable: Throwable) {
        errorLiveData.postValue(BackendErrorHandler.getExceptionStringRes(throwable))
    }

    fun stop() {
        stop = true
        globalParamsRequest?.dispose()
        identityProvidersRequest?.dispose()
    }

    private suspend fun getRecoverRequestUrl(identityProvider: IdentityProvider, globalInfo: GlobalParamsWrapper, identityIndex: Int): String? {
        val recoveryRequestInput = GenerateRecoveryRequestInput(
            identityProvider.ipInfo,
            globalInfo.value,
            seed,
            net,
            identityIndex,
            System.currentTimeMillis() / 1000
        )

        val output = App.appCore.cryptoLibrary.generateRecoveryRequest(recoveryRequestInput)

        if (output != null && identityProvider.metadata.recoveryStart != null && identityProvider.metadata.recoveryStart.isNotBlank()) {
            val encoded = Uri.encode(output)
            val urlFromIpInfo = "${identityProvider.metadata.recoveryStart}?state="
            return "$urlFromIpInfo$encoded"
        }

        return null
    }

    private suspend fun saveIdentity(identityObject: IdentityObject, identityProvider: IdentityProvider, identityIndex: Int): Identity {
        val identity = Identity(
            0,
            "Identity $identityIndex",
            IdentityStatus.DONE,
            "",
            "",
            0, // Next account number is set to 0, because we don't have any account yet
            identityProvider,
            identityObject,
            identityProvider.ipInfo.ipIdentity,
            identityIndex
        )
        val identityRepository = IdentityRepository(WalletDatabase.getDatabase(getApplication()).identityDao())
        val existingIdentity = identityRepository.findByProviderIdAndIndex(identityProvider.ipInfo.ipIdentity, identityIndex)
        if (existingIdentity == null) {
            val newIdentityId = identityRepository.insert(identity)
            identity.id = newIdentityId.toInt()
            identityWithAccountsFound.add(IdentityWithAccounts(identity, mutableListOf()))
            return identity
        }
        return existingIdentity
    }

    private suspend fun recoverAccount(identity: Identity, globalInfo: GlobalParamsWrapper, accountIndex: Int) {
        if ((accountGaps[identity.id] ?: ACCOUNT_GAP_MAX) >= ACCOUNT_GAP_MAX) {
            checkAllDone()
            return
        }

        var createCredentialOutput: CreateCredentialOutputV1? = null
        if (identity.identityObject != null) {
            val credentialInput = CreateCredentialInputV1(
                identity.identityProvider.ipInfo,
                identity.identityProvider.arsInfos,
                globalInfo.value,
                identity.identityObject!!,
                JsonArray(),
                seed,
                net,
                identity.identityIndex,
                accountIndex,
                (DateTimeUtil.nowPlusMinutes(5).time) / 1000
            )
            createCredentialOutput = App.appCore.cryptoLibrary.createCredentialV1(credentialInput)
        }

        if (createCredentialOutput == null)
            return

        val jsonToBeEncrypted = App.appCore.gson.toJson(
            StorageAccountData(
                accountAddress = createCredentialOutput.accountAddress,
                accountKeys = createCredentialOutput.accountKeys,
                encryptionSecretKey = createCredentialOutput.encryptionSecretKey,
                commitmentsRandomness = createCredentialOutput.commitmentsRandomness
            )
        )
        val encryptedAccountData = App.appCore.getCurrentAuthenticationManager().encryptInBackground(password, jsonToBeEncrypted)
            ?: return

        try {
            val accountBalance = ProxyRepository().getAccountBalanceSuspended(createCredentialOutput.accountAddress)
            if (accountBalance.finalizedBalance != null) {
                val account = Account(
                    id = 0,
                    identityId = identity.id,
                    name = "Account ${identity.nextAccountNumber}",
                    address = createCredentialOutput.accountAddress,
                    submissionId = "",
                    transactionStatus = TransactionStatus.FINALIZED,
                    encryptedAccountData = encryptedAccountData,
                    revealedAttributes = listOf(),
                    credential = createCredentialOutput.credential,
                    finalizedBalance = accountBalance.finalizedBalance.accountAmount.toLong(),
                    currentBalance = accountBalance.currentBalance?.accountAmount?.toLong() ?: 0,
                    totalBalance = 0,
                    totalUnshieldedBalance = accountBalance.finalizedBalance.accountAmount.toLong(),
                    totalShieldedBalance = 0,
                    finalizedEncryptedBalance = accountBalance.finalizedBalance.accountEncryptedAmount,
                    currentEncryptedBalance = accountBalance.currentBalance?.accountEncryptedAmount,
                    encryptedBalanceStatus = ShieldedAccountEncryptionStatus.ENCRYPTED,
                    totalStaked = if (accountBalance.finalizedBalance.accountBaker != null) accountBalance.finalizedBalance.accountBaker.stakedAmount.toLong() else 0,
                    totalAtDisposal = 0,
                    readOnly = false,
                    finalizedAccountReleaseSchedule = accountBalance.finalizedBalance.accountReleaseSchedule,
                    bakerId = accountBalance.finalizedBalance.accountBaker?.bakerId?.toLong(),
                    accountDelegation = accountBalance.finalizedBalance.accountDelegation,
                    accountBaker = accountBalance.finalizedBalance.accountBaker,
                    accountIndex = accountBalance.finalizedBalance.accountIndex
                )

                val accountRepository = AccountRepository(WalletDatabase.getDatabase(getApplication()).accountDao())
                if (accountRepository.findByAddress(account.address) == null) {
                    accountRepository.insertAccountAndCountUpNextAccountNumber(account)
                    val recipientRepository = RecipientRepository(WalletDatabase.getDatabase(getApplication()).recipientDao())
                    if (recipientRepository.getRecipientByAddress(account.address) == null) {
                        recipientRepository.insert(Recipient(0, account.name, account.address))
                    }
                    val iWithAFound = identityWithAccountsFound.firstOrNull { it.identity.identityProviderId == identity.identityProviderId && it.identity.identityIndex == identity.identityIndex }
                    if (iWithAFound != null)
                        iWithAFound.accounts.add(account)
                    else
                        identityWithAccountsFound.add(IdentityWithAccounts(identity, mutableListOf(account)))
                }
            } else {
                accountGaps.increase(identity.id)
            }
        } catch (ex: Exception) {
            stop = true
            handleBackendError(ex)
        }

        checkAllDone()

        if (!stop)
            recoverAccount(identity, globalInfo, accountIndex + 1)
    }

    private fun identitiesPercent(): Int {
        if (identityGaps.size == 0) {
            progressIdentities.postValue(0)
            return 0
        }
        var identities = 0
        for (gap in identityGaps.values) {
            identities += IDENTITY_GAP_MAX - gap
        }
        val total = identityGaps.size * IDENTITY_GAP_MAX
        identities = total - identities
        val percent = (identities * 100) / total
        progressIdentities.postValue(percent)
        return percent
    }

    private fun accountsPercent(): Int {
        if (accountGaps.size == 0) {
            progressAccounts.postValue(0)
            return 0
        }
        var accounts = 0
        for (gap in accountGaps.values) {
            accounts += ACCOUNT_GAP_MAX - gap
        }
        val total = accountGaps.size * ACCOUNT_GAP_MAX
        accounts = total - accounts
        val percent = (accounts * 100) / total
        progressAccounts.postValue(percent)
        return percent
    }

    private fun checkAllDone() {
        val identitiesPercent = identitiesPercent()
        val accountsPercent = accountsPercent()
        if ((identitiesPercent >= 100 && accountsPercent >= 100) || (identitiesPercent >= 100 && accountGaps.size == 0))  {
            recoverProcessData.identitiesWithAccounts = identityWithAccountsFound
            waiting.postValue(false)
            statusChanged.postValue(STATUS_DONE)
        }
    }
}
