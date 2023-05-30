package com.concordium.wallet.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable


@Entity(tableName = "recipient_table")
data class Recipient(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    var name: String,
    var address: String
) : Serializable {
    fun displayName(): String {
        if (name.isNullOrEmpty()) {
            return address
        } else {
            return name
        }
    }
}