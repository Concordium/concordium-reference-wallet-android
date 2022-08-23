package com.concordium.wallet.ui.passphrase.recoverprocess

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.AppConfig
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.backend.repository.IdentityProviderRepository
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.cryptolib.CreateCredentialInputV1
import com.concordium.wallet.data.cryptolib.CreateCredentialOutputV1
import com.concordium.wallet.data.cryptolib.GenerateRecoveryRequestInput
import com.concordium.wallet.data.cryptolib.StorageAccountData
import com.concordium.wallet.data.model.*
import com.concordium.wallet.data.preferences.AuthPreferences
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.data.room.IdentityWithAccounts
import com.concordium.wallet.data.room.WalletDatabase
import com.concordium.wallet.util.DateTimeUtil
import com.google.gson.JsonArray
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Url
import java.io.Serializable

interface IdentityProviderApi {
    @GET suspend fun recover(@Url url: String?): RecoverResponse
    @GET suspend fun identity(@Url url: String?): IdentityTokenContainer
}

class RecoverProcessViewModel(application: Application) : AndroidViewModel(application), Serializable {
    companion object {
        const val RECOVER_PROCESS_DATA = "RECOVER_PROCESS_DATA"
        const val STATUS_OK = 1
        const val STATUS_NOTHING_TO_RECOVER = 2
        private var IDENTITY_GAP_MAX = 20
        private var ACCOUNT_GAP_MAX = 20
    }

    var identitiesWithAccounts: List<IdentityWithAccounts> = mutableListOf()
    val statusChanged: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val waiting: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }

    private val identityDao = WalletDatabase.getDatabase(getApplication()).identityDao()
    private val identityRepository = IdentityRepository(identityDao)
    private val accountDao = WalletDatabase.getDatabase(getApplication()).accountDao()
    private val accountRepository = AccountRepository(accountDao)
    private val proxyRepository = ProxyRepository()

    private var identityGap = 0
    private var accountGap = 0

    fun recoverIdentitiesAndAccounts(password: String) {
        if (BuildConfig.DEBUG) {
            IDENTITY_GAP_MAX = 1
            ACCOUNT_GAP_MAX = 1
        }

        val net = AppConfig.net
        val seed = AuthPreferences(getApplication()).getSeedPhrase()
        val repository = IdentityProviderRepository()
        val retrofit = Retrofit.Builder().baseUrl("https://some.api.url/").addConverterFactory(GsonConverterFactory.create()).build()
        val identityProviderService = retrofit.create(IdentityProviderApi::class.java)

        viewModelScope.launch {
            waiting.value = true

            val globalInfo = repository.getGlobalInfoSuspended()

            val identityProviders = repository.getIdentityProviderInfoSuspended()
            identityProviders.forEach { identityProvider ->
                val identityIndex = 1
                identityGap = 0
                getIdentityFromProvider(identityProvider, globalInfo, seed, net, identityProviderService, identityIndex)
            }

            identityRepository.getAllDone().forEach { doneIdentity ->
                accountGap = 0
                recoverAccount(password, seed, net, doneIdentity.id, globalInfo)
            }

            val data = mutableListOf<IdentityWithAccounts>()
            identityRepository.getAll().forEach { identity ->
                val accounts = accountRepository.getAllByIdentityId(identity.id)
                data.add(IdentityWithAccounts(identity, accounts))
            }
            identitiesWithAccounts = data

            waiting.value = false

            if (data.isNotEmpty())
                statusChanged.value = STATUS_OK
            else
                statusChanged.value = STATUS_NOTHING_TO_RECOVER
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

    private suspend fun getIdentityFromProvider(identityProvider: IdentityProvider, globalInfo: GlobalParamsWrapper, seed: String, net: String, identityProviderService: IdentityProviderApi, identityIndex: Int) {
        if (identityGap >= IDENTITY_GAP_MAX) {
            return
        }

        val recoverRequestUrl = getRecoverRequestUrl(identityProvider, globalInfo, seed, net, identityIndex)
        if (recoverRequestUrl != null) {
            val recoverInfo = identityProviderService.recover(recoverRequestUrl)
            val identityTokenContainer = identityProviderService.identity(recoverInfo.identityRetrievalUrl)
            if (identityTokenContainer.token != null) {
                saveIdentity(identityTokenContainer, identityProvider, identityIndex)
            } else {
                identityGap++
            }
        } else {
            identityGap++
        }

        getIdentityFromProvider(identityProvider, globalInfo, seed, net, identityProviderService, identityIndex + 1)
    }

    private suspend fun saveIdentity(identityTokenContainer: IdentityTokenContainer, identityProvider: IdentityProvider, id: Int) {
        val identity = Identity(
            id,
            "Identity $id",
            identityTokenContainer.status,
            identityTokenContainer.detail,
            "",
            1, // Next account number is set to 1, because we don't have any account yet
            identityProvider,
            identityTokenContainer.token?.identityObject?.value
        )
        identityRepository.insert(identity)
    }

    private suspend fun recoverAccount(password: String, seed: String, net: String, identityId: Int, globalInfo: GlobalParamsWrapper) {
        if (accountGap >= ACCOUNT_GAP_MAX) {
            return
        }

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
                identity.id,
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

        val accountBalance = proxyRepository.getAccountBalanceSuspended(createCredentialOutput.accountAddress)

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

            accountRepository.insertAccountAndCountUpNextAccountNumber(account)
        } else {
            accountGap++
        }

        recoverAccount(password, seed, net, identityId, globalInfo)
    }
}
