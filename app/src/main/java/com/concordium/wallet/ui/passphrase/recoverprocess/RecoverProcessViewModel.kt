package com.concordium.wallet.ui.passphrase.recoverprocess

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.backend.repository.IdentityProviderRepository
import com.concordium.wallet.data.cryptolib.GenerateRecoveryRequestInput
import com.concordium.wallet.data.model.*
import com.concordium.wallet.data.preferences.AuthPreferences
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.data.room.IdentityWithAccounts
import com.concordium.wallet.data.room.WalletDatabase
import kotlinx.coroutines.*
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

    fun recoverIdentitiesAndAccounts() {
        val net = "Mainnet"
        val seed = AuthPreferences(getApplication()).getSeedPhrase()
        val repository = IdentityProviderRepository()
        val retrofit = Retrofit.Builder().baseUrl("https://some.api.url/").addConverterFactory(GsonConverterFactory.create()).build()
        val identityProviderService = retrofit.create(IdentityProviderApi::class.java)

        viewModelScope.launch {
            waiting.value = true

            withContext(kotlin.coroutines.coroutineContext) {
                val globalInfo = repository.getGlobalInfoSuspended()
                val identityProviders = repository.getIdentityProviderInfoSuspended()
                identityProviders.forEach { identityProvider ->
                    val identityIndex = 1
                    println("LC -> STEP 0 -> ${identityProvider.ipInfo.ipDescription.name}")
                    getIdentityFromProvider(identityProvider, globalInfo, seed, net, identityProviderService, identityIndex)
                }
            }

            val data = mutableListOf<IdentityWithAccounts>()
            identityRepository.getAll().forEach { identity ->
                data.add(IdentityWithAccounts(identity, listOf()))
            }
            identitiesWithAccounts = data

            waiting.value = false

            if (data.count() > 1)
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
}
