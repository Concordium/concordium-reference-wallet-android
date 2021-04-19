package com.concordium.wallet.core.authentication

import android.content.Context
import android.os.CountDownTimer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.concordium.wallet.App
import com.concordium.wallet.data.preferences.AuthPreferences
import com.concordium.wallet.data.preferences.FilterPreferences

class Session {

    private var authPreferences: AuthPreferences
    private var filterPreferences: FilterPreferences

    var hasSetupPassword = false
        private set

    var tempPassword: String? = null
        private set

    private val _isLoggedIn = MutableLiveData<Boolean>(false)
    val isLoggedIn: LiveData<Boolean>
        get() = _isLoggedIn

    constructor(context: Context) {
        authPreferences = AuthPreferences(context)
        hasSetupPassword = authPreferences.getHasSetupUser()
        filterPreferences = FilterPreferences(context)
    }

    fun setHasShowRewards(id: Int, value: Boolean) {
        filterPreferences.setHasShowRewards(id, value)
    }

    fun getHasShowRewards(id: Int): Boolean {
        return filterPreferences.getHasShowRewards(id)
    }

    fun setHasShowFinalizationRewards(id: Int, value: Boolean) {
        filterPreferences.setHasShowFinalizationRewards(id, value)
    }

    fun getHasShowFinalizationRewards(id: Int): Boolean {
        return filterPreferences.getHasShowFinalizationRewards(id)
    }

    fun hasSetupPassword(passcodeUsed: Boolean = false) {
        _isLoggedIn.value = true
        authPreferences.setHasSetupUser(true)
        App.appCore.getCurrentAuthenticationManager().setUsePassCode(passcodeUsed)
        hasSetupPassword = true
    }

    fun hasFinishedSetupPassword() {
        tempPassword = null
    }

    fun startPasswordSetup(password: String) {
        tempPassword = password
    }

    fun checkPassword(password: String): Boolean {
        return password.equals(tempPassword)
    }

    fun hasLoggedInUser() {
        _isLoggedIn.value = true
        resetLogoutTimeout()
    }

    fun resetLogoutTimeout() {
        if(_isLoggedIn.value!!){
            inactivityCountDownTimer.cancel()
            inactivityCountDownTimer.start()
        }
    }

    var inactivityCountDownTimer =
        object : CountDownTimer(60 * 5 * 1000.toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {}

            override fun onFinish() {
                _isLoggedIn.value = false
            }
        }

    fun getBiometricAuthKeyName() : String{
        return authPreferences.getAuthKeyName()
    }

    fun setBiometricAuthKeyName(resetBiometricKeyNameAppendix: String) {
        authPreferences.setAuthKeyName(resetBiometricKeyNameAppendix)
    }


}