package com.concordium.wallet.data.model

data class CommissionRates(
    val bakingCommission: Double,
    val finalizationCommission: Double,
    val transactionCommission: Double
)