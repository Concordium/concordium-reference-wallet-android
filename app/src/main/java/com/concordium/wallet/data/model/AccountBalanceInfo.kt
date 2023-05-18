package com.concordium.wallet.data.model

import com.concordium.wallet.util.toBigDecimal
import java.io.Serializable
import java.math.BigDecimal

data class AccountBalanceInfo(
    val accountAmount: String,
    val accountEncryptedAmount: AccountEncryptedAmount,
    val accountNonce: Int,
    val accountReleaseSchedule: AccountReleaseSchedule,
    val accountBaker: AccountBaker?,
    val accountDelegation: AccountDelegation?,
    val accountIndex: Int
) : Serializable {

    fun getAmount(): BigDecimal {
        return accountAmount.toBigDecimal()
    }

    fun getEncryptedAmount(): AccountEncryptedAmount {
        return accountEncryptedAmount
    }
}
