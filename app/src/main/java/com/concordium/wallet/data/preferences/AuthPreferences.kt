package com.concordium.wallet.data.preferences

import android.content.Context

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
        val PREFKEY_TERMS_HASHED = "PREFKEY_TERMS_HASHED"
        val PREFKEY_ACCOUNTS_BACKED_UP = "PREFKEY_ACCOUNTS_BACKED_UP"
        val PREFKEY_VERSION_BACKED_UP = "PREFKEY_VERSION_BACKED_UP"
        val PREFKEY_SHIELDING_ENABLED_ = "PREFKEY_SHIELDING_ENABLED_"
        val PREFKEY_SHIELDED_WARNING_DISMISSED_ = "PREFKEY_SHIELDED_WARNING_DISMISSED_"
        val PREFKEY_IDENTITY_PENDING_ACKNOWLEDGED = "PREFKEY_IDENTITY_PENDING_ACKNOWLEDGED_"
        val PASS_PHRASE = "PASS_PHRASE"
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

    fun getTermsHashed(): Int {
        return getInt(PREFKEY_TERMS_HASHED, 0)
    }

    fun setTermsHashed(key: Int) {
        return setInt(PREFKEY_TERMS_HASHED, key)
    }

    fun isShieldingEnabled(accountAddress: String): Boolean {
        return getBoolean(PREFKEY_SHIELDING_ENABLED_+accountAddress, false)
    }

    fun setShieldingEnabled(accountAddress: String, value: Boolean) {
        return setBoolean(PREFKEY_SHIELDING_ENABLED_+accountAddress, value)
    }

    fun isShieldedWarningDismissed(accountAddress: String): Boolean {
        return getBoolean(PREFKEY_SHIELDED_WARNING_DISMISSED_+accountAddress, false)
    }

    fun setShieldedWarningDismissed(accountAddress: String, value: Boolean) {
        return setBoolean(PREFKEY_SHIELDED_WARNING_DISMISSED_+accountAddress, value)
    }

    fun isAccountsBackedUp(): Boolean {
        return getBoolean(PREFKEY_ACCOUNTS_BACKED_UP, false)
    }

    fun setAccountsBackedUp(value: Boolean) {
        return setBoolean(PREFKEY_ACCOUNTS_BACKED_UP, value)
    }

    fun addAccountsBackedUpListener(listener: Listener) {
        addListener(PREFKEY_ACCOUNTS_BACKED_UP, listener)
    }

    fun getVersionBackedUp(): Int {
        return getInt(PREFKEY_VERSION_BACKED_UP, 0)
    }

    fun setVersionBackedUp(value: Int) {
        return setInt(PREFKEY_VERSION_BACKED_UP, value)
    }

    fun setIdentityPendingWarningAcknowledged(id: Int) {
        return setBoolean(PREFKEY_IDENTITY_PENDING_ACKNOWLEDGED+id, true)
    }

    fun isIdentityPendingWarningAcknowledged(id: Int): Boolean {
        return getBoolean(PREFKEY_IDENTITY_PENDING_ACKNOWLEDGED+id, false)
    }

    fun setPassPhrase(value: String) {
        setString(PASS_PHRASE, value)
    }

    fun getPassPhrase(): String {
        return getString(PASS_PHRASE, "")
    }

    fun hasPassPhrase(): Boolean {
        return getPassPhrase() != ""
    }
}