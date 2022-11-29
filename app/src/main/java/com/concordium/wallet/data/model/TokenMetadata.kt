package com.concordium.wallet.data.model
import com.concordium.wallet.data.room.ContractToken
import java.io.Serializable

data class TokenMetadata(
    val decimals: Int,
    val description: String,
    val name: String,
    val symbol: String?,
    val thumbnail: Thumbnail,
    val unique: Boolean,
    var display: UrlHolder?,
    var artifact: UrlHolder?,
    var attributes: List<Attribute>?,
    var assets: List<ContractToken>?,
    val localization: Map<String, Localization>?,
) : Serializable

data class Thumbnail(
    val url: String
) : Serializable

data class UrlHolder(
    val url: String?,
) : Serializable

data class Attribute(
    val type: String?,
    val name: String?,
    val value: String?,
) : Serializable

data class Localization(
    val url: String?,
    val hash: String?,
) : Serializable
