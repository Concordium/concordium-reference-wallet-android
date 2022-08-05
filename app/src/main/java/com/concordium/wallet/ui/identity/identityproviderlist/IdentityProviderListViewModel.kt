package com.concordium.wallet.ui.identity.identityproviderlist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.Event
import com.concordium.wallet.core.backend.BackendRequest
import com.concordium.wallet.core.security.KeystoreEncryptionException
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.backend.repository.IdentityProviderRepository
import com.concordium.wallet.data.cryptolib.IdRequestAndPrivateDataOutputV1
import com.concordium.wallet.data.model.*
import com.concordium.wallet.data.preferences.AuthPreferences
import com.concordium.wallet.data.room.WalletDatabase
import com.concordium.wallet.ui.common.BackendErrorHandler
import kotlinx.coroutines.launch
import javax.crypto.Cipher

class IdentityProviderListViewModel(application: Application) : AndroidViewModel(application) {
    private lateinit var identityCustomName: String

    private val repository: IdentityProviderRepository = IdentityProviderRepository()
    private val identityRepository: IdentityRepository

    private var identityProviderInfoRequest: BackendRequest<ArrayList<IdentityProvider>>? = null
    private var globalParamsRequest: BackendRequest<GlobalParamsWrapper>? = null

    private val tempData = TempData()
    private var currentIdentityProvider: IdentityProvider? = null

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

    private val _gotoIdentityProviderWebView = MutableLiveData<Event<Boolean>>()
    val gotoIdentityProviderWebView: LiveData<Event<Boolean>>
        get() = _gotoIdentityProviderWebView

    private class TempData {
        var globalParams: GlobalParams? = null
        var identityProvider: IdentityProvider? = null
        var idObjectRequest: RawJson? = null
    }

    init {
        _waitingLiveData.value = true
        _waitingGlobalData.value = true
        val identityDao = WalletDatabase.getDatabase(getApplication()).identityDao()
        identityRepository = IdentityRepository(identityDao)
    }

    fun initialize(identityNamePrefix: String) {
        viewModelScope.launch {
            val identityCount = identityRepository.nextIdentityNumber()
            identityCustomName = "$identityNamePrefix $identityCount"
        }
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
        return try {
            val cipher = App.appCore.getCurrentAuthenticationManager().initBiometricsCipherForDecryption()
            if (cipher == null) {
                _errorLiveData.value = Event(R.string.app_error_keystore_key_invalidated)
            }
            cipher
        } catch (e: KeystoreEncryptionException) {
            _errorLiveData.value = Event(R.string.app_error_keystore)
            null
        }
    }

    fun continueWithPassword() = viewModelScope.launch {
        _waitingLiveData.value = true
        encryptAndContinue()
    }

    fun checkLogin(cipher: Cipher) = viewModelScope.launch {
        _waitingLiveData.value = true
        val password = App.appCore.getCurrentAuthenticationManager().checkPasswordInBackground(cipher)
        if (password != null) {
            encryptAndContinue()
        } else {
            _errorLiveData.value = Event(R.string.app_error_encryption)
            _waitingLiveData.value = false
        }
    }

    private suspend fun encryptAndContinue() {
        val output = createIdRequestAndPrivateDataV1()
        if (output != null) {
            tempData.idObjectRequest = output.idObjectRequest
            _gotoIdentityProviderWebView.value = Event(true)
        } else {
            _errorLiveData.value = Event(R.string.app_error_encryption)
            _waitingLiveData.value = false
        }
    }

    private suspend fun createIdRequestAndPrivateDataV1(): IdRequestAndPrivateDataOutputV1? {
        val identityProvider = tempData.identityProvider
        val global = tempData.globalParams
        if (identityProvider == null) {
            _errorLiveData.value = Event(R.string.app_error_general)
            _waitingLiveData.value = false
            return null
        }

        val net = "Mainnet"
        val identityIndex = identityRepository.nextIdentityNumber()
        val seed = AuthPreferences(getApplication()).getSeedPhrase()

        val output = App.appCore.cryptoLibrary.createIdRequestAndPrivateDataV1(identityProvider.ipInfo, identityProvider.arsInfos, global, seed, net, identityIndex)
        return if (output != null) {
            output
        } else {
            _errorLiveData.value = Event(R.string.app_error_lib)
            _waitingLiveData.value = false
            null
        }
    }

    fun getIdentityCreationData(): IdentityCreationData? {
        val identityProvider = tempData.identityProvider
        val idObjectRequest = tempData.idObjectRequest
        if (identityProvider == null || idObjectRequest == null) {
            _errorLiveData.value = Event(R.string.app_error_general)
            return null
        }
        return IdentityCreationData(
            identityProvider,
            idObjectRequest,
            identityCustomName
        )
    }

    fun usePasscode(): Boolean {
        return App.appCore.getCurrentAuthenticationManager().usePasscode()
    }
}
