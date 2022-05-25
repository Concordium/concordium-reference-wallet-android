package com.concordium.wallet.data.model

import java.io.Serializable

data class ChainParameters(
    val mintPerPayday: Double,
    val poolOwnerCooldown: Long,
    val capitalBound: Double,
    val rewardPeriodLength: Int,
    val transactionCommissionLPool: Double,
    val foundationAccountIndex: Int,
    val finalizationCommissionLPool: Double,
    val delegatorCooldown: Long,
    val bakingCommissionLPool: Double,
    val accountCreationLimit: Int,
    val electionDifficulty: Double,
    val minimumEquityCapital: String,
    val bakingCommissionRange: BakingCommissionRange,
    val finalizationCommissionRange: FinalizationCommissionRange,
    val transactionCommissionRange: TransactionCommissionRange
) : Serializable
