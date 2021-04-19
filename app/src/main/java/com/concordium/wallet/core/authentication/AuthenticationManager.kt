package com.concordium.wallet.core.authentication

import android.util.Base64
import com.concordium.wallet.App
import com.concordium.wallet.core.security.EncryptionException
import com.concordium.wallet.core.security.EncryptionHelper
import com.concordium.wallet.core.security.KeystoreEncryptionException
import com.concordium.wallet.core.security.KeystoreHelper
import com.concordium.wallet.data.preferences.AuthPreferences
import com.concordium.wallet.util.Log
import com.concordium.wallet.util.RandomUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.SecretKey


class AuthenticationManager(biometricKeyName: String) {


    private val biometricKeyName: String = biometricKeyName
    private val authPreferences = AuthPreferences(App.appContext)

    //region Biometrics

    fun setupBiometrics(password: String, cipher: Cipher): Boolean {
        try {
            val initVector = cipher.iv
            val encrypted = cipher.doFinal(password.toByteArray())
            val encodedEncryptedPassword = Base64.encodeToString(encrypted, Base64.DEFAULT)
            authPreferences.setEncryptedPassword(biometricKeyName,encodedEncryptedPassword)
            authPreferences.setEncryptedPasswordDerivedKeyInitVector(
                biometricKeyName,
                Base64.encodeToString(
                    initVector,
                    Base64.DEFAULT
                )
            )
            authPreferences.setUseBiometrics(biometricKeyName, true)
            return true
        } catch (e: java.lang.Exception) {
            when (e) {
                is BadPaddingException,
                is IllegalBlockSizeException -> {
                    Log.e("Failed to encrypt the data with the generated key. ${e.message}")
                    return false
                }
                else -> throw e
            }
        }
    }

    fun generateBiometricsSecretKey(): Boolean {
        return try {
            KeystoreHelper().generateSecretKey(biometricKeyName)
            true
        } catch (e: KeystoreEncryptionException) {
            false
        }
    }

    /**
     * Can throw KeystoreEncryptionException or return null in case of KeyPermanentlyInvalidatedException
     */
    fun initBiometricsCipherForEncryption(): Cipher? {
        return KeystoreHelper().initCipherForEncryption(biometricKeyName)
    }

    /**
     * Can throw KeystoreEncryptionException or return null in case of KeyPermanentlyInvalidatedException
     */
    fun initBiometricsCipherForDecryption(): Cipher? {
        try {
            val initVector = authPreferences.getBiometricsKeyEncryptionInitVector(biometricKeyName)
            val cipher =  KeystoreHelper().initCipherForDecryption(
                biometricKeyName,
                Base64.decode(initVector, Base64.DEFAULT)
            )

            if(cipher == null){
                authPreferences.setUseBiometrics(biometricKeyName, false)
            }
            return cipher;
        }
        catch (e:KeystoreEncryptionException){
            authPreferences.setUseBiometrics(biometricKeyName,false)
            throw e
        }
    }

    //endregion

    fun createPasswordCheck(password: String): Boolean {
        // Create encryption data used for encryption and decryption with password derived key
        try {
            val (salt, iv) = EncryptionHelper.createEncryptionData()
            // Save encryption data
            val encodedSalt = Base64.encodeToString(salt, Base64.DEFAULT)
            val encodedIV = Base64.encodeToString(iv, Base64.DEFAULT)
            authPreferences.setPasswordEncryptionSalt(biometricKeyName, encodedSalt)
            authPreferences.setPasswordEncryptionInitVector(biometricKeyName, encodedIV)

            val passwordCheck = RandomUtil.randomString(20)
            Log.d("PasswordCheck: $passwordCheck")

            // Derive password key and encrypt password check
            val result = EncryptionHelper.encrypt(password, salt, iv, passwordCheck)
            authPreferences.setPasswordCheckEncrypted(biometricKeyName, result)
            authPreferences.setPasswordCheck(biometricKeyName, passwordCheck)
            return true
        } catch (e: EncryptionException) {
            return false
        }
    }

    fun checkPassword(password: String): Boolean {
        val encodedSalt = authPreferences.getPasswordEncryptionSalt(biometricKeyName)
        val encodedIV = authPreferences.getPasswordEncryptionInitVector(biometricKeyName)
        val encodedPasswordCheckEncrypted = authPreferences.getPasswordCheckEncrypted(biometricKeyName)
        val salt = Base64.decode(encodedSalt, Base64.DEFAULT)
        val iv = Base64.decode(encodedIV, Base64.DEFAULT)
        val passwordCheckEncrypted = Base64.decode(encodedPasswordCheckEncrypted, Base64.DEFAULT)
        // Derive password key and decrypt password check
        try {
            val decryptedString =
                EncryptionHelper.decrypt(password, salt, iv, passwordCheckEncrypted)
            val savedPasswordCheck = authPreferences.getPasswordCheck(biometricKeyName)
            return decryptedString.equals(savedPasswordCheck)
        } catch (e: EncryptionException) {
            return false
        }
    }

    suspend fun checkPasswordInBackground(password: String): Boolean = withContext(Dispatchers.Default) {
        return@withContext checkPassword(password)
    }

    suspend fun checkPasswordInBackground(cipher: Cipher): String? =
        withContext(Dispatchers.Default) {
            try {
                val encodedEncryptedPassword = getEncryptedPassword()
                var encryptedPassword = Base64.decode(encodedEncryptedPassword, Base64.DEFAULT)

                val decryptedByteArray = cipher.doFinal(encryptedPassword)
                val decryptedPasswordString = String(decryptedByteArray, charset("UTF-8"))

                val res = checkPassword(decryptedPasswordString)
                return@withContext if (res) {
                    decryptedPasswordString
                } else {
                    null
                }
            } catch (e: Exception) {
                when (e) {
                    is BadPaddingException,
                    is IllegalBlockSizeException -> {
                        Log.e("Failed to decrypt the data with the generated key. ${e.message}")
                        return@withContext null
                    }
                    else -> throw e
                }
            }
        }

    suspend fun encryptInBackground(password: String, toBeEncrypted: String): String? =
        withContext(Dispatchers.Default) {
            val encodedSalt = authPreferences.getPasswordEncryptionSalt(biometricKeyName)
            val encodedIV = authPreferences.getPasswordEncryptionInitVector(biometricKeyName)
            val salt = Base64.decode(encodedSalt, Base64.DEFAULT)
            val iv = Base64.decode(encodedIV, Base64.DEFAULT)

            // Derive password key and encrypt
            try {
                val encodedEncrypted = EncryptionHelper.encrypt(password, salt, iv, toBeEncrypted)
                return@withContext encodedEncrypted
            } catch (e: EncryptionException) {
                return@withContext null
            }
        }

    suspend fun decryptInBackground(password: String, encodedToBeDecrypted: String): String? =
        withContext(Dispatchers.Default) {
            val encodedSalt = authPreferences.getPasswordEncryptionSalt(biometricKeyName)
            val encodedIV = authPreferences.getPasswordEncryptionInitVector(biometricKeyName)
            val salt = Base64.decode(encodedSalt, Base64.DEFAULT)
            val iv = Base64.decode(encodedIV, Base64.DEFAULT)

            // Derive password key and decrypt
            val toBeDecryptedByteArray = Base64.decode(encodedToBeDecrypted, Base64.DEFAULT)
            try {
                val decrypted = EncryptionHelper.decrypt(password, salt, iv, toBeDecryptedByteArray)
                return@withContext decrypted
            } catch (e: EncryptionException) {
                return@withContext null
            }
        }

    suspend fun derivePasswordKeyInBackground(password: String): SecretKey? = withContext(Dispatchers.Default) {
        val encodedSalt = authPreferences.getPasswordEncryptionSalt(biometricKeyName)
        val salt = Base64.decode(encodedSalt, Base64.DEFAULT)
        // Derive password key
        try {
            val key = EncryptionHelper.generateKey(password, salt)
            return@withContext key
        } catch (e: EncryptionException) {
            return@withContext null
        }
    }

    suspend fun encryptInBackground(key: SecretKey, toBeEncrypted: String): String? = withContext(Dispatchers.Default) {
        val encodedIV = authPreferences.getPasswordEncryptionInitVector(biometricKeyName)
        val iv = Base64.decode(encodedIV, Base64.DEFAULT)

        // Derive password key and encrypt
        try {
            val encodedEncrypted = EncryptionHelper.encrypt(key, iv, toBeEncrypted)
            return@withContext encodedEncrypted
        } catch (e: EncryptionException) {
            return@withContext null
        }
    }

    suspend fun decryptInBackground(key: SecretKey, encodedToBeDecrypted: String): String? = withContext(Dispatchers.Default) {
        val encodedIV = authPreferences.getPasswordEncryptionInitVector(biometricKeyName)
        val iv = Base64.decode(encodedIV, Base64.DEFAULT)

        // Derive password key and decrypt
        val toBeDecryptedByteArray = Base64.decode(encodedToBeDecrypted, Base64.DEFAULT)
        try {
            val decrypted = EncryptionHelper.decrypt(key, iv, toBeDecryptedByteArray)
            return@withContext decrypted
        } catch (e: EncryptionException) {
            return@withContext null
        }
    }

    fun usePasscode(): Boolean {
        return authPreferences.getUsePasscode(biometricKeyName)
    }

    fun useBiometrics(): Boolean {
        return authPreferences.getUseBiometrics(biometricKeyName)
    }

    fun getEncryptedPassword(): String {
        return authPreferences.getEncryptedPassword(biometricKeyName)
    }

    fun setUsePassCode(passcodeUsed: Boolean) {
        authPreferences.setUsePasscode(biometricKeyName, passcodeUsed)
    }

}