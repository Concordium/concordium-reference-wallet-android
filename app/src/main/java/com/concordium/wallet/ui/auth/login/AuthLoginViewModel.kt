package com.concordium.wallet.ui.auth.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.Event
import com.concordium.wallet.core.authentication.AuthenticationManager
import com.concordium.wallet.core.authentication.Session
import com.concordium.wallet.core.security.KeystoreEncryptionException
import kotlinx.coroutines.launch
import javax.crypto.Cipher

class AuthLoginViewModel(application: Application) : AndroidViewModel(application) {

    private val session: Session = App.appCore.session

    private val _waitingLiveData = MutableLiveData<Boolean>()
    val waitingLiveData: LiveData<Boolean>
        get() = _waitingLiveData

    private val _errorLiveData = MutableLiveData<Event<Int>>()
    val errorLiveData: LiveData<Event<Int>>
        get() = _errorLiveData

    private val _passwordErrorLiveData = MutableLiveData<Event<Int>>()
    val passwordErrorLiveData: LiveData<Event<Int>>
        get() = _passwordErrorLiveData

    private val _finishScreenLiveData = MutableLiveData<Event<Boolean>>()
    val finishScreenLiveData: LiveData<Event<Boolean>>
        get() = _finishScreenLiveData



    fun initialize() {

    }

    fun shouldShowBiometrics(): Boolean {
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

    fun checkLogin(password: String) = viewModelScope.launch {
        _waitingLiveData.value = true
        val res = App.appCore.getCurrentAuthenticationManager().checkPasswordInBackground(password)
        if (res) {
            loginSuccess()
        } else {
            _passwordErrorLiveData.value =
                Event(if (App.appCore.getCurrentAuthenticationManager().usePasscode()) R.string.auth_login_passcode_error else R.string.auth_login_password_error)
            _waitingLiveData.value = false
        }
    }

    fun checkLogin(cipher: Cipher) = viewModelScope.launch {
        _waitingLiveData.value = true
        val password = App.appCore.getCurrentAuthenticationManager().checkPasswordInBackground(cipher)
        if (password != null) {
            loginSuccess()
        } else {
            _passwordErrorLiveData.value =
                Event(if (App.appCore.getCurrentAuthenticationManager().usePasscode()) R.string.auth_login_passcode_error else R.string.auth_login_password_error)
            _waitingLiveData.value = false
        }
    }

    private fun loginSuccess() {
        session.hasLoggedInUser()
        _finishScreenLiveData.value = Event(true)
    }

    fun usePasscode(): Boolean {
        return App.appCore.getCurrentAuthenticationManager().usePasscode()
    }

}