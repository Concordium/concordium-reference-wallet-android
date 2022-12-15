package com.concordium.wallet.data.model

data class ProofReveal(
    val type: AttributeType?,
    val attributeTag: AttributeTag?,
    val name: String?,
    val value: String?,
    val rawValue: String?,
    val status: Boolean?
)