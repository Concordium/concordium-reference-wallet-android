package com.concordium.wallet.data

import androidx.lifecycle.LiveData
import com.concordium.wallet.data.room.Transfer
import com.concordium.wallet.data.room.TransferDao

class TransferRepository(private val transferDao: TransferDao) {

    val allTransfers: LiveData<List<Transfer>> = transferDao.getAllAsLiveData()

    suspend fun getAll(): List<Transfer> {
        return transferDao.getAll()
    }

    suspend fun getAllByAccountId(id: Int): List<Transfer> {
        return transferDao.getAllByAccountId(id)
    }

    suspend fun findById(id: Int): Transfer? {
        return transferDao.findById(id)
    }

    suspend fun insert(transfer: Transfer) {
        transferDao.insert(transfer)
    }

    suspend fun insertAll(transferList: List<Transfer>) {
        transferDao.insert(*transferList.toTypedArray())
    }

    suspend fun update(transfer: Transfer) {
        transferDao.update(transfer)
    }

    suspend fun updateAll(transferList: List<Transfer>) {
        transferDao.update(*transferList.toTypedArray())
    }

    suspend fun delete(transfer: Transfer) {
        transferDao.delete(transfer)
    }

    suspend fun deleteAll(transferList: List<Transfer>) {
        transferDao.delete(*transferList.toTypedArray())
    }

    suspend fun deleteAll() {
        transferDao.deleteAll()
    }
}