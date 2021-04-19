package com.concordium.wallet.data.model

import java.io.Serializable

data class Schedule(
    val timestamp: Long,
    val amount: String,
    val transactions: List<String>
) : Serializable
