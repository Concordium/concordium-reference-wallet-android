package com.concordium.wallet.ui.identity.identityproviderlist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.AppConfig
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
import com.concordium.wallet.data.model.GlobalParams
 import com.concordium.wallet.data.model.IdentityContainer
 import com.concordium.wallet.data.model.IdentityProvider
 import com.concordium.wallet.data.model.IdentityRequest
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

import com.concordium.sdk.Connection
import com.concordium.sdk.ClientV2
import com.concordium.sdk.requests.BlockQuery
import com.concordium.sdk.TLSConfig

class IdentityProviderListViewModel(application: Application) : AndroidViewModel(application) {
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
        var identityIndex = 0
        var identityName = ""
    }

    init {
        _waitingLiveData.value = true
        _waitingGlobalData.value = true
        val identityDao = WalletDatabase.getDatabase(getApplication()).identityDao()
        identityRepository = IdentityRepository(identityDao)
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

        val connection = Connection.newBuilder()
                .host("grpc.testnet.concordium.com")
                .port(20000)
                .useTLS(TLSConfig.auto())
                .build()
        val client = ClientV2.from(connection)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val globalInfo = client.getCryptographicParameters(BlockQuery.BEST)
                withContext(Dispatchers.Main) {
                    println("good")
                    System.out.println(globalInfo.getOnChainCommitmentKey().toHex())
                    System.out.println(globalInfo.getBulletproofGenerators().toHex())
                    System.out.println(globalInfo.getGenesisString())
                    System.out.println(globalInfo.getVersion())
                 tempData.globalParams = GlobalParams(onChainCommitmentKey= globalInfo.getOnChainCommitmentKey().toHex(), bulletproofGenerators = globalInfo.getBulletproofGenerators().toHex(), genesisString= globalInfo.getGenesisString())
                 _waitingGlobalData.value = false
                }
       } catch (t: Throwable) {
                withContext(Dispatchers.Main) {
                    println("bad")
                    Log.e("ccd", Log.getStackTraceString(t))
                    _waitingGlobalData.value = false
                    _errorLiveData.value = Event(BackendErrorHandler.getExceptionStringRes(t))
                }
        }
    }
}

    fun selectedIdentityVerificationItem(identityProvider: IdentityProvider) {
        currentIdentityProvider = identityProvider
        tempData.identityProvider = identityProvider
        _showAuthenticationLiveData.value = Event(true)
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
        val output = createIdRequestAndPrivateDataV1(password)
        if (output != null) {
            tempData.idObjectRequest = output.idObjectRequest
            _gotoIdentityProviderWebView.value = Event(true)
        } else {
            _errorLiveData.value = Event(R.string.app_error_encryption)
            _waitingLiveData.value = false
        }
    }

    private suspend fun createIdRequestAndPrivateDataV1(password: String): IdRequestAndPrivateDataOutputV1? {
        val identityProvider = tempData.identityProvider
        val global = tempData.globalParams
        if (identityProvider == null) {
            _errorLiveData.value = Event(R.string.app_error_general)
            _waitingLiveData.value = false
            return null
        }

        val net = AppConfig.net
        tempData.identityIndex = identityRepository.nextIdentityIndex(identityProvider.ipInfo.ipIdentity)
        tempData.identityName = identityRepository.nextIdentityName(getApplication<Application>().getString(R.string.view_identity_identity))
        val seed = AuthPreferences(getApplication()).getSeedPhrase(password)

        val output = App.appCore.cryptoLibrary.createIdRequestAndPrivateDataV1(identityProvider.ipInfo, identityProvider.arsInfos, global, seed, net, tempData.identityIndex)
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
            tempData.identityName,
            tempData.identityIndex
        )
    }

    fun usePasscode(): Boolean {
        return App.appCore.getCurrentAuthenticationManager().usePasscode()
    }
}
