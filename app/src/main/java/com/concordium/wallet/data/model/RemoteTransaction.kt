package com.concordium.wallet.data.model

import java.math.BigInteger

data class RemoteTransaction(
    val id: Int,
    val origin: TransactionOrigin,
    val blockHash: String,
    val blockTime: Double,
    val transactionHash: String?,
    val subtotal: BigInteger?,
    val cost: BigInteger?,
    val total: BigInteger,
    val energy: Long?,
    val details: TransactionDetails,
    val encrypted: TransactionEncrypted?

)