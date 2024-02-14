package com.concordium.wallet.data.model

import java.io.Serializable
import java.math.BigInteger

data class BakerPoolStatus(
    val poolType: String,
    val bakerId: Int,
    val bakerEquityCapital: BigInteger,
    val delegatedCapitalCap: BigInteger,
    val bakerAddress: String,
    val delegatedCapital: BigInteger,
    val currentPaydayStatus: PayDayStatus,
    val poolInfo: BakerPoolInfo,
    val bakerStakePendingChange: BakerStakePendingChange,
    val minimumEquityCapital: BigInteger
) : Serializable
