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
    val minimumEquityCapital: Long
) : Serializable




/*

{
    "rewardParameters": {
        "mintDistribution": {
            "bakingReward": 0.45,
            "finalizationReward": 0.35
        },
        "transactionFeeDistribution": {
            "gasAccount": 0.45,
            "baker": 0.45
        },
        "gASRewards": {
            "chainUpdate": 5.0e-3,
            "accountCreation": 2.0e-3,
            "baker": 0.25,
            "finalizationProof": 5.0e-3
        }
    },
    "microGTUPerEuro": {
        "denominator": 472657102571,
        "numerator": 12054725891240307000
    },
    "leverageBound": {
        "denominator": 1,
        "numerator": 2
    },
    "bakingCommissionRange": {
        "max": 5.0e-2,
        "min": 5.0e-2
    },
    "finalizationCommissionRange": {
        "max": 1.0,
        "min": 1.0
    },
    "euroPerEnergy": {
        "denominator": 1000000,
        "numerator": 1
    },
    "transactionCommissionRange": {
        "max": 5.0e-2,
        "min": 5.0e-2
    },
}

 */