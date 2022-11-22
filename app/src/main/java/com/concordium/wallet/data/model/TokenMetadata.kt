package com.concordium.wallet.data.model

import java.io.Serializable

data class TokenMetadata(
    val decimals: Int,
    val description: String,
    val name: String,
    val symbol: String,
    val thumbnail: Thumbnail,
    val unique: Boolean
) : Serializable

data class Thumbnail(
    val url: String
) : Serializable
