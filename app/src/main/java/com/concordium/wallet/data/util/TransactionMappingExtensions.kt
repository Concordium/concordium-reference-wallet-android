package com.concordium.wallet.data.util

import com.concordium.wallet.data.model.*
import com.concordium.wallet.data.room.Transfer
import java.util.*
import kotlin.math.abs

fun RemoteTransaction.toTransaction() = Transaction(
    source = TransactionSource.Remote,
    timeStamp = Date(blockTime.toLong() * 1000),
    subtotal = subtotal,
    cost = cost,
    total = total,
    transactionStatus = TransactionStatus.FINALIZED,
    outcome = details.outcome,
    blockHashes = arrayListOf(blockHash),
    transactionHash = transactionHash,
    rejectReason = details.rejectReason,
    events = details.events,
    fromAddress = details.transferSource,
    toAddress = details.transferDestination,
    fromAddressTitle = "",
    toAddressTitle = "",
    submissionId = null,
    origin = origin,
    details = details,
    encrypted = encrypted

)

fun Transfer.toTransaction() = Transaction(
    source = TransactionSource.Local,
    timeStamp = Date(createdAt),
    subtotal = amount,
    cost = cost,
    total = -(amount + cost),
    transactionStatus = transactionStatus,
    outcome = outcome,
    blockHashes = null,
    transactionHash = null,
    rejectReason = null,
    events = null,
    fromAddress = fromAddress,
    toAddress = toAddress,
    fromAddressTitle = "",
    toAddressTitle = "",
    submissionId = submissionId,
    origin = TransactionOrigin(TransactionOriginType.Self, null),
    details = TransactionDetails(transactionType, "", TransactionOutcome.UNKNOWN, "", null, null,null,null,null,null, null,null,null, null, null),
    encrypted = null
)