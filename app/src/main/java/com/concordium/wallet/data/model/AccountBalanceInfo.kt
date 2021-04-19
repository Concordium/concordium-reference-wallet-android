package com.concordium.wallet.data.model


data class AccountBalanceInfo(
    val accountAmount: String,
    val accountEncryptedAmount: AccountEncryptedAmount,
    val accountNonce: Int,
    val accountReleaseSchedule: AccountReleaseSchedule,
    val accountBaker: AccountBaker
){
    fun getAmount(): Long {
        return accountAmount.toLong()
    }

    fun getEncryptedAmount(): AccountEncryptedAmount {
        return accountEncryptedAmount
    }

    fun hasEncryptedAmount(): Boolean {
        return !accountEncryptedAmount.selfAmount.isEmpty()
    }
}

