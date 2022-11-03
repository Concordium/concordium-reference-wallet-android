package com.concordium.wallet.data.room

import androidx.room.*

@Dao
interface ContractTokenDao {
    @Query("SELECT * FROM contract_token_table WHERE contract_index = :contractIndex")
    fun getTokensByContractIndex(contractIndex: String): List<ContractToken>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(vararg contractToken: ContractToken)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(vararg contractToken: ContractToken)

    @Delete
    suspend fun delete(contractToken: ContractToken)
}
