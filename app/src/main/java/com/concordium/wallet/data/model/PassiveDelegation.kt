package com.concordium.wallet.data.model

import java.io.Serializable

data class PassiveDelegation(
    val allPoolTotalCapital: String,
    val commissionRates: CommissionRates,
    val currentPaydayDelegatedCapital: String,
    val currentPaydayTransactionFeesEarned: String,
    val delegatedCapital: String,
    val poolType: String
) : Serializable