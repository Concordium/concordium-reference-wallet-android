package com.concordium.wallet.ui.account.common.accountupdater

import java.math.BigInteger

data class TotalBalancesData(
    val totalBalanceForAllAccounts: BigInteger,
    val totalAtDisposalForAllAccounts: BigInteger,
    val totalStakedForAllAccounts: BigInteger,
    val totalContainsEncrypted: Boolean
)