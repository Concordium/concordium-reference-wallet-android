package com.concordium.wallet.data.model

import java.io.Serializable

data class TransactionDetails(
    val type: TransactionType,
    val description: String,
    val outcome: TransactionOutcome,
    val rejectReason: String,
    val events: List<String>?,
    val transferSource: String?,
    val transferDestination: String?,
    val transferAmount: Long?,
    val newIndex: Int?,
    val memo: String?,
    val newSelfEncryptedAmount: String?,
    val inputEncryptedAmount: String?,
    val encryptedAmount: String?,
    val aggregatedIndex: Int?,
    val amountSubtracted: String?,
    val amountAdded: String?
) : Serializable
