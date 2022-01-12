package com.concordium.wallet.data.export

import java.io.Serializable


data class ExportData(
    val type: String,
    val v: Int,
    val value: ExportValue,
    val environment: String
) : Serializable {

    @Suppress("SENSELESS_COMPARISON")
    fun hasRequiredData(): Boolean {
        return type != null
                && v != null
                && value != null
                && value.hasRequiredData()
    }

    fun hasRequiredIdentities(): Boolean {
        return value != null && value.hasRequiredIdentities()
    }
}