package com.concordium.wallet.ui.passphrase.recoverprocess

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.AppConfig
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
import com.concordium.wallet.ui.passphrase.recoverprocess.retrofit.IdentityProviderApiInstance
import com.concordium.wallet.util.DateTimeUtil
import com.google.gson.JsonArray
import kotlinx.coroutines.launch
import java.io.Serializable
import kotlin.math.max

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

    private var identityGap = 0
    private var accountGap = 0
    val statusChanged: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val waiting: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val recoverProcessData = RecoverProcessData()
    val progress: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    private var progressValue = 0
    private var step = 0
    private val identityWithAccountsFound = mutableListOf<IdentityWithAccounts>()

    fun recoverIdentitiesAndAccounts(password: String) {
        val net = AppConfig.net
        val seed = AuthPreferences(getApplication()).getSeedPhrase()
        val repository = IdentityProviderRepository()

        viewModelScope.launch {
            waiting.value = true
            progress.value = 0

            val globalInfo = repository.getGlobalInfoSuspended()
            val identityProviders = repository.getIdentityProviderInfoSuspended()
            step = 500 / (max(identityProviders.size, 1) * IDENTITY_GAP_MAX)

            identityProviders.forEach { identityProvider ->
                identityGap = 0
                val identityIndex = 0
                getIdentityFromProvider(identityProvider, globalInfo, seed, net, identityIndex)
            }

            setProgress(1000, 500)

            val identityRepository = IdentityRepository(WalletDatabase.getDatabase(getApplication()).identityDao())
            val allIdentitiesFound = identityRepository.getAllDone()
            step = 500 / (max(allIdentitiesFound.size, 1) * ACCOUNT_GAP_MAX)

            allIdentitiesFound.forEach { identityFound ->
                accountGap = 0
                recoverAccount(password, seed, net, identityFound.id, globalInfo)
            }

            recoverProcessData.identitiesWithAccounts = identityWithAccountsFound

            setProgress(1000, 1000)
            waiting.value = false

            statusChanged.value = STATUS_DONE
        }
    }

    private suspend fun getRecoverRequestUrl(identityProvider: IdentityProvider, globalInfo: GlobalParamsWrapper, seed: String, net: String, identityIndex: Int): String? {
        val recoveryRequestInput = GenerateRecoveryRequestInput(
            identityProvider.ipInfo,
            globalInfo.value,
            seed,
            net,
            identityIndex,
            System.currentTimeMillis() / 1000
        )

        val output = App.appCore.cryptoLibrary.generateRecoveryRequest(recoveryRequestInput)

        if (output != null && identityProvider.metadata.recoveryStart != null) {
            val encoded = Uri.encode(output)
            val urlFromIpInfo = "${identityProvider.metadata.recoveryStart}?state="
            return "$urlFromIpInfo$encoded"
        }

        return null
    }

    private suspend fun getIdentityFromProvider(identityProvider: IdentityProvider, globalInfo: GlobalParamsWrapper, seed: String, net: String, identityIndex: Int) {
        if (identityGap >= IDENTITY_GAP_MAX) {
            return
        }

        val recoverRequestUrl = getRecoverRequestUrl(identityProvider, globalInfo, seed, net, identityIndex)
        if (recoverRequestUrl != null) {
            val recoverResponse = IdentityProviderApiInstance.safeRecoverCall(recoverRequestUrl)
            if (recoverResponse != null) {
                if (recoverResponse.identityRetrievalUrl.isNotBlank()) {
                    val identityTokenContainer = IdentityProviderApiInstance.safeIdentityCall(recoverResponse.identityRetrievalUrl)
                    if (identityTokenContainer != null) {
                        if (identityTokenContainer.token != null) {
                            saveIdentity(identityTokenContainer, identityProvider, identityIndex)
                        } else {
                            identityGap++
                        }
                    } else {
                        recoverProcessData.noResponseFrom.add(identityProvider.ipInfo.ipDescription.name)
                        identityGap++
                    }
                } else {
                    identityGap++
                }
            } else {
                recoverProcessData.noResponseFrom.add(identityProvider.ipInfo.ipDescription.name)
                identityGap++
            }
        } else {
            identityGap++
        }

        setProgress(step, 500)
        getIdentityFromProvider(identityProvider, globalInfo, seed, net, identityIndex + 1)
    }

    private suspend fun saveIdentity(identityTokenContainer: IdentityTokenContainer, identityProvider: IdentityProvider, identityIndex: Int) {
        val identity = Identity(
            0,
            "Identity $identityIndex",
            identityTokenContainer.status,
            identityTokenContainer.detail,
            "",
            0, // Next account number is set to 0, because we don't have any account yet
            identityProvider,
            identityTokenContainer.token?.identityObject?.value,
            identityProvider.ipInfo.ipIdentity,
            identityIndex
        )
        val identityRepository = IdentityRepository(WalletDatabase.getDatabase(getApplication()).identityDao())
        if (identityRepository.findByProviderIdAndIndex(identityProvider.ipInfo.ipIdentity, identityIndex) == null) {
            identityRepository.insert(identity)
            identityWithAccountsFound.add(IdentityWithAccounts(identity, mutableListOf()))
        }
    }

    private suspend fun recoverAccount(password: String, seed: String, net: String, identityId: Int, globalInfo: GlobalParamsWrapper) {
        if (accountGap >= ACCOUNT_GAP_MAX) {
            return
        }

        val identityRepository = IdentityRepository(WalletDatabase.getDatabase(getApplication()).identityDao())
        val identity = identityRepository.findById(identityId)
            ?: return

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
                identity.id - 1,
                identity.nextAccountNumber,
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
            accountGap++
        }

        setProgress(step, 1000)
        recoverAccount(password, seed, net, identityId, globalInfo)
    }

    private fun setProgress(step: Int, maxValue: Int) {
        if (progressValue + step <= maxValue)
            progressValue += step
        else
            progressValue = maxValue
        progress.value = progressValue
    }
}
