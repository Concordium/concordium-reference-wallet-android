package com.concordium.wallet.data.room

import androidx.lifecycle.LiveData
import androidx.room.*


@Dao
interface RecipientDao {

    @Query("SELECT * FROM recipient_table ORDER BY name ASC")
    fun getAllAsLiveData(): LiveData<List<Recipient>>

    @Query("SELECT * FROM recipient_table ORDER BY name ASC")
    suspend fun getAll(): List<Recipient>

    @Query("SELECT * FROM recipient_table WHERE address = :address")
    suspend fun getRecipientByAddress(address: String): Recipient?

    @Query("SELECT * FROM recipient_table WHERE address = :address AND name = :name")
    fun getRecipientByAddressAndName(name: String, address: String): Recipient?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg recipient: Recipient)

    @Transaction
    suspend fun insertUnique(recipient: Recipient) {
        val existingRecipient = getRecipientByAddressAndName(recipient.name, recipient.address) //prevent adding multiple identical entries
        if(existingRecipient == null){
            insert(recipient)
        }
    }

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(vararg recipient: Recipient)

    @Delete
    suspend fun delete(recipient: Recipient)

    @Query("DELETE FROM recipient_table")
    suspend fun deleteAll()

}