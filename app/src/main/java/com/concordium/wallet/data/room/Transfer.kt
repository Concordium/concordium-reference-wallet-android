package com.concordium.wallet.data.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.concordium.wallet.data.model.AccountNonce
import com.concordium.wallet.data.model.TransactionOutcome
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.model.TransactionType
import com.concordium.wallet.data.room.typeconverter.TransactionTypeConverters
import java.io.Serializable
import java.math.BigDecimal


@Entity(tableName = "transfer_table")
@TypeConverters(TransactionTypeConverters::class)
data class Transfer(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    @ColumnInfo(name = "account_id")
    val accountId: Int,
    val amount: BigDecimal,
    var cost: BigDecimal,
    @ColumnInfo(name = "from_address")
    var fromAddress: String,
    @ColumnInfo(name = "to_address")
    val toAddress: String,
    val expiry: Long,   //seconds
    val memo: String?,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,    //millis
    @ColumnInfo(name = "submission_id")
    val submissionId: String,
    @ColumnInfo(name = "transaction_status")
    var transactionStatus: TransactionStatus,
    var outcome: TransactionOutcome,
    var transactionType: TransactionType,
    var newSelfEncryptedAmount: String?,
    var newStartIndex: Int,
    var nonce: AccountNonce?

) : Serializable