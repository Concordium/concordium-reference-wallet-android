package com.concordium.wallet.data.model

data class PassiveDelegation(
    val allPoolTotalCapital: String,
    val commissionRates: CommissionRates,
    val currentPaydayDelegatedCapital: String,
    val currentPaydayTransactionFeesEarned: String,
    val delegatedCapital: String,
    val poolType: String
)