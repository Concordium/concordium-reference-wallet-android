package com.concordium.wallet.data.model

data class TransferSubmissionStatus(
    val status: TransactionStatus,
    val outcome: TransactionOutcome?,
    val amount: Long?,
    val sender: String?,
    val to: String?,
    val cost: Long?,
    val transactionHash: String?,
    val blockHashes: List<String>?,
    val rejectReason: String?,
    val encryptedAmount: String?,
    val aggregatedIndex: Int?
)