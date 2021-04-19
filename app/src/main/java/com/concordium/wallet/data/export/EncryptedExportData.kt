package com.concordium.wallet.data.export

import java.io.Serializable

data class EncryptedExportData(
    val metadata: ExportEncryptionMetaData,
    val cipherText: String
) : Serializable {

    @Suppress("SENSELESS_COMPARISON")
    fun hasRequiredData(): Boolean {
        return metadata != null
                && metadata.hasRequiredData()
                && cipherText != null
    }
}