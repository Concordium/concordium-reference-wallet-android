package com.concordium.wallet.data.model

import java.io.Serializable
import java.math.BigInteger

data class AccountBalanceInfo(
    val accountAmount: String,
    val accountEncryptedAmount: AccountEncryptedAmount,
    val accountNonce: Int,
    val accountReleaseSchedule: AccountReleaseSchedule,
    val accountBaker: AccountBaker?,
    val accountDelegation: AccountDelegation?,
    val accountIndex: Int
) : Serializable {

    fun getAmount(): BigInteger {
        return accountAmount.toBigInteger()
    }

    fun getEncryptedAmount(): AccountEncryptedAmount {
        return accountEncryptedAmount
    }
}
