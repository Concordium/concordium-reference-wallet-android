package com.concordium.wallet.data.room

import androidx.room.*

@Dao
interface AccountContractDao {
    @Query("SELECT * FROM account_contract_table WHERE account_address = :accountAddress AND contract_index = :contractIndex")
    suspend fun find(accountAddress: String, contractIndex: String): AccountContract?

    @Query("SELECT * FROM account_contract_table WHERE account_address = :accountAddress")
    suspend fun find(accountAddress: String): List<AccountContract>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg accountContract: AccountContract)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(vararg accountContract: AccountContract)

    @Delete
    suspend fun delete(accountContract: AccountContract)
}
