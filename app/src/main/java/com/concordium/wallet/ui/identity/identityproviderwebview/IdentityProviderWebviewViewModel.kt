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
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.RecipientRepository
import com.concordium.wallet.data.backend.repository.IdentityProviderRepository
import com.concordium.wallet.data.model.*
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.data.room.Recipient
import com.concordium.wallet.data.room.WalletDatabase
import com.concordium.wallet.ui.common.BackendErrorHandler
import com.concordium.wallet.util.Log
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch

data class GotoIdentityConfirmedData(
    val identity: Identity,
    val account: Account
)


class IdentityProviderWebviewViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        val CALLBACK_URL = BuildConfig.SCHEME + "://identity-issuer/callback"
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

    lateinit var identityCreationData: IdentityCreationData
    private val gson = App.appCore.gson
    private val identityRepository: IdentityRepository
    private val accountRepository: AccountRepository
    private val recipientRepository: RecipientRepository
    private val repository: IdentityProviderRepository = IdentityProviderRepository()
    private var identityRequest: BackendRequest<IdentityContainer>? = null
    val useTemporaryBackend = BuildConfig.USE_BACKEND_MOCK

    init {
        val identityDao = WalletDatabase.getDatabase(application).identityDao()
        identityRepository = IdentityRepository(identityDao)
        val accountDao = WalletDatabase.getDatabase(application).accountDao()
        accountRepository = AccountRepository(accountDao)
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
        return "${baseUrl}${delimiter}response_type=code&redirect_uri=$CALLBACK_URL&scope=identity&state=$idObjectRequest"
        //Used to test production:
        //return "https://idiss.notabene.id/idiss/authorize?response_type=token&redirect_uri=concordiumwallet://identity-issuer/callback&scope=identity&state=idObjectRequest"
    }

    private fun saveNewIdentity(identityObject: IdentityObject) {
        val identity = Identity(
            0,
            identityCreationData.identityName,
            IdentityStatus.DONE,
            "",
            "",
            1,  //Next acccount number is set to 1, because 0 has been used for the initial account created by the id provider
            identityCreationData.identityProvider,
            identityObject,
            identityCreationData.privateIdObjectDataEncrypted
        )
        saveNewIdentity(identity)
    }

    private fun saveNewIdentity(identity: Identity) = viewModelScope.launch {
        val identityId = identityRepository.insert(identity)
        identity.id = identityId.toInt()
        createTempInitialAccount(identityId)
        _gotoIdentityConfirmedLiveData.value = Event(identity)
    }

    fun parseIdentityAndSavePending(callbackUri: String) {
        val pubInfoForIP = PubInfoForIp("", RawJson("{}"), "")
        val preIdentityObject =
            PreIdentityObject(
                RawJson("{}"), pubInfoForIP, "",
                RawJson("{}"), "",
                RawJson("{}"), ""
            )
        val identity = Identity(
            0,
            identityCreationData.identityName,
            IdentityStatus.PENDING,
            "",
            callbackUri,
            1,  //Next acccount number is set to 1, because 0 has been used for the initial account created by the id provider
            identityCreationData.identityProvider,
            IdentityObject(
                AttributeList(HashMap(), "", 0, "0"),
                preIdentityObject,
                RawJson("{}")
            ), //TODO: allow null is probably a better solution
            identityCreationData.privateIdObjectDataEncrypted
        )
        saveNewIdentity(identity)
    }

    fun parseIdentityAndSave(identity: String) {
        val identityContainer =
            gson.fromJson<IdentityContainer>(identity, IdentityContainer::class.java)
        saveNewIdentity(identityContainer.value)
    }

    fun parseIdentityError(errorContent: String) {
        val error: Map<String, Any> =
            gson.fromJson(errorContent, object : TypeToken<Map<String, Any>>() {}.type)
        val map: Map<String, Any> = error.get("error") as Map<String, Any>
        val event = Event(map.get("detail").toString());
        if (map.get("code")!!.equals("USER_CANCEL")) {
            _identityCreationUserCancel.value = event
        } else {
            _identityCreationError.value = event
        }
    }

    private suspend fun createTempInitialAccount(identityId: Long): Account {
        val accountName = identityCreationData.accountName
        val accountAddress = identityCreationData.accountAddress
        val encryptedAccountData = identityCreationData.encryptedAccountData

        val account = Account(
            0,
            identityId.toInt(),
            accountName,
            accountAddress,
            "", // This account will not have a valid submissionId
            TransactionStatus.COMMITTED, //This is just a temp state, it will be updated to finalized or absent based on the identity poll response
            encryptedAccountData,
            emptyList(),
            null, // Temp data - the actual data will be returned together with the identityObject for the identity
            0,
            0,
            0,
            0,
            0,
            null,
            null,
            ShieldedAccountEncryptionStatus.ENCRYPTED,
            0,
            0,
            false,
            null
        )
        accountRepository.insert(account)
        //recipientRepository.insert(Recipient(0, account.name, account.address)) - JVE disabled, saved when status is confirmed
        return account
    }

    //region Temporary backend

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

    //endregion
}
