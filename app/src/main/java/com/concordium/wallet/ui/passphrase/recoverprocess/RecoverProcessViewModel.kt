package com.concordium.wallet.ui.passphrase.recoverprocess

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.backend.repository.IdentityProviderRepository
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.cryptolib.*
import com.concordium.wallet.data.model.*
import com.concordium.wallet.data.preferences.AuthPreferences
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.data.room.IdentityWithAccounts
import com.concordium.wallet.data.room.WalletDatabase
import com.concordium.wallet.util.DateTimeUtil
import com.google.gson.JsonArray
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    }

    var identitiesWithAccounts: List<IdentityWithAccounts> = mutableListOf()
    val statusChanged: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val waiting: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }

    private val identityDao = WalletDatabase.getDatabase(getApplication()).identityDao()
    private val identityRepository = IdentityRepository(identityDao)
    private val accountDao = WalletDatabase.getDatabase(getApplication()).accountDao()
    private val accountRepository = AccountRepository(accountDao)
    private val proxyRepository = ProxyRepository()

    fun recoverIdentitiesAndAccounts(password: String) {
        val net = "Mainnet"
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
                getIdentityFromProvider(identityProvider, globalInfo, seed, net, identityProviderService, identityIndex)
            }

            identityRepository.getAllDone().forEach { doneIdentity ->
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
        val urlFromIpInfo = "https://id-service.stagenet.concordium.com/api/v1/recover?state=" // TODO must come from ip_info endpoint

        if (output != null) {
            val encoded = Uri.encode(output)
            return "$urlFromIpInfo$encoded"
        }

        return null
    }

    private suspend fun getIdentityFromProvider(identityProvider: IdentityProvider, globalInfo: GlobalParamsWrapper, seed: String, net: String, identityProviderService: IdentityProviderApi, identityIndex: Int) {
        val recoverRequestUrl = getRecoverRequestUrl(identityProvider, globalInfo, seed, net, identityIndex)
        val recoverInfo = identityProviderService.recover(recoverRequestUrl)
        val identityTokenContainer = identityProviderService.identity(recoverInfo.identityRetrievalUrl)
        if (identityTokenContainer.token != null) {
            saveIdentity(identityTokenContainer, identityProvider, identityIndex)
            getIdentityFromProvider(identityProvider, globalInfo, seed, net, identityProviderService, identityIndex + 1)
        }
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
            identityTokenContainer.token?.identityObject?.value,
            ""
        )
        identityRepository.insert(identity)
    }

    private suspend fun recoverAccount(password: String, seed: String, net: String, identityId: Int, globalInfo: GlobalParamsWrapper) {

        val identity = identityRepository.findById(identityId) ?: return

        //val identityKeysAndRandomnessInput = IdentityKeysAndRandomnessInput(seed, net, identity.id)
        //val identityKeysAndRandomnessOutput = App.appCore.cryptoLibrary.getIdentityKeysAndRandomness(identityKeysAndRandomnessInput)

        //println("LC -> identityKeysAndRandomnessOutput = $identityKeysAndRandomnessOutput")

        identity.identityObject?.let { identityObject ->
            val credentialInput = CreateCredentialInputV1(
                identity.identityProvider.ipInfo,
                identity.identityProvider.arsInfos,
                globalInfo.value,
                identityObject,
                JsonArray(),
                seed,
                net,
                identity.id,
                identity.nextAccountNumber,
                (DateTimeUtil.nowPlusMinutes(5).time) / 1000
            )

            App.appCore.cryptoLibrary.createCredentialV1(credentialInput)?.let { createCredentialOutput ->

                println("LC -> createCredentialOutput = $createCredentialOutput")

                proxyRepository.getAccountBalanceSuspended(createCredentialOutput.accountAddress).let { accountBalance ->

                    println("LC -> accountBalance = $accountBalance   ${createCredentialOutput.accountAddress}")

                    val accountKeysAndRandomnessInput = AccountKeysAndRandomnessInput(seed, net, identity.id, createCredentialOutput.credential.v)
                    App.appCore.cryptoLibrary.getAccountKeysAndRandomness(accountKeysAndRandomnessInput)?.let { accountKeysAndRandomnessOutput2 ->

                        println("LC -> accountKeysAndRandomnessOutput = $accountKeysAndRandomnessOutput2")

                        // identityKeysAndRandomnessOutput.prfKey
                        // accountKeysAndRandomnessOutput.signKey

                        val jsonToBeEncrypted = App.appCore.gson.toJson(
                            StorageAccountData(
                                accountAddress = createCredentialOutput.accountAddress,
                                accountKeys = createCredentialOutput.accountKeys,
                                encryptionSecretKey = createCredentialOutput.encryptionSecretKey,
                                commitmentsRandomness = createCredentialOutput.commitmentsRandomness
                            )
                        )

                        val encryptedAccountData = App.appCore.getCurrentAuthenticationManager().encryptInBackground(password, jsonToBeEncrypted)

                        println("LC -> encryptedAccountData = $encryptedAccountData")

                        val account = Account(
                            id = identity.nextAccountNumber,
                            identityId = identity.id,
                            name = "Account ${identity.nextAccountNumber}",
                            address = createCredentialOutput.accountAddress,
                            submissionId = "",
                            transactionStatus = TransactionStatus.FINALIZED,
                            encryptedAccountData = encryptedAccountData ?: "",
                            revealedAttributes = listOf(),
                            credential = createCredentialOutput.credential,
                            finalizedBalance = accountBalance.finalizedBalance?.accountAmount?.toLong() ?: 0,
                            currentBalance = accountBalance.currentBalance?.accountAmount?.toLong() ?: 0,
                            totalBalance = 0,
                            totalUnshieldedBalance = accountBalance.finalizedBalance?.accountAmount?.toLong() ?: 0,
                            totalShieldedBalance = 0,
                            finalizedEncryptedBalance = accountBalance.finalizedBalance?.accountEncryptedAmount,
                            currentEncryptedBalance = accountBalance.currentBalance?.accountEncryptedAmount,
                            encryptedBalanceStatus = ShieldedAccountEncryptionStatus.ENCRYPTED,
                            totalStaked = if (accountBalance.finalizedBalance?.accountBaker != null) accountBalance.finalizedBalance.accountBaker.stakedAmount.toLong() else
                                if (accountBalance.finalizedBalance?.accountDelegation != null) accountBalance.finalizedBalance.accountDelegation.stakedAmount.toLong() else 0,
                            totalAtDisposal = 0,
                            readOnly = false,
                            finalizedAccountReleaseSchedule = accountBalance.finalizedBalance?.accountReleaseSchedule,
                            bakerId = accountBalance.finalizedBalance?.accountBaker?.bakerId?.toLong(),
                            accountDelegation = accountBalance.finalizedBalance?.accountDelegation,
                            accountBaker = accountBalance.finalizedBalance?.accountBaker,
                            accountIndex = accountBalance.finalizedBalance?.accountIndex
                        )

                        println("LC -> account = $account")

                        accountRepository.insertAccountAndCountUpNextAccountNumber(account)

                        //recoverAccount(password, seed, net, identityId, globalInfo)


                    }
                }
            }
        }
    }
}
