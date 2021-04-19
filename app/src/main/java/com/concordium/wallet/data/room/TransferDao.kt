package com.concordium.wallet.data.room

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface TransferDao {

    @Query("SELECT * FROM transfer_table ORDER BY created_at DESC")
    fun getAllAsLiveData(): LiveData<List<Transfer>>

    @Query("SELECT * FROM transfer_table ORDER BY created_at DESC")
    suspend fun getAll(): List<Transfer>

    @Query("SELECT * FROM transfer_table WHERE account_id = :id ORDER BY created_at DESC")
    suspend fun getAllByAccountId(id: Int): List<Transfer>

    @Query("SELECT * FROM transfer_table WHERE id = :id")
    suspend fun findById(id: Int): Transfer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg transfer: Transfer)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(vararg transfer: Transfer)

    @Delete
    suspend fun delete(vararg transfer: Transfer)

    @Query("DELETE FROM transfer_table")
    suspend fun deleteAll()
}