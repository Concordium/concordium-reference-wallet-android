package com.concordium.wallet.ui.intro.introstart

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.model.AppSettings
import com.concordium.wallet.data.room.WalletDatabase
import com.concordium.wallet.util.Log
import kotlinx.coroutines.launch

class IntroTermsViewModel(application: Application) : AndroidViewModel(application) {

    private val proxyRepository = ProxyRepository()
    private val identityRepository: IdentityRepository

    private var _hasExistingWalletLiveData = MutableLiveData<Boolean>()
    val hasExistingWalletLiveData: LiveData<Boolean>
        get() = _hasExistingWalletLiveData

    private var _appSettingsLiveData = MutableLiveData<AppSettings>()
    val appSettingsLiveData: LiveData<AppSettings>
        get() = _appSettingsLiveData

    init {
        val identityDao = WalletDatabase.getDatabase(application).identityDao()
        identityRepository = IdentityRepository(identityDao)
    }

    fun checkForExistingWallet() {
        viewModelScope.launch {
            _hasExistingWalletLiveData.value = identityRepository.getCount() > 0
        }
    }

    fun loadAppSettings() {
        viewModelScope.launch {
            val response = proxyRepository.getAppSettings(App.appCore.getAppVersion())
            if (response.isSuccessful) {
                response.body()?.let {
                    _appSettingsLiveData.value = it
                } ?: run {
                    _appSettingsLiveData.value = null
                }
            } else {
                Log.d("appSettings failed")
                _appSettingsLiveData.value = null
            }
        }
    }
}
