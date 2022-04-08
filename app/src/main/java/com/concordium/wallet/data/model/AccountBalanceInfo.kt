package com.concordium.wallet.data.model


data class AccountBalanceInfo(

    val accountAmount: String,
    val accountEncryptedAmount: AccountEncryptedAmount,
    val accountNonce: Int,
    val accountReleaseSchedule: AccountReleaseSchedule,
    val accountBaker: AccountBaker,
    val accountDelegation: AccountDelegation? //if present indicates that this account is registered as a delegator. If present, the value is always an object with fields
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

