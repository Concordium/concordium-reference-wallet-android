package com.concordium.wallet.data.model

import java.io.Serializable

data class CommissionRates(
    val bakingCommission: Double,
    val finalizationCommission: Double,
    val transactionCommission: Double
) : Serializable