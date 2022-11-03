package com.concordium.wallet.data

import com.concordium.wallet.data.room.ContractToken
import com.concordium.wallet.data.room.ContractTokenDao

class ContractTokensRepository(private val contractTokenDao: ContractTokenDao) {
    suspend fun upsert(contractToken: ContractToken) {
        contractTokenDao.upsert(contractToken)
    }

    suspend fun update(contractToken: ContractToken) {
        contractTokenDao.update(contractToken)
    }

    suspend fun delete(contractToken: ContractToken) {
        contractTokenDao.delete(contractToken)
    }

    suspend fun getTokensByContractIndex(contractIndex: String): List<ContractToken> {
        return contractTokenDao.getTokensByContractIndex(contractIndex)
    }
}
