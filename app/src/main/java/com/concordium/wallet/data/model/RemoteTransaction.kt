package com.concordium.wallet.data.model

data class RemoteTransaction(
    val id: Int,
    val origin: TransactionOrigin,
    val blockHash: String,
    val blockTime: Double,
    val transactionHash: String?,
    val subtotal: Long?,
    val cost: Long?,
    val total: Long,
    val energy: Long?,
    val details: TransactionDetails,
    val encrypted: TransactionEncrypted?

)