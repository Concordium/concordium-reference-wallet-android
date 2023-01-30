package com.concordium.wallet.data.preferences

import android.content.Context
import cash.z.ecc.android.bip39.Mnemonics
import cash.z.ecc.android.bip39.toSeed
import com.concordium.wallet.App
import com.concordium.wallet.util.toHex

class AuthPreferences(val context: Context) :
    Preferences(context, SharedPreferencesKeys.PREF_FILE_AUTH, Context.MODE_PRIVATE) {

    companion object {
        const val PREFKEY_HAS_SETUP_USER = "PREFKEY_HAS_SETUP_USER"
        const val PREFKEY_USE_PASSCODE = "PREFKEY_USE_PASSCODE"
        const val PREFKEY_USE_BIOMETRICS = "PREFKEY_USE_BIOMETRICS"
        const val PREFKEY_PASSWORD_CHECK = "PREFKEY_PASSWORD_CHECK"
        const val PREFKEY_PASSWORD_CHECK_ENCRYPTED = "PREFKEY_PASSWORD_CHECK_ENCRYPTED"
        const val PREFKEY_PASSWORD_ENCRYPTION_SALT = "PREFKEY_PASSWORD_ENCRYPTION_SALT"
        const val PREFKEY_PASSWORD_ENCRYPTION_INITVECTOR = "PREFKEY_PASSWORD_ENCRYPTION_INITVECTOR"
        const val PREFKEY_ENCRYPTED_PASSWORD = "PREFKEY_ENCRYPTED_PASSWORD_DERIVED_KEY"
        const val PREFKEY_ENCRYPTED_PASSWORD_DERIVED_KEY_INITVECTOR = "PREFKEY_ENCRYPTED_PASSWORD_DERIVED_KEY_INITVECTOR"
        const val PREFKEY_BIOMETRIC_KEY = "PREFKEY_BIOMETRIC_KEY"
        const val PREFKEY_TERMS_HASHED = "PREFKEY_TERMS_HASHED"
        const val PREFKEY_SHIELDING_ENABLED_ = "PREFKEY_SHIELDING_ENABLED_"
        const val PREFKEY_SHIELDED_WARNING_DISMISSED_ = "PREFKEY_SHIELDED_WARNING_DISMISSED_"
        const val PREFKEY_IDENTITY_PENDING_ACKNOWLEDGED = "PREFKEY_IDENTITY_PENDING_ACKNOWLEDGED_"
        const val SEED_PHRASE = "SEED_PHRASE"
        const val SEED_PHRASE_ENCRYPTED = "SEED_PHRASE_ENCRYPTED"
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

    fun setIdentityPendingWarningAcknowledged(id: Int) {
        return setBoolean(PREFKEY_IDENTITY_PENDING_ACKNOWLEDGED+id, true)
    }

    fun isIdentityPendingWarningAcknowledged(id: Int): Boolean {
        return getBoolean(PREFKEY_IDENTITY_PENDING_ACKNOWLEDGED+id, false)
    }

    fun resetSeedPhrase() {
        setString(SEED_PHRASE, "")
    }

    suspend fun setSeedPhrase(value: String, password: String): Boolean {
        val seed = Mnemonics.MnemonicCode(value).toSeed()
        val seedEncoded = seed.toHex()
        val encryptedSeed = App.appCore.getCurrentAuthenticationManager().encryptInBackground(password, seedEncoded)
            ?: return false
       return setStringWithResult(SEED_PHRASE_ENCRYPTED, encryptedSeed)
    }

    fun getSeedPhrase(password: String): String {
        if(getString(SEED_PHRASE_ENCRYPTED) == null){

        }

        return getString(SEED_PHRASE, "")
    }

    fun hasSeedPhrase(): Boolean {
        return getString(SEED_PHRASE) != null || getString(SEED_PHRASE_ENCRYPTED) != null
    }
}
