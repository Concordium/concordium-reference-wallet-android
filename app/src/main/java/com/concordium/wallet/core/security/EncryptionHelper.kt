package com.concordium.wallet.core.security

import android.security.keystore.KeyProperties
import android.util.Base64
import com.concordium.wallet.util.Log
import java.io.UnsupportedEncodingException
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.spec.InvalidKeySpecException
import java.security.spec.KeySpec
import javax.crypto.*
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Encryption functions. Beware that these are used for both authentication and export.
 */
object EncryptionHelper {

    private const val DEFAULT_ITERATION_COUNT = 10000
    private const val KEY_LENGTH = 256
    private const val CIPHER_TRANSFORMATION =
        "${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_CBC}/PKCS7Padding"

    /**
     * @exception EncryptionException
     */
    fun createEncryptionData(): Pair<ByteArray, ByteArray> {
        val saltLength = KEY_LENGTH / 8 // same size as key output
        val random = SecureRandom()
        val salt = ByteArray(saltLength)
        random.nextBytes(salt)

        try {
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            val iv = ByteArray(cipher.blockSize)
            random.nextBytes(iv)

            return Pair(salt, iv)
        } catch (e: Exception) {
            when (e) {
                is NoSuchAlgorithmException,
                is NoSuchPaddingException -> {
                    Log.d("Failed creating encryption data", e)
                    throw EncryptionException(e)
                }

                else -> throw e
            }
        }
    }

    /**
     * @exception EncryptionException
     */
    @Throws(EncryptionException::class)
    fun generateKey(
        password: String,
        salt: ByteArray,
        iterationCount: Int = DEFAULT_ITERATION_COUNT
    ): SecretKey {
        val keyBytes = generateKeyAsByteArray(password, salt, iterationCount)
        val key: SecretKey = SecretKeySpec(keyBytes, "AES")
        return key
    }

    /**
     * @exception EncryptionException
     */
    fun generateKeyAsByteArray(
        password: String,
        salt: ByteArray,
        iterationCount: Int = DEFAULT_ITERATION_COUNT
    ): ByteArray {
        try {
            val keySpec: KeySpec =
                PBEKeySpec(
                    password.toCharArray(), salt,
                    iterationCount,
                    KEY_LENGTH
                )
            val keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val keyBytes = keyFactory.generateSecret(keySpec).encoded
            return keyBytes

        } catch (e: Exception) {
            when (e) {
                is NoSuchAlgorithmException,
                is InvalidKeySpecException -> {
                    Log.d("Failed to create key", e)
                    throw EncryptionException(e)
                }

                else -> throw e
            }
        }
    }

    /**
     * @exception EncryptionException
     */
    fun encrypt(
        key: SecretKey,
        iv: ByteArray,
        toBeEncrypted: String,
        base64EncodeFlags: Int = Base64.DEFAULT
    ): String {
        try {
            val toBeEncryptedByteArray = toBeEncrypted.toByteArray(charset("UTF-8"))
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            val ivParams = IvParameterSpec(iv)
            cipher.init(Cipher.ENCRYPT_MODE, key, ivParams)
            val cipherText = cipher.doFinal(toBeEncryptedByteArray)
            val encodedEncrypted = Base64.encodeToString(cipherText, base64EncodeFlags)
            Log.d("Encrypted text: $encodedEncrypted")
            return encodedEncrypted

        } catch (e: Exception) {
            when (e) {
                is NoSuchAlgorithmException,
                is NoSuchPaddingException,
                is InvalidAlgorithmParameterException,
                is InvalidKeyException,
                is UnsupportedEncodingException,
                is BadPaddingException,
                is IllegalBlockSizeException -> {
                    Log.d("Failed to encrypt data", e)
                    throw EncryptionException(e)
                }

                else -> throw e
            }
        }
    }

    /**
     * @exception EncryptionException
     */
    fun decrypt(
        key: SecretKey,
        iv: ByteArray,
        toBeDecrypted: ByteArray
    ): String {
        try {
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            val ivParams = IvParameterSpec(iv)
            cipher.init(Cipher.DECRYPT_MODE, key, ivParams)
            val plainText = cipher.doFinal(toBeDecrypted)
            val decryptedString = String(plainText, charset("UTF-8"))
            Log.d("Decrypted text: $decryptedString")
            return decryptedString
        } catch (e: Exception) {
            when (e) {
                is NoSuchAlgorithmException,
                is NoSuchPaddingException,
                is InvalidAlgorithmParameterException,
                is InvalidKeyException,
                is UnsupportedEncodingException,
                is BadPaddingException,
                is IllegalBlockSizeException -> {
                    Log.d("Failed to decrypt data", e)
                    throw EncryptionException(e)
                }

                else -> throw e
            }
        }
    }

    /**
     * @exception EncryptionException
     */
    fun encrypt(
        password: String,
        salt: ByteArray,
        iv: ByteArray,
        toBeEncrypted: String,
        base64EncodeFlags: Int = Base64.DEFAULT
    ): String {
        val key = generateKey(password, salt)
        return encrypt(key, iv, toBeEncrypted, base64EncodeFlags)
    }

    /**
     * @exception EncryptionException
     */
    fun decrypt(
        password: String,
        salt: ByteArray,
        iv: ByteArray,
        toBeDecrypted: ByteArray
    ): String {
        val key = generateKey(password, salt)
        return decrypt(key, iv, toBeDecrypted)
    }

}