package com.concordium.wallet.data

import com.concordium.wallet.data.room.ContractToken
import com.concordium.wallet.data.room.ContractTokenDao

class ContractTokensRepository(private val contractTokenDao: ContractTokenDao) {
    suspend fun insert(contractToken: ContractToken) {
        contractTokenDao.insert(contractToken)
    }

    suspend fun delete(contractToken: ContractToken) {
        contractTokenDao.delete(contractToken)
    }

    fun find(contractIndex: String, tokenId: String): ContractToken? {
        return contractTokenDao.find(contractIndex, tokenId)
    }

    fun getTokens(contractIndex: String): List<ContractToken> {
        return contractTokenDao.getTokens(contractIndex)
    }

    fun getTokens(contractIndex: String, isFungible: Boolean): List<ContractToken> {
        return contractTokenDao.getTokens(contractIndex, isFungible)
    }
}
