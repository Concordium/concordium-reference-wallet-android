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

    fun find(accountAddress: String, contractIndex: String, tokenId: String): ContractToken? {
        return contractTokenDao.find(accountAddress, contractIndex, tokenId)
    }

    fun getTokens(accountAddress: String, contractIndex: String): List<ContractToken> {
        return contractTokenDao.getTokens(accountAddress, contractIndex)
    }

    fun getTokens(accountAddress: String, contractIndex: String, isFungible: Boolean): List<ContractToken> {
        return contractTokenDao.getTokens(accountAddress, contractIndex, isFungible)
    }
}
