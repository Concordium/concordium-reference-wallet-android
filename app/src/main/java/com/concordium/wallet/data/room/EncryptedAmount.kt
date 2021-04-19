package com.concordium.wallet.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable


@Entity(tableName = "encrypted_amount_table")
data class EncryptedAmount(
    @PrimaryKey
    val encryptedkey: String,
    var amount: String?
) : Serializable