package com.concordium.wallet.data.model

import java.math.BigDecimal

data class TransferSubmissionStatus(
    val status: TransactionStatus,
    val outcome: TransactionOutcome?,
    val amount: BigDecimal?,
    val sender: String?,
    val to: String?,
    val cost: BigDecimal?,
    val transactionHash: String?,
    val blockHashes: List<String>?,
    val rejectReason: String?,
    val encryptedAmount: String?,
    val aggregatedIndex: Int?
)