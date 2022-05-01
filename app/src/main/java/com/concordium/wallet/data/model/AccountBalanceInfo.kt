package com.concordium.wallet.data.model

import java.io.Serializable

data class AccountBalanceInfo(
    val accountAmount: String,
    val accountEncryptedAmount: AccountEncryptedAmount,
    val accountNonce: Int,
    val accountReleaseSchedule: AccountReleaseSchedule,
    val accountBaker: AccountBaker?,
    val accountDelegation: AccountDelegation?
) : Serializable {

    fun getAmount(): Long {
        return accountAmount.toLong()
    }

    fun getEncryptedAmount(): AccountEncryptedAmount {
        return accountEncryptedAmount
    }
}
