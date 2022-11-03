package com.concordium.wallet.data.room

import androidx.room.*

@Dao
interface AccountContractDao {
    @Query("SELECT * FROM account_contract_table WHERE account_address = :accountAddress")
    fun getContractsByAccountAddress(accountAddress: String): List<AccountContract>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(vararg accountContract: AccountContract)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(vararg accountContract: AccountContract)

    @Delete
    suspend fun delete(accountContract: AccountContract)
}
