package com.concordium.wallet.data.model

import java.math.BigDecimal

data class RemoteTransaction(
    val id: Int,
    val origin: TransactionOrigin,
    val blockHash: String,
    val blockTime: Double,
    val transactionHash: String?,
    val subtotal: BigDecimal?,
    val cost: BigDecimal?,
    val total: BigDecimal,
    val energy: Long?,
    val details: TransactionDetails,
    val encrypted: TransactionEncrypted?

)