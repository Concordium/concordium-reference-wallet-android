package com.concordium.wallet.data.walletconnect

sealed class Schema {
    data class ValueSchema(
        val type: String?,
        val value: String?
    ) : Schema()

    /**
     * For unknown reasons sometimes the schema comes in bad format, after some research it looks
     * like a broken buffer issue, in order to not crash and successfully handle transactions
     * we handle this specific case
     */
    data class BrokenSchema(
        val type: String?,
        val value: BrokenValue
    ) : Schema() {
        data class BrokenValue(val type: String?, val data: List<Int>)
    }
}
