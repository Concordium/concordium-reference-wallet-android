package com.concordium.wallet.data.model

import java.io.Serializable

data class AccountBaker (
    val restakeEarnings: Boolean,
    val bakerId: Int,
    val bakerPoolInfo: BakerPoolInfo,
    val stakedAmount: String,
    val pendingChange: PendingChange,
    val bakerAggregationVerifyKey: String,
    val bakerElectionVerifyKey: String,
    val bakerSignatureVerifyKey: String
): Serializable
