package com.concordium.wallet.data.room

import  androidx.room.*
import com.concordium.wallet.data.model.TokenMetadata
import com.concordium.wallet.data.room.typeconverter.ContractTypeConverters
import java.io.Serializable

@Entity(
    tableName = "contract_token_table",
    indices = [Index(value = ["contract_index", "token_id"], unique = true)]
)
@TypeConverters(ContractTypeConverters::class)
data class ContractToken(
    @PrimaryKey(autoGenerate = true)
    var id: Int,
    @ColumnInfo(name = "contract_index")
    val contractIndex: String,
    @ColumnInfo(name = "token_id")
    val tokenId: String,
    @ColumnInfo(name = "is_fungible")
    val isFungible: Boolean,
    @ColumnInfo(name = "token_metadata")
    val tokenMetadata: TokenMetadata?


) : Serializable
