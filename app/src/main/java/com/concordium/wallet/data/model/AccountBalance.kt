package com.concordium.wallet.data.model

import java.io.Serializable

data class AccountBalance(
    val currentBalance: AccountBalanceInfo?,
    val finalizedBalance: AccountBalanceInfo?
) : Serializable
