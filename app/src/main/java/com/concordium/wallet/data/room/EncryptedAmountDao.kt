package com.concordium.wallet.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface EncryptedAmountDao {

    @Query("SELECT * FROM encrypted_amount_table WHERE encryptedkey = :key")
    suspend fun findByKey(key: String): EncryptedAmount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg amount: EncryptedAmount)

    @Query("DELETE FROM encrypted_amount_table")
    suspend fun deleteAll()

    @Query("SELECT * FROM encrypted_amount_table WHERE amount is null")
    fun findAllUndecrypted(): List<EncryptedAmount>
}