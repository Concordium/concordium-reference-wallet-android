package com.concordium.wallet.ui.auth.setup

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.concordium.wallet.App
import com.concordium.wallet.core.arch.Event
import com.concordium.wallet.core.authentication.Session
import com.concordium.wallet.util.BiometricsUtil

class AuthSetupViewModel(application: Application) : AndroidViewModel(application) {

    private val session: Session = App.appCore.session

    private val _errorLiveData = MutableLiveData<Event<Boolean>>()
    val errorLiveData: LiveData<Event<Boolean>>
        get() = _errorLiveData
    private val _finishScreenLiveData = MutableLiveData<Event<Boolean>>()
    val finishScreenLiveData: LiveData<Event<Boolean>>
        get() = _finishScreenLiveData
    private val _gotoBiometricsSetupLiveData = MutableLiveData<Event<Boolean>>()
    val gotoBiometricsSetupLiveData: LiveData<Event<Boolean>>
        get() = _gotoBiometricsSetupLiveData

    fun initialize() {

    }

    fun hasFinishedSetupPassword() {
        session.hasFinishedSetupPassword()
    }

    fun startSetupPassword(password: String) {
        // Keep password for re-enter check and biometrics activation
        session.startPasswordSetup(password)
    }

    fun checkPasswordRequirements(password: String): Boolean {
        return (password.length == 6)
    }

    fun setupPassword(password: String, continueFlow: Boolean) {
        val res = App.appCore.getCurrentAuthenticationManager().createPasswordCheck(password)
        if (res) {
            // Setting up password is done, so login screen should be shown next time app is opened
            session.hasSetupPassword(true)
            if (BiometricsUtil.isBiometricsAvailable()) {
                _gotoBiometricsSetupLiveData.value = Event(true)
            } else {
                if (continueFlow) { // if we continue flow (setting up account and identity) we are at the end of the line and we clear stored password
                    session.hasFinishedSetupPassword()
                }
                _finishScreenLiveData.value = Event(true)
            }
        } else {
            _errorLiveData.value = Event(true)
        }
    }
}