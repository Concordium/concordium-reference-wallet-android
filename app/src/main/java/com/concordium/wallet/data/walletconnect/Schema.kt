package com.concordium.wallet.data.walletconnect

sealed class Schema {
    data class ValueSchema(
        val type: String?,
        val value: String?
    ) : Schema()

    /**
     * In some cases the buffer object in Java Script has been serialized directly, instead of first
     * converting the buffer object to a hex string and thereafter converting the string to a
     * byte array.
     */
    data class BrokenSchema(
        val type: String?,
        val value: BrokenValue
    ) : Schema() {
        data class BrokenValue(val type: String?, val data: List<Int>)
    }
}
