package com.concordium.wallet.data.model

import java.io.Serializable

data class Token (
    val id: String,
    var token: String,
    val totalSupply: String,
    var tokenMetadata: TokenMetadata?,
    var isSelected: Boolean = false
): Serializable {
    fun isCCDToken() : Boolean {
        return true
    }
}
