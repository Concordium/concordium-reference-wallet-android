package com.concordium.wallet.data.model

import java.io.Serializable
import java.math.BigInteger

data class PassiveDelegation(
    val allPoolTotalCapital: BigInteger,
    val commissionRates: CommissionRates,
    val currentPaydayDelegatedCapital: BigInteger,
    val currentPaydayTransactionFeesEarned: BigInteger,
    val delegatedCapital: BigInteger,
    val poolType: String
) : Serializable
