package com.concordium.wallet.data

import com.concordium.wallet.data.room.EncryptedAmount
import com.concordium.wallet.data.room.EncryptedAmountDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EncryptedAmountRepository(private val encryptedAmountDao: EncryptedAmountDao) {

    suspend fun findByAddress(key: String): EncryptedAmount? {
        return withContext(Dispatchers.IO){
            encryptedAmountDao.findByKey(key)
        }
    }

    suspend fun insert(amount: EncryptedAmount) {
        return withContext(Dispatchers.IO){
            encryptedAmountDao.insert(amount)
        }
    }

    suspend fun deleteAll() {
        return withContext(Dispatchers.IO){
            encryptedAmountDao.deleteAll()
        }
    }

    suspend fun findAllUndecrypted(): List<EncryptedAmount> {
        return withContext(Dispatchers.IO){
            encryptedAmountDao.findAllUndecrypted()
        }
    }
}