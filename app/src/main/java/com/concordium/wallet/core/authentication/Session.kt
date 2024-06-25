package com.concordium.wallet.core.authentication

import android.content.Context
import android.os.CountDownTimer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.concordium.wallet.App
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.data.preferences.AuthPreferences
import com.concordium.wallet.data.preferences.FilterPreferences
import com.concordium.wallet.data.preferences.Preferences

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

    // The notice must be shown once per app start.
    private var isShieldingNoticeShown = false

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

    fun shieldingNoticeShown() {
        isShieldingNoticeShown = true
    }

    fun isShieldingNoticeShown(): Boolean =
        isShieldingNoticeShown

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

    fun getTermsHashed(): Int {
        return authPreferences.getTermsHashed()
    }

    fun setTermsHashed(key: Int) {
        return authPreferences.setTermsHashed(key)
    }

    fun isAccountsBackedUp(): Boolean {
        return authPreferences.isAccountsBackedUp()
    }

    fun setAccountsBackedUp(value: Boolean) {
        return authPreferences.setAccountsBackedUp(value)
    }

    fun isIdentityPendingWarningAcknowledged(id: Int): Boolean {
        return authPreferences.isIdentityPendingWarningAcknowledged(id)
    }

    fun setIdentityPendingWarningAcknowledged(id: Int) {
        return authPreferences.setIdentityPendingWarningAcknowledged(id)
    }

    fun isShieldingEnabled(accountAddress: String): Boolean {
        return authPreferences.isShieldingEnabled(accountAddress)
    }

    fun setShieldingEnabled(accountAddress: String, value: Boolean) {
        return authPreferences.setShieldingEnabled(accountAddress, value)
    }

    fun isShieldedWarningDismissed(accountAddress: String): Boolean {
        return authPreferences.isShieldedWarningDismissed(accountAddress)
    }

    fun setShieldedWarningDismissed(accountAddress: String, value: Boolean) {
        return authPreferences.setShieldedWarningDismissed(accountAddress, value)
    }



    fun addAccountsBackedUpListener(listener: Preferences.Listener) {
        authPreferences.addAccountsBackedUpListener(listener)
    }
    fun removeAccountsBackedUpListener(listener: Preferences.Listener) {
        authPreferences.removeListener(listener)
    }

    fun shouldPromptForBackedUp(context: Context): Boolean {
        val isFirstTimeInstall = with(context.packageManager.getPackageInfo(context.packageName, 0)) {
            firstInstallTime == lastUpdateTime
        }
        val versionBackedUp = authPreferences.getVersionBackedUp()
        authPreferences.setVersionBackedUp(BuildConfig.VERSION_CODE)
        if(isFirstTimeInstall){
            return false
        }
        if(versionBackedUp == 0){
            authPreferences.setAccountsBackedUp(false)
            return true
        }
        return false
    }



}
