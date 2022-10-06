package com.concordium.wallet.data

import com.concordium.wallet.data.room.AccountContract
import com.concordium.wallet.data.room.AccountContractDao

class AccountContractRepository(private val accountContractDao: AccountContractDao) {
    suspend fun find(accountAddress: String): List<AccountContract> {
        return accountContractDao.find(accountAddress)
    }

    suspend fun find(accountAddress: String, contractIndex: String): AccountContract? {
        return accountContractDao.find(accountAddress, contractIndex)
    }

    suspend fun insert(accountContract: AccountContract) {
        accountContractDao.insert(accountContract)
    }

    suspend fun update(accountContract: AccountContract) {
        accountContractDao.update(accountContract)
    }

    suspend fun delete(accountContract: AccountContract) {
        accountContractDao.delete(accountContract)
    }
}
