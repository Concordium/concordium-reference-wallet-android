package com.concordium.wallet.data.model

import java.math.BigInteger

data class TransferSubmissionStatus(
    val status: TransactionStatus,
    val outcome: TransactionOutcome?,
    val amount: BigInteger?,
    val sender: String?,
    val to: String?,
    val cost: BigInteger?,
    val transactionHash: String?,
    val blockHashes: List<String>?,
    val rejectReason: String?,
    val encryptedAmount: String?,
    val aggregatedIndex: Int?
)