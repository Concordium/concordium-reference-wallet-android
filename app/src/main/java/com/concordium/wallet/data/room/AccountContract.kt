package com.concordium.wallet.data.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "account_contract_table", indices = [Index(value = ["account_address", "contract_index"], unique = true)])
data class AccountContract(
    @PrimaryKey(autoGenerate = true)
    var id: Int,
    @ColumnInfo(name = "account_address")
    val accountAddress: String,
    @ColumnInfo(name = "contract_index")
    val contractIndex: String
) : Serializable
