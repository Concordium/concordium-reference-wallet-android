package com.concordium.wallet.data.model

import java.math.BigInteger

data class TransferCost(
    val energy: BigInteger,
    val cost: String,
    val success: Boolean?
)