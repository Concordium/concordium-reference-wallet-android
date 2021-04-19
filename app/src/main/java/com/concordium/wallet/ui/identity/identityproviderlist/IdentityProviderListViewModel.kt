package com.concordium.wallet.ui.identity.identityproviderlist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.Event
import com.concordium.wallet.core.authentication.AuthenticationManager
import com.concordium.wallet.core.backend.BackendRequest
import com.concordium.wallet.core.security.KeystoreEncryptionException
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.backend.repository.IdentityProviderRepository
import com.concordium.wallet.data.cryptolib.IdRequestAndPrivateDataOutput
import com.concordium.wallet.data.model.*
import com.concordium.wallet.data.room.WalletDatabase
import com.concordium.wallet.ui.common.BackendErrorHandler
import kotlinx.coroutines.launch
import javax.crypto.Cipher

class IdentityProviderListViewModel(application: Application) :
    AndroidViewModel(application) {

    lateinit var identityCustomName: String
    lateinit var accountCustomName: String

    private val identityRepository: IdentityRepository
    private val repository: IdentityProviderRepository = IdentityProviderRepository()
    private val gson = App.appCore.gson

    private var identityProviderInfoRequest: BackendRequest<ArrayList<IdentityProvider>>? = null
    private var globalParamsRequest: BackendRequest<GlobalParamsWrapper>? = null

    private val tempData = TempData()
    var currentIdentityProvider: IdentityProvider? = null
        private set


    private val _waitingLiveData = MutableLiveData<Boolean>()
    val waitingLiveData: LiveData<Boolean>
        get() = _waitingLiveData

    private val _waitingGlobalData = MutableLiveData<Boolean>()
    val waitingGlobalData: LiveData<Boolean>
        get() = _waitingGlobalData

    private val _errorLiveData = MutableLiveData<Event<Int>>()
    val errorLiveData: LiveData<Event<Int>>
        get() = _errorLiveData

    private val _identityProviderList = MutableLiveData<List<IdentityProvider>>()
    val identityProviderList: LiveData<List<IdentityProvider>>
        get() = _identityProviderList

    private val _showAuthenticationLiveData = MutableLiveData<Event<Boolean>>()
    val showAuthenticationLiveData: LiveData<Event<Boolean>>
        get() = _showAuthenticationLiveData

    private val _gotoIdentityProviderWebview = MutableLiveData<Event<Boolean>>()
    val gotoIdentityProviderWebview: LiveData<Event<Boolean>>
        get() = _gotoIdentityProviderWebview

    private class TempData {
        var globalParams: GlobalParams? = null
        var identityProvider: IdentityProvider? = null
        var idObjectRequest: RawJson? = null
        var privateIdObjectDataEncrypted: String? = null
        var encryptedAccountData: String? = null
        var accountAddress: String? = null
    }

    init {
        val identityDao = WalletDatabase.getDatabase(application).identityDao()
        identityRepository = IdentityRepository(identityDao)
        _waitingLiveData.value = true
        _waitingGlobalData.value = true
    }

    fun initialize(identityCustomName: String, accountCustomName: String) {
        this.identityCustomName = identityCustomName
        this.accountCustomName = accountCustomName
    }

    override fun onCleared() {
        super.onCleared()
        identityProviderInfoRequest?.dispose()
    }

    fun getIdentityProviders() {
        _waitingLiveData.value = true
        identityProviderInfoRequest?.dispose()
        identityProviderInfoRequest = repository.getIdentityProviderInfo(
            {
                _identityProviderList.value = it
                _waitingLiveData.value = false
            },
            {
                _waitingLiveData.value = false
                _errorLiveData.value = Event(BackendErrorHandler.getExceptionStringRes(it))
            }
        )
    }

    fun getGlobalInfo() {
        _waitingGlobalData.value = true
        globalParamsRequest?.dispose()
        globalParamsRequest = repository.getIGlobalInfo(
            {
                tempData.globalParams = it.value
                _waitingGlobalData.value = false
            },
            {
                _waitingGlobalData.value = false
                _errorLiveData.value = Event(BackendErrorHandler.getExceptionStringRes(it))
            }
        )
    }

    fun selectedIdentityVerificationItem(identityProvider: IdentityProvider) {
        currentIdentityProvider = identityProvider
        tempData.identityProvider = identityProvider
        _showAuthenticationLiveData.value = Event(true)
    }

    fun shouldUseBiometrics(): Boolean {
        return App.appCore.getCurrentAuthenticationManager().useBiometrics()
    }

    fun getCipherForBiometrics(): Cipher? {
        try {
            val cipher = App.appCore.getCurrentAuthenticationManager().initBiometricsCipherForDecryption()
            if (cipher == null) {
                _errorLiveData.value = Event(R.string.app_error_keystore_key_invalidated)
            }
            return cipher
        } catch (e: KeystoreEncryptionException) {
            _errorLiveData.value = Event(R.string.app_error_keystore)
            return null
        }
    }

    fun continueWithPassword(password: String) = viewModelScope.launch {
        _waitingLiveData.value = true
        encryptAndContinue(password)
    }

    fun checkLogin(cipher: Cipher) = viewModelScope.launch {
        _waitingLiveData.value = true
        val password = App.appCore.getCurrentAuthenticationManager().checkPasswordInBackground(cipher)
        if (password != null) {
            encryptAndContinue(password)
        } else {
            _errorLiveData.value = Event(R.string.app_error_encryption)
            _waitingLiveData.value = false
        }
    }

    private suspend fun encryptAndContinue(password: String) {
        // Create and encrypt the private data
        val output = createIdRequestAndPrivateData()
        if (output != null) {
            val tempCurrentPrivateIdObjectDataJson = gson.toJson(output.privateIdObjectData.value)
            val encodedEncrypted =
                App.appCore.getCurrentAuthenticationManager().encryptInBackground(
                    password,
                    tempCurrentPrivateIdObjectDataJson
                )
            if (encodedEncrypted != null && encryptAccountData(password, output)) {
                tempData.privateIdObjectDataEncrypted = encodedEncrypted
                tempData.idObjectRequest = output.idObjectRequest
                tempData.accountAddress = output.initialAccountData.accountAddress
                _gotoIdentityProviderWebview.value = Event(true)
            } else {
                _errorLiveData.value = Event(R.string.app_error_encryption)
                _waitingLiveData.value = false
            }
        }
    }

    private suspend fun encryptAccountData(password: String, output: IdRequestAndPrivateDataOutput): Boolean {
        //Encrypt account data for later when saving account
        val initialAccountData = output.initialAccountData
        val jsonToBeEncrypted = gson.toJson(initialAccountData)
        val storageAccountDataEncrypted = App.appCore.getCurrentAuthenticationManager().encryptInBackground(password, jsonToBeEncrypted)
        if (storageAccountDataEncrypted != null) {
            tempData.encryptedAccountData = storageAccountDataEncrypted
            return true
        }
        return false
    }

    private suspend fun createIdRequestAndPrivateData(): IdRequestAndPrivateDataOutput? {
        val identityProvider = tempData.identityProvider
        val global = tempData.globalParams
        if (identityProvider == null) {
            _errorLiveData.value = Event(R.string.app_error_general)
            _waitingLiveData.value = false
            return null
        }
        val output =
            App.appCore.cryptoLibrary.createIdRequestAndPrivateData(identityProvider.ipInfo, identityProvider.arsInfos, global)
        if (output != null) {
            return output
        } else {
            _errorLiveData.value = Event(R.string.app_error_lib)
            _waitingLiveData.value = false
            return null
        }
    }

    fun getIdentityCreationData(): IdentityCreationData? {
        val identityProvider = tempData.identityProvider
        val idObjectRequest = tempData.idObjectRequest
        val privateIdObjectDataEncrypted = tempData.privateIdObjectDataEncrypted
        val encryptedAccountData = tempData.encryptedAccountData
        val accountAddress = tempData.accountAddress
        if (identityProvider == null || idObjectRequest == null || privateIdObjectDataEncrypted == null || encryptedAccountData == null || accountAddress == null) {
            _errorLiveData.value = Event(R.string.app_error_general)
            return null
        }
        return IdentityCreationData(
            identityProvider,
            idObjectRequest,
            privateIdObjectDataEncrypted,
            identityCustomName,
            accountCustomName,
            encryptedAccountData,
            accountAddress
        )
    }

    fun usePasscode(): Boolean {
        return App.appCore.getCurrentAuthenticationManager().usePasscode()
    }
}