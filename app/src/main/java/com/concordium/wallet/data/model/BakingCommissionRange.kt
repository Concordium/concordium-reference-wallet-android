package com.concordium.wallet.data.model

import java.io.Serializable

data class BakingCommissionRange(
    val max: Double,
    val min: Double
) : Serializable