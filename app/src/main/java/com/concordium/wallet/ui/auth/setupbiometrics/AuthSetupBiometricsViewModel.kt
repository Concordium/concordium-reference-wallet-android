package com.concordium.wallet.ui.auth.setupbiometrics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.Event
import com.concordium.wallet.core.authentication.AuthenticationManager
import com.concordium.wallet.core.authentication.Session
import com.concordium.wallet.core.security.KeystoreEncryptionException
import com.concordium.wallet.util.Log
import javax.crypto.Cipher

class AuthSetupBiometricsViewModel(application: Application) : AndroidViewModel(application) {

    private val session: Session = App.appCore.session

    private val _errorLiveData = MutableLiveData<Event<Int>>()
    val errorLiveData: LiveData<Event<Int>>
        get() = _errorLiveData
    private val _finishScreenLiveData = MutableLiveData<Event<Boolean>>()
    val finishScreenLiveData: LiveData<Event<Boolean>>
        get() = _finishScreenLiveData



    fun initialize() {

        val generated = App.appCore.getCurrentAuthenticationManager().generateBiometricsSecretKey()
        if (!generated) {
            _errorLiveData.value = Event(R.string.app_error_keystore)
        }
    }

    fun getCipherForBiometrics(): Cipher? {
        try {
            val cipher = App.appCore.getCurrentAuthenticationManager().initBiometricsCipherForEncryption()
            if (cipher == null) {
                _errorLiveData.value = Event(R.string.app_error_keystore_key_invalidated)
            }
            return cipher
        } catch (e: KeystoreEncryptionException) {
            _errorLiveData.value = Event(R.string.app_error_keystore)
            return null
        }
    }

    fun setupBiometricWithPassword(cipher: Cipher) {
        val password = session.tempPassword
        if (password != null) {
            val setupDone = App.appCore.getCurrentAuthenticationManager().setupBiometrics(password, cipher)
            if (setupDone) {
                _finishScreenLiveData.value = Event(true)
            } else {
                _errorLiveData.value = Event(R.string.app_error_keystore)
            }
        } else {
            Log.e("Temp password has been removed before biometrics was setup")
        }
    }
}