package com.concordium.wallet.data.model

import java.io.Serializable

data class Token (
    val id: Int,
    var token: String,
    val totalSupply: String,
    var imageUrl: String?,
    var isSelected: Boolean? = false

/*
    val imageUrl: String,
    val name: String,
    val shortName: String,
    val balance: Long?,
    var isSelected: Boolean? = false
*/
): Serializable {
    fun isCCDToken() : Boolean {
        return true
    }
}
