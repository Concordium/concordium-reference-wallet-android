package com.concordium.wallet.data

import com.concordium.wallet.data.room.AccountContract
import com.concordium.wallet.data.room.AccountContractDao

class AccountContractRepository(private val accountContractDao: AccountContractDao) {
    suspend fun upsert(accountContract: AccountContract) {
        accountContractDao.upsert(accountContract)
    }

    suspend fun update(accountContract: AccountContract) {
        accountContractDao.update(accountContract)
    }

    suspend fun delete(accountContract: AccountContract) {
        accountContractDao.delete(accountContract)
    }

    suspend fun getContractsByAccountAddress(accountAddress: String): List<AccountContract> {
        return accountContractDao.getContractsByAccountAddress(accountAddress)
    }
}
