package com.concordium.wallet.data.model

import java.io.Serializable

data class BakerPoolInfo(
    val openStatus: String,
    val metadataUrl: String? = null,
    val commissionRates: CommissionRates? = null

) : Serializable {
    companion object {
        val OPEN_STATUS_OPEN_FOR_ALL = "openForAll"
        val OPEN_STATUS_CLOSED_FOR_NEW = "closedForNew"
        val OPEN_STATUS_CLOSED_FOR_ALL = "closedForAll"
    }
}
