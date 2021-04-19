package com.concordium.wallet.data.model

data class AccountBalance(
    val currentBalance: AccountBalanceInfo?,
    val finalizedBalance: AccountBalanceInfo?
) {
    fun accountExists(): Boolean {
        val doesNotExist = (currentBalance == null && finalizedBalance == null)
        return !doesNotExist
    }
}



