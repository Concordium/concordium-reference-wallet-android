package com.concordium.wallet.data

import androidx.lifecycle.LiveData
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.AccountDao
import com.concordium.wallet.data.room.AccountWithIdentity

class AccountRepository(private val accountDao: AccountDao) {
    val allAccountsWithIdentity: LiveData<List<AccountWithIdentity>> = accountDao.getAllWithIdentityAsLiveData()

    suspend fun getCount(): Int {
        return accountDao.getCount()
    }

    suspend fun getAll(): List<Account> {
        return accountDao.getAll()
    }

    suspend fun getAllDone(): List<Account> {
        return accountDao.getAllDone()
    }

    suspend fun getAllDoneWithIdentity(): List<AccountWithIdentity> {
        return accountDao.getAllDoneWithIdentity()
    }

    suspend fun getAllByIdentityId(id: Int): List<Account> {
        return accountDao.getAllByIdentityId(id)
    }

    suspend fun getNonDoneCount(): Int {
        return accountDao.getStatusCount(TransactionStatus.FINALIZED.ordinal)
    }

    suspend fun findById(id: Int): Account? {
        return accountDao.findById(id)
    }

    suspend fun findByAddress(address: String): Account? {
        return accountDao.findByAddress(address)
    }

    suspend fun insert(account: Account): Long {
        return accountDao.insert(account).first()
    }

    suspend fun insertAll(accountList: List<Account>) {
        accountDao.insert(*accountList.toTypedArray())
    }

    suspend fun update(account: Account) {
        accountDao.update(account)
    }

    suspend fun updateAll(accountList: List<Account>) {
        accountDao.updateExceptFinalState(*accountList.toTypedArray())
    }

    suspend fun delete(account: Account) {
        accountDao.delete(account)
    }

    suspend fun deleteAll() {
        accountDao.deleteAll()
    }

    suspend fun nextCredNumber(identityId: Int): Int {
        val accounts = accountDao.getAllByIdentityId(identityId)
        var nextCredNumber = 0
        while (accounts.filter { it.credNumber == nextCredNumber }.size == 1)
            nextCredNumber++
        return nextCredNumber
    }
}
