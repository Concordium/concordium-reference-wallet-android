package com.concordium.wallet.data.room

import  androidx.room.*
import java.io.Serializable

@Entity(tableName = "contract_token_table", indices = [Index(value = ["contract_index", "token_id"], unique = true)])
data class ContractToken(
    @PrimaryKey(autoGenerate = true)
    var id: Int,
    @ColumnInfo(name = "contract_index")
    val contractIndex: String,
    @ColumnInfo(name = "token_id")
    val tokenId: String,
    @ColumnInfo(name = "is_fungible")
    val isFungible: Boolean
) : Serializable
