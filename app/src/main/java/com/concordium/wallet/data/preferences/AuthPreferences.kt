package com.concordium.wallet.data.preferences

import android.content.Context
import android.util.Log

class AuthPreferences(val context: Context) :
    Preferences(context, SharedPreferencesKeys.PREF_FILE_AUTH, Context.MODE_PRIVATE) {

    companion object {
        val PREFKEY_HAS_SETUP_USER = "PREFKEY_HAS_SETUP_USER"
        val PREFKEY_USE_PASSCODE = "PREFKEY_USE_PASSCODE"
        val PREFKEY_USE_BIOMETRICS = "PREFKEY_USE_BIOMETRICS"
        val PREFKEY_PASSWORD_CHECK = "PREFKEY_PASSWORD_CHECK"
        val PREFKEY_PASSWORD_CHECK_ENCRYPTED = "PREFKEY_PASSWORD_CHECK_ENCRYPTED"
        val PREFKEY_PASSWORD_ENCRYPTION_SALT = "PREFKEY_PASSWORD_ENCRYPTION_SALT"
        val PREFKEY_PASSWORD_ENCRYPTION_INITVECTOR = "PREFKEY_PASSWORD_ENCRYPTION_INITVECTOR"
        val PREFKEY_ENCRYPTED_PASSWORD = "PREFKEY_ENCRYPTED_PASSWORD_DERIVED_KEY"
        val PREFKEY_ENCRYPTED_PASSWORD_DERIVED_KEY_INITVECTOR = "PREFKEY_ENCRYPTED_PASSWORD_DERIVED_KEY_INITVECTOR"
        val PREFKEY_BIOMETRIC_KEY = "PREFKEY_BIOMETRIC_KEY"
    }

    fun setHasSetupUser(value: Boolean) {
        setBoolean(PREFKEY_HAS_SETUP_USER, value)
    }

    fun getHasSetupUser(): Boolean {
        return getBoolean(PREFKEY_HAS_SETUP_USER)
    }

    fun setUsePasscode(appendix: String, value: Boolean) {
        setBoolean(PREFKEY_USE_PASSCODE+appendix, value)
    }

    fun getUsePasscode(appendix: String): Boolean {
        return getBoolean(PREFKEY_USE_PASSCODE+appendix)
    }

    fun setUseBiometrics(appendix: String, value: Boolean) {
        setBoolean(PREFKEY_USE_BIOMETRICS+appendix, value)
    }

    fun getUseBiometrics(appendix: String): Boolean {
        return getBoolean(PREFKEY_USE_BIOMETRICS+appendix)
    }

    fun setPasswordCheck(appendix: String, value: String) {
        setString(PREFKEY_PASSWORD_CHECK+appendix, value)
    }

    fun getPasswordCheck(appendix: String): String? {
        return getString(PREFKEY_PASSWORD_CHECK+appendix)
    }

    fun setPasswordCheckEncrypted(appendix: String, value: String) {
        setString(PREFKEY_PASSWORD_CHECK_ENCRYPTED+appendix, value)
    }

    fun getPasswordCheckEncrypted(appendix: String): String {
        return getString(PREFKEY_PASSWORD_CHECK_ENCRYPTED+appendix, "")
    }

    fun setPasswordEncryptionSalt(appendix: String, value: String) {
        setString(PREFKEY_PASSWORD_ENCRYPTION_SALT+appendix, value)
    }

    fun getPasswordEncryptionSalt(appendix: String): String {
        return getString(PREFKEY_PASSWORD_ENCRYPTION_SALT+appendix, "")
    }

    fun setPasswordEncryptionInitVector(appendix: String, value: String) {
        setString(PREFKEY_PASSWORD_ENCRYPTION_INITVECTOR+appendix, value)
    }

    fun getPasswordEncryptionInitVector(appendix: String): String {
        return getString(PREFKEY_PASSWORD_ENCRYPTION_INITVECTOR+appendix, "")
    }

    fun setEncryptedPassword(appendix: String, value: String) {
        setString(PREFKEY_ENCRYPTED_PASSWORD+appendix, value)
    }

    fun getEncryptedPassword(appendix: String): String {
        return getString(PREFKEY_ENCRYPTED_PASSWORD+appendix, "")
    }

    fun setEncryptedPasswordDerivedKeyInitVector(appendix: String, value: String) {
        setString(PREFKEY_ENCRYPTED_PASSWORD_DERIVED_KEY_INITVECTOR+appendix, value)
    }

    fun getBiometricsKeyEncryptionInitVector(appendix: String): String {
        return getString(PREFKEY_ENCRYPTED_PASSWORD_DERIVED_KEY_INITVECTOR+appendix, "")
    }

    fun getAuthKeyName(): String {
        return getString(PREFKEY_BIOMETRIC_KEY, "default_key")
    }

    fun setAuthKeyName(key: String) {
        return setString(PREFKEY_BIOMETRIC_KEY, key)
    }

}