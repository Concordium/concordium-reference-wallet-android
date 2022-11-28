package com.concordium.wallet.data.model

class CIS2TokensBalances : ArrayList<CIS2TokenBalanceItem>()

data class CIS2TokenBalanceItem(
    val balance: String,
    val tokenId: String
)
