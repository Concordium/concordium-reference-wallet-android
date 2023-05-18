package com.concordium.wallet.data.model

import com.concordium.wallet.CBORUtil
import java.io.Serializable
import java.math.BigDecimal
import java.util.*


data class Transaction(
    val source: TransactionSource,
    val timeStamp: Date,
    var title: String = "",
    val subtotal: BigDecimal?,
    val cost: BigDecimal?,
    val total: BigDecimal,
    var transactionStatus: TransactionStatus,
    var outcome: TransactionOutcome,
    var blockHashes: List<String>?,
    var transactionHash: String?,
    var rejectReason: String?,
    val events: List<String>?,
    val fromAddress: String?,
    val toAddress: String?,
    var fromAddressTitle: String?,
    var toAddressTitle: String?,
    val submissionId: String?,
    val origin: TransactionOrigin?,
    val details: TransactionDetails?,
    val encrypted: TransactionEncrypted?
) : Serializable {

    fun isSameAccount() : Boolean {
        return fromAddress == toAddress
    }

    fun isRemoteTransaction(): Boolean {
        return transactionStatus == TransactionStatus.FINALIZED
    }

    fun isBakerTransfer(): Boolean {
        return details?.type == TransactionType.LOCAL_BAKER
    }

    fun isDelegationTransfer(): Boolean {
        return details?.type == TransactionType.LOCAL_DELEGATION
    }

    fun isSimpleTransfer(): Boolean {
        return details?.type == TransactionType.TRANSFER
    }

    fun isTransferToSecret(): Boolean {
        return details?.type == TransactionType.TRANSFERTOENCRYPTED
    }

    fun isTransferToPublic(): Boolean {
        return details?.type == TransactionType.TRANSFERTOPUBLIC
    }

    fun isEncryptedTransfer(): Boolean {
        return details?.type == TransactionType.ENCRYPTEDAMOUNTTRANSFER || details?.type == TransactionType.ENCRYPTEDAMOUNTTRANSFERWITHMEMO
    }

    fun isOriginSelf(): Boolean {
        return origin?.type == TransactionOriginType.Self
    }


    fun getTotalAmountForRegular(): BigDecimal {
        if (transactionStatus == TransactionStatus.ABSENT) {
            return BigDecimal.ZERO
        } else if (outcome == TransactionOutcome.Reject) {
            return if (cost == null) BigDecimal.ZERO else -cost
        }
        return total
    }

    fun getTotalAmountForShielded() : BigDecimal {
        if(subtotal == null)
            return BigDecimal.ZERO
        else{
            return -subtotal
        }
    }

    fun isReward(): Boolean {
        return origin?.type == TransactionOriginType.Reward
    }

    fun isFinalizedReward(): Boolean {
        return details?.type == TransactionType.FINALIZATIONREWARD && isReward()
    }

    fun getDecryptedMemo(): String{
        return details?.memo?.let { return CBORUtil.decodeHexAndCBOR(it) } ?: ""
    }

    fun hasMemo(): Boolean{
        return details != null && details.memo != null && details.memo.length > 0
    }
}