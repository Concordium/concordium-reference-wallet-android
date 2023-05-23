package com.concordium.wallet.data.model

import java.io.Serializable

data class PayDayStatus(
    val finalizationLive: Boolean,
    val effectiveStake: String,
    val transactionFeesEarned: Long,
    val bakerEquityCapital: String,
    val lotteryPower: Double,
    val blocksBaked: Long,
    val delegatedCapital: String
) : Serializable

/*
{
    "finalizationLive": true,
    "effectiveStake": "3001500480508",
    "transactionFeesEarned": "0",
    "bakerEquityCapital": "3000000000000",
    "lotteryPower": 0.20008001762276778,
    "blocksBaked": 27,
    "delegatedCapital": "1500480508"
}*/
