package com.concordium.wallet.data.util

import android.util.Base64
import com.concordium.wallet.core.security.EncryptionException
import com.concordium.wallet.core.security.EncryptionHelper
import com.concordium.wallet.data.export.EncryptedExportData
import com.concordium.wallet.data.export.ExportEncryptionMetaData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ExportEncryptionHelper {

    /**
     * @exception EncryptionException
     */
    @Throws(EncryptionException::class)
    suspend fun encryptExportData(password: String, toBeEncrypted: String): EncryptedExportData = withContext(Dispatchers.Default) {
        val iterations = 100000
        val (salt, iv) = EncryptionHelper.createEncryptionData()
        val saltEncoded = Base64.encodeToString(salt, Base64.NO_WRAP)
        val ivEncoded = Base64.encodeToString(iv, Base64.NO_WRAP)
        val encryptionMetaData =
            ExportEncryptionMetaData(
                "AES-256",
                "PBKDF2WithHmacSHA256",
                iterations,
                saltEncoded,
                ivEncoded
            )
        val key = EncryptionHelper.generateKey(password, salt, iterations)
        // Encrypt (using NO_WRAP to avoid line break in the end)
        val cipherText = EncryptionHelper.encrypt(key, iv, toBeEncrypted, Base64.NO_WRAP)
        val encryptedExportData = EncryptedExportData(encryptionMetaData, cipherText)
        return@withContext encryptedExportData
    }

    @Throws(EncryptionException::class)
    suspend fun decryptExportData(password: String, encryptedExportData: EncryptedExportData): String = withContext(Dispatchers.Default) {
        val toBeEncryptedEncoded = encryptedExportData.cipherText
        val toBeEncrypted = Base64.decode(toBeEncryptedEncoded, Base64.DEFAULT)
        val iterations = encryptedExportData.metadata.iterations
        val saltEncoded = encryptedExportData.metadata.salt
        val ivEncoded = encryptedExportData.metadata.initializationVector
        val salt = Base64.decode(saltEncoded, Base64.DEFAULT)
        val iv = Base64.decode(ivEncoded, Base64.DEFAULT)
        // Assume encryptionMethod: AES-256 and keyDerivationMethod: PBKDF2WithHmacSHA256
        // Because that is the only thing we support
        val key = EncryptionHelper.generateKey(password, salt, iterations)
        val decrypted = EncryptionHelper.decrypt(key, iv, toBeEncrypted)
        return@withContext decrypted
    }

}