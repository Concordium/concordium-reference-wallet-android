package com.concordium.wallet.data.model

data class CIS2Tokens(
    val count: Int,
    val from: Int,
    val limit: Int,
    val tokens: List<Token>
)
