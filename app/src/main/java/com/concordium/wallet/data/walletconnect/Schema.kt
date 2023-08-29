package com.concordium.wallet.data.walletconnect

sealed class Schema {
    data class ValueSchema(
        val type: String?,
        val value: String?
    ) : Schema()

    data class BrokenSchema(
        val type: String?,
        val value: BrokenValue
    ) : Schema() {
        data class BrokenValue(val type: String?, val data: List<Int>)
    }
}
