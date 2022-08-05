package com.concordium.wallet.data

import androidx.lifecycle.LiveData
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.data.room.IdentityDao

class IdentityRepository(private val identityDao: IdentityDao) {
    val allIdentities: LiveData<List<Identity>> = identityDao.getAllAsLiveData()
    val allDoneIdentities: LiveData<List<Identity>> = identityDao.getAllDoneAsLiveData()

    suspend fun getCount(): Int {
        return identityDao.getCount()
    }

    suspend fun getAll(): List<Identity> {
        return identityDao.getAll()
    }

    suspend fun getAllDone(): List<Identity> {
        return identityDao.getAllDone()
    }

    suspend fun getAllPending(): List<Identity> {
        return identityDao.getAllPending()
    }

    suspend fun getNonDoneCount(): Int {
        return identityDao.getNonDoneCount()
    }

    suspend fun getNonFailedCount(): Int {
        return identityDao.getNonFailedCount()
    }

    suspend fun findById(id: Int): Identity? {
        return identityDao.findById(id)
    }

    suspend fun insert(identity: Identity): Long {
        return identityDao.insert(identity).first()
    }

    suspend fun update(identity: Identity) {
        identityDao.update(identity)
    }

    suspend fun delete(identity: Identity) {
        identityDao.delete(identity)
    }

    suspend fun deleteAll() {
        identityDao.deleteAll()
    }

    suspend fun nextAccountNumber(): Int {
        val allIdentities = getAll()
        return allIdentities.maxOfOrNull { it.nextAccountNumber } ?: 0
    }
}
