package com.concordium.wallet.data.model

data class ProofZeroKnowledge(
    val type: AttributeType?,
    val attributeTag: AttributeTag?,
    val name: String?,
    val value: String?,
    val rawValue: String?,
    val description: String?,
    val title: String?,
    val status: Boolean?
)