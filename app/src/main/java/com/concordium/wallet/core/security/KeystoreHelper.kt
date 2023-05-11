package com.concordium.wallet.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import com.concordium.wallet.util.Log
import java.io.IOException
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import java.security.UnrecoverableKeyException
import java.security.cert.CertificateException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.NoSuchPaddingException
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

class KeystoreHelper {

    companion object {
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
    }

    private fun generateSecretKeyWithSpecs(keyGenParameterSpec: KeyGenParameterSpec) {
        val keyGenerator =
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    }

    fun generateSecretKey(keyName: String) {
        try {
            val keyProperties = KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            val builder = KeyGenParameterSpec.Builder(keyName, keyProperties)
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setUserAuthenticationRequired(true)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                .setInvalidatedByBiometricEnrollment(true)

            generateSecretKeyWithSpecs(builder.build())

        } catch (e: Exception) {
            when (e) {
                is NoSuchAlgorithmException,
                is NoSuchProviderException,
                is InvalidAlgorithmParameterException,
                is CertificateException,
                is IOException -> {
                    Log.d("Failed to generate secret key", e)
                    throw KeystoreEncryptionException("Failed to generate secret key", e)
                }

                else -> throw e
            }
        }
    }

    private fun setupCipher(): Cipher {
        val cipherString =
            "${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_CBC}/${KeyProperties.ENCRYPTION_PADDING_PKCS7}"
        return Cipher.getInstance(cipherString)
    }

    fun getSecretKey(keyName: String): SecretKey? {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE)
        if (keyStore == null) {
            return null
        }
        keyStore.load(null)
        val key = keyStore.getKey(keyName, null)
        if (key == null) {
            return null
        }
        return key as SecretKey
    }

    fun initCipherForEncryption(keyName: String): Cipher? {
        try {
            val cipher = setupCipher()
            val secretKey = getSecretKey(keyName)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            return cipher
        } catch (e: Exception) {
            when (e) {
                is KeyPermanentlyInvalidatedException -> return null
                is KeyStoreException,
                is CertificateException,
                is UnrecoverableKeyException,
                is IOException,
                is NoSuchAlgorithmException,
                is NoSuchPaddingException,
                is InvalidKeyException -> {
                    Log.d("Failed to init Cipher", e)
                    throw KeystoreEncryptionException("Failed to init Cipher", e)
                }

                else -> throw e
            }
        }
    }

    fun initCipherForDecryption(keyName: String, initVector: ByteArray): Cipher? {
        try {
            val cipher = setupCipher()
            val secretKey = getSecretKey(keyName)
            val ivParams = IvParameterSpec(initVector)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParams)
            return cipher
        } catch (e: Exception) {
            when (e) {
                is KeyPermanentlyInvalidatedException -> return null
                is KeyStoreException,
                is CertificateException,
                is UnrecoverableKeyException,
                is IOException,
                is NoSuchAlgorithmException,
                is NoSuchPaddingException,
                is InvalidAlgorithmParameterException,
                is InvalidKeyException -> {
                    Log.d("Failed to init Cipher", e)
                    throw KeystoreEncryptionException("Failed to init Cipher", e)
                }

                else -> throw e
            }
        }
    }


}