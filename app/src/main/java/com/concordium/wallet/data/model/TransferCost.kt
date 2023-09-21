package com.concordium.wallet.data.model

data class TransferCost(
    val energy: Long,
    val cost: String,
    val success: Boolean?
)