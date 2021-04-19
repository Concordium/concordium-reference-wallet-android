package com.concordium.wallet.data.room

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface AccountWithTransfersDao {

    @Transaction
    @Query("SELECT * FROM account_table")
    fun getAccountsWithTransfers(): List<AccountWithTransfers>

}