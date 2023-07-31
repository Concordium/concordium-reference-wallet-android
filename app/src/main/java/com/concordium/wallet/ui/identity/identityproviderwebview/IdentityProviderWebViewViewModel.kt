package com.concordium.wallet.ui.identity.identityproviderwebview

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.core.arch.Event
import com.concordium.wallet.core.backend.BackendError
import com.concordium.wallet.core.backend.BackendErrorException
import com.concordium.wallet.core.backend.BackendRequest
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.RecipientRepository
import com.concordium.wallet.data.backend.repository.IdentityProviderRepository
import com.concordium.wallet.data.model.AttributeList
import com.concordium.wallet.data.model.IdentityContainer
import com.concordium.wallet.data.model.IdentityCreationData
import com.concordium.wallet.data.model.IdentityObject
import com.concordium.wallet.data.model.IdentityRequest
import com.concordium.wallet.data.model.IdentityStatus
import com.concordium.wallet.data.model.PreIdentityContainer
import com.concordium.wallet.data.model.PreIdentityObject
import com.concordium.wallet.data.model.PubInfoForIp
import com.concordium.wallet.data.model.RawJson
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.data.room.WalletDatabase
import com.concordium.wallet.ui.common.BackendErrorHandler
import com.concordium.wallet.util.Log
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Response


class IdentityProviderWebViewViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        const val CALLBACK_URL = BuildConfig.SCHEME + "://identity-issuer/callback"
    }

    private val _waitingLiveData = MutableLiveData<Boolean>()
    val waitingLiveData: LiveData<Boolean>
        get() = _waitingLiveData

    private val _errorLiveData = MutableLiveData<Event<Int>>()
    val errorLiveData: LiveData<Event<Int>>
        get() = _errorLiveData

    private val _identityCreationError = MutableLiveData<Event<String>>()
    val identityCreationError: LiveData<Event<String>>
        get() = _identityCreationError

    private val _identityCreationUserCancel = MutableLiveData<Event<String>>()
    val identityCreationUserCancel: LiveData<Event<String>>
        get() = _identityCreationUserCancel

    private val _gotoIdentityConfirmedLiveData = MutableLiveData<Event<Identity>>()
    val gotoIdentityConfirmedLiveData: LiveData<Event<Identity>>
        get() = _gotoIdentityConfirmedLiveData

    private val _gotoFailedLiveData = MutableLiveData<Event<Pair<Boolean, BackendError?>>>()
    val gotoFailedLiveData: LiveData<Event<Pair<Boolean, BackendError?>>>
        get() = _gotoFailedLiveData

    private val _createIdentity = MutableLiveData(false)
    val createIdentity: LiveData<Boolean>
        get() = _createIdentity

    private val _createIdentityError = MutableLiveData(CreateIdentityError.NONE)
    val createIdentityError: LiveData<CreateIdentityError>
        get() = _createIdentityError

    lateinit var identityCreationData: IdentityCreationData
    private val gson = App.appCore.gson
    private val identityRepository: IdentityRepository
    private val recipientRepository: RecipientRepository
    private val repository: IdentityProviderRepository = IdentityProviderRepository()
    private var identityRequest: BackendRequest<IdentityContainer>? = null
    val useTemporaryBackend = BuildConfig.USE_BACKEND_MOCK

    init {
        val identityDao = WalletDatabase.getDatabase(application).identityDao()
        identityRepository = IdentityRepository(identityDao)
        val recipientDao = WalletDatabase.getDatabase(application).recipientDao()
        recipientRepository = RecipientRepository(recipientDao)
        _waitingLiveData.value = true
    }

    fun initialize(tempData: IdentityCreationData) {
        this.identityCreationData = tempData
        if (useTemporaryBackend) {
            getIdentityObjectFromProvider(
                IdentityRequest(
                    tempData.idObjectRequest
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        identityRequest?.dispose()
    }

    private fun handleBackendError(throwable: Throwable) {
        Log.e("Backend request failed", throwable)
        if (throwable is BackendErrorException) {
            _gotoFailedLiveData.value = Event(Pair(true, throwable.error))
        } else {
            _errorLiveData.value = Event(BackendErrorHandler.getExceptionStringRes(throwable))
        }
    }

    fun getIdentityProviderUrl(): String {
        val idObjectRequest = gson.toJson(IdentityRequest(identityCreationData.idObjectRequest))
        val baseUrl = identityCreationData.identityProvider.metadata.issuanceStart
        val delimiter = if (baseUrl.contains('?')) "&" else "?"
        return if (BuildConfig.DEBUG && BuildConfig.ENV_NAME == "prod_testnet" && baseUrl.lowercase()
                .contains("notabene")
        )
            "${baseUrl}${delimiter}response_type=code&redirect_uri=$CALLBACK_URL&scope=identity&state=$idObjectRequest&test_flow=1"
        else
            "${baseUrl}${delimiter}response_type=code&redirect_uri=$CALLBACK_URL&scope=identity&state=$idObjectRequest"
    }

    private fun saveNewIdentity(identityObject: IdentityObject) {
        val identity = Identity(
            0,
            identityCreationData.identityName,
            IdentityStatus.DONE,
            "",
            "",
            identityCreationData.identityProvider,
            identityObject,
            identityCreationData.identityProvider.ipInfo.ipIdentity,
            identityCreationData.identityIndex
        )
        saveNewIdentity(identity)
    }

    private fun saveNewIdentity(identity: Identity) = viewModelScope.launch {
        val identityId = identityRepository.insert(identity)
        identity.id = identityId.toInt()
        _gotoIdentityConfirmedLiveData.value = Event(identity)
    }

    fun parseIdentityAndSavePending(callbackUri: String) {
        val idObjectRequest = gson.fromJson(
            identityCreationData.idObjectRequest.json,
            PreIdentityContainer::class.java
        )
        val pubInfoForIP = PubInfoForIp("", RawJson("{}"), "")
        val preIdentityObject =
            PreIdentityObject(
                RawJson("{}"),
                pubInfoForIP,
                "",
                RawJson("{}"),
                "",
                RawJson("{}"),
                "",
                idObjectRequest.value.idCredPub
            )
        val identity = Identity(
            0,
            identityCreationData.identityName,
            IdentityStatus.PENDING,
            "",
            callbackUri,
            identityCreationData.identityProvider,
            IdentityObject(
                AttributeList(HashMap(), "", 0, "0"),
                preIdentityObject,
                RawJson("{}")
            ),
            identityCreationData.identityProvider.ipInfo.ipIdentity,
            identityCreationData.identityIndex
        )
        saveNewIdentity(identity)
    }

    fun parseIdentityAndSave(identity: String) {
        val identityContainer = gson.fromJson(identity, IdentityContainer::class.java)
        saveNewIdentity(identityContainer.value)
    }

    fun parseIdentityError(errorContent: String) {
        val error: Map<String, Any> =
            gson.fromJson(errorContent, object : TypeToken<Map<String, Any>>() {}.type)
        val map = error["error"] as Map<*, *>
        val event = Event(map["detail"].toString())
        if (map["code"]!! == "USER_CANCEL") {
            _identityCreationUserCancel.value = event
        } else {
            _identityCreationError.value = event
        }
    }

    private fun getIdentityObjectFromProvider(request: IdentityRequest) {
        _waitingLiveData.value = true
        identityRequest?.dispose()
        identityRequest = repository.requestIdentity(request,
            {
                saveNewIdentity(it.value)
                _waitingLiveData.value = false
            },
            {
                _waitingLiveData.value = false
                handleBackendError(it)
            }
        )
    }

    fun startIdentityCreation() {
        viewModelScope.launch(Dispatchers.IO) {
            App.appCore.getProxyBackend().checkIdentityProvider(getIdentityProviderUrl())
                .enqueue(object : retrofit2.Callback<String> {
                    override fun onResponse(call: Call<String>, response: Response<String>) {
                        val response = response.errorBody()?.string()
                        when {
                            response?.contains(CreateIdentityError.NONE.message) == true -> {
                                _createIdentityError.value = CreateIdentityError.NONE
                                _createIdentity.value = true
                            }

                            response?.contains(CreateIdentityError.ID_PUB.message) == true -> _createIdentityError.value =
                                CreateIdentityError.ID_PUB

                            else -> {
                                _createIdentityError.value = CreateIdentityError.UNKNOWN
                                _createIdentity.value = true
                            }
                        }
                    }

                    override fun onFailure(call: Call<String>, t: Throwable) {
                        _createIdentityError.value = CreateIdentityError.UNKNOWN
                        _createIdentity.value = true
                    }
                })
        }
    }
}
