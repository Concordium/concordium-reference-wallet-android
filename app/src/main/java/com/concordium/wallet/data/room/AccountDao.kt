package com.concordium.wallet.data.room

import androidx.lifecycle.LiveData
import androidx.room.*
import com.concordium.wallet.data.model.TransactionStatus

@Dao
interface AccountDao {

    @Query("SELECT COUNT(id) FROM account_table")
    suspend fun getCount(): Int

    @Query("SELECT * FROM account_table ORDER BY read_only ASC, name ASC")
    fun getAllAsLiveData(): LiveData<List<Account>>

    @Transaction
    @Query("SELECT * FROM account_table ORDER BY read_only ASC, name ASC")
    fun getAllWithIdentityAsLiveData(): LiveData<List<AccountWithIdentity>>

    @Transaction
    @Query("SELECT * FROM account_table WHERE identity_id = :id ORDER BY read_only ASC, name ASC")
    fun getAllByIdentityIdWithIdentityAsLiveData(id: Int): LiveData<List<AccountWithIdentity>>

    @Transaction
    @Query("SELECT * FROM account_table WHERE id = :id")
    fun getByIdWithIdentityAsLiveData(id: Int): LiveData<AccountWithIdentity>

    @Query("SELECT * FROM account_table ORDER BY name ASC")
    suspend fun getAll(): List<Account>

    @Query("SELECT count(*) FROM account_table WHERE transaction_status != :status")
    suspend fun getStatusCount(status: Int): Int

    @Query("SELECT * FROM account_table WHERE identity_id = :id ORDER BY id ASC")
    suspend fun getAllByIdentityId(id: Int): List<Account>

    @Query("SELECT * FROM account_table WHERE id = :id")
    suspend fun findById(id: Int): Account?

    @Query("SELECT * FROM account_table WHERE address = :address")
    suspend fun findByAddress(address: String): Account?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg account: Account): List<Long>

    @Update
    suspend fun updateIdentity(vararg identity: Identity)

    @Query("SELECT * FROM identity_table WHERE id = :id")
    suspend fun findIdentityById(id: Int): Identity?

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(vararg account: Account)

    @Transaction
    suspend fun updateExceptFinalState(vararg accounts: Account) {
        for (account in accounts) {
            // finalized state is final and cannot be changed
            val accountFromDB = findById(account.id)
            if(accountFromDB != null && accountFromDB.transactionStatus == TransactionStatus.FINALIZED){
                account.transactionStatus = TransactionStatus.FINALIZED
            }
            update(account)
        }
    }

    @Delete
    suspend fun delete(account: Account)

    @Query("DELETE FROM account_table")
    suspend fun deleteAll()
}