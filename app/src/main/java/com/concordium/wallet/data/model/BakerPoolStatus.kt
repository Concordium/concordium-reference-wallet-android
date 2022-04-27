package com.concordium.wallet.data.model

import java.io.Serializable

data class BakerPoolStatus(
    val poolType: String,
    val bakerId: Int,
    val bakerEquityCapital: String,
    val delegatedCapitalCap: String,
    val bakerAddress: String,
    val delegatedCapital: String,
    val currentPaydayStatus: PayDayStatus,
    val poolInfo: BakerPoolInfo,
    val bakerStakePendingChange: BakerStakePendingChange
) : Serializable
