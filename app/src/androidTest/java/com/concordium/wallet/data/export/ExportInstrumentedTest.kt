package com.concordium.wallet.data.export

import android.security.keystore.KeyProperties
import android.util.Base64
import com.concordium.wallet.core.security.EncryptionException
import com.concordium.wallet.data.util.ExportEncryptionHelper
import com.concordium.wallet.util.Log
import com.concordium.wallet.util.PerformanceUtil
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
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

class ExportInstrumentedTest {

    private val iterationCount = 100000
    private val keyLength = 256
    private val cipherTransformation =
        "${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_CBC}/PKCS7Padding"

    @Test
    fun testExport() {
        val password = "password"
        val input = "swrj4893 vna vr894 rv48 vsjef 0v2"

        runBlocking {
            val encryptedExportData = ExportEncryptionHelper.encryptExportData(password, input)
            val decrypted = ExportEncryptionHelper.decryptExportData(password, encryptedExportData)

            assertEquals(input, decrypted)
        }
    }

    @Test
    fun testExportExperiments() {
        val encData = createEncryptionData()
        val saltOutput =  Base64.encodeToString(encData.first, Base64.DEFAULT)
        val ivOutput =  Base64.encodeToString(encData.second, Base64.DEFAULT)

        val salt = "Lb9ul7JP2FzxZITi+5PebOM0VMZPyl/ogzRUIZBg3zM=\n"
        val iv = "Iq+RX1R0oMtD61n5MnaonQ==\n"

        PerformanceUtil.showDeltaTime("Before key generation")
        val saltAsBytes = Base64.decode(salt, Base64.DEFAULT)
        val key = generateKey("password", saltAsBytes)
        PerformanceUtil.showDeltaTime("After key generation")

        val ivAsBytes = Base64.decode(iv, Base64.DEFAULT)
        val encrypted = encrypt(key, ivAsBytes, "AES256")
        PerformanceUtil.showDeltaTime("After encryption")

        val encryptedAsBytes = Base64.decode(encrypted, Base64.DEFAULT)
        val decrypted = decrypt(key, ivAsBytes, encryptedAsBytes)
        PerformanceUtil.showDeltaTime("After decryption")

        assertEquals("sQCDJmYQ7h0TIE2g1GgPqg==\n", encrypted)
        assertEquals("AES256", decrypted)

        assertEquals("A5Si7eMyyaE+uC6bJGMWBMMd+Xi04vD70sVJlE+deaU=\n", pbkdf2("password", "salt", 100000, 32))

    }

    @Throws(
        NoSuchAlgorithmException::class,
        InvalidKeySpecException::class
    )
    fun pbkdf2(
        password: String,
        salt: String,
        iterations: Int,
        keyLength: Int
    ): String? {
        val chars = password.toCharArray()
        val spec = PBEKeySpec(chars, salt.toByteArray(), iterations, keyLength * 8)
        val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = skf.generateSecret(spec).encoded
        val encoded = Base64.encodeToString(hash, Base64.DEFAULT)
        Log.d("Encoded: $encoded")
        return encoded
    }

    fun generateKey(
        password: String,
        salt: ByteArray
    ): SecretKey {
        val keyBytes = generateKeyAsByteArray(password, salt)
        val key: SecretKey = SecretKeySpec(keyBytes, "AES")
        return key
    }

    fun generateKeyAsByteArray(
        password: String,
        salt: ByteArray
    ): ByteArray {
        try {
            val keySpec: KeySpec =
                PBEKeySpec(password.toCharArray(), salt,
                    iterationCount,
                    keyLength
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

    fun encrypt(
        key: SecretKey,
        iv: ByteArray,
        toBeEncrypted: String
    ): String {
        try {
            val toBeEncryptedByteArray = toBeEncrypted.toByteArray(charset("UTF-8"))
            val cipher = Cipher.getInstance(cipherTransformation)
            val ivParams = IvParameterSpec(iv)
            cipher.init(Cipher.ENCRYPT_MODE, key, ivParams)
            val cipherText = cipher.doFinal(toBeEncryptedByteArray)
            val encodedEncrypted = Base64.encodeToString(cipherText, Base64.DEFAULT)
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

    fun decrypt(
        key: SecretKey,
        iv: ByteArray,
        toBeDecrypted: ByteArray
    ): String {
        try {
            val cipher = Cipher.getInstance(cipherTransformation)
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



    //************************************************************

    fun createEncryptionData(): Pair<ByteArray, ByteArray> {
        val saltLength = keyLength / 8 // same size as key output
        val random = SecureRandom()
        val salt = ByteArray(saltLength)
        random.nextBytes(salt)

        try {
            val cipher = Cipher.getInstance(cipherTransformation)
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

}
