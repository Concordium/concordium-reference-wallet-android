package com.concordium.wallet.data.model

import java.io.Serializable

data class AccountBaker (
    val bakerId: Int,
    val stakedAmount: String,
    val pendingChange: PendingChange
): Serializable
