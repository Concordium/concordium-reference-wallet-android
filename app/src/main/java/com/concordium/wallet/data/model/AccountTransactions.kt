package com.concordium.wallet.data.model

data class AccountTransactions(
    val order: String,
    val from: Int,
    val limit: Int,
    val count: Int,
    val transactions: List<RemoteTransaction>
)
