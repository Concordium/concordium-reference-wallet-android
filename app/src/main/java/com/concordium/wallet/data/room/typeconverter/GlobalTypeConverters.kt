package com.concordium.wallet.data.room.typeconverter

import androidx.room.TypeConverter
import com.concordium.wallet.data.model.ShieldedAccountEncryptionStatus
import com.concordium.wallet.data.model.TransactionOriginType
import com.concordium.wallet.data.model.TransactionOutcome
import com.concordium.wallet.data.model.TransactionStatus

class GlobalTypeConverters {

    @TypeConverter
    fun intToTransactionStatus(value: Int): TransactionStatus {
        return when (value) {
            0 -> TransactionStatus.RECEIVED
            1 -> TransactionStatus.ABSENT
            2 -> TransactionStatus.COMMITTED
            3 -> TransactionStatus.FINALIZED
            else -> TransactionStatus.UNKNOWN
        }
    }

    @TypeConverter
    fun transactionStatusToInt(transactionStatus: TransactionStatus): Int {
        return transactionStatus.code
    }

    @TypeConverter
    fun intToTransactionOutcome(value: Int): TransactionOutcome {
        return when (value) {
            0 -> TransactionOutcome.Success
            1 -> TransactionOutcome.Reject
            2 -> TransactionOutcome.Ambiguous
            else -> TransactionOutcome.UNKNOWN
        }
    }

    @TypeConverter
    fun transactionOutcomeToInt(transactionOutcome: TransactionOutcome): Int {
        return transactionOutcome.code
    }

    @TypeConverter
    fun intToTransactionOriginType(value: Int): TransactionOriginType {
        return when (value) {
            0 -> TransactionOriginType.Self
            1 -> TransactionOriginType.Account
            2 -> TransactionOriginType.Reward
            3 -> TransactionOriginType.None
            else -> TransactionOriginType.UNKNOWN
        }
    }

    @TypeConverter
    fun transactionOriginTypeToInt(transactionOriginType: TransactionOriginType): Int {
        return transactionOriginType.code
    }

    @TypeConverter
    fun intToShieldedAccountEncryptionStatus(value: Int): ShieldedAccountEncryptionStatus {
        return when (value) {
            ShieldedAccountEncryptionStatus.ENCRYPTED.code -> ShieldedAccountEncryptionStatus.ENCRYPTED
            ShieldedAccountEncryptionStatus.PARTIALLYDECRYPTED.code -> ShieldedAccountEncryptionStatus.PARTIALLYDECRYPTED
            ShieldedAccountEncryptionStatus.DECRYPTED.code -> ShieldedAccountEncryptionStatus.DECRYPTED
            else -> ShieldedAccountEncryptionStatus.DECRYPTED
        }
    }

    @TypeConverter
    fun shieldedAccountEncryptionStatusToInt(status: ShieldedAccountEncryptionStatus): Int {
        return status.code
    }

}