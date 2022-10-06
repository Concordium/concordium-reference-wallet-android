package com.concordium.wallet.data.room

import androidx.room.*

@Dao
interface ContractTokenDao {
    @Query("SELECT * FROM contract_token_table WHERE contract_index = :contractIndex")
    fun getTokens(contractIndex: String): List<ContractToken>

    @Query("SELECT * FROM contract_token_table WHERE contract_index = :contractIndex AND is_fungible = :isFungible")
    fun getTokens(contractIndex: String, isFungible: Boolean): List<ContractToken>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg contractToken: ContractToken)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(vararg contractToken: ContractToken)

    @Delete
    suspend fun delete(contractToken: ContractToken)

    @Query("SELECT * FROM contract_token_table WHERE contract_index = :contractIndex AND token_id = :tokenId")
    fun find(contractIndex: String, tokenId: String): ContractToken?
}
