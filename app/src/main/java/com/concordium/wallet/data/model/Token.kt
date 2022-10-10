package com.concordium.wallet.data.model

import java.io.Serializable

data class Token (
    val imageUrl: String,
    val name: String,
    val shortName: String,
    val balance: Long?
): Serializable {
    fun isCCDToken() : Boolean {
        return true
    }
}
