package com.concordium.wallet.data.export

import java.io.Serializable

data class ExportEncryptionMetaData(
    val encryptionMethod: String,
    val keyDerivationMethod: String,
    val iterations: Int,
    val salt: String,
    val initializationVector: String
) : Serializable {

    @Suppress("SENSELESS_COMPARISON")
    fun hasRequiredData(): Boolean {
        return encryptionMethod != null
                && keyDerivationMethod != null
                && iterations != null
                && salt != null
                && initializationVector != null
    }
}