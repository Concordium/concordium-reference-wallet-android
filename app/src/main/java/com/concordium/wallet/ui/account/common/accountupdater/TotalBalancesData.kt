package com.concordium.wallet.ui.account.common.accountupdater

import java.math.BigDecimal

data class TotalBalancesData(
    val totalBalanceForAllAccounts: BigDecimal,
    val totalAtDisposalForAllAccounts: BigDecimal,
    val totalStakedForAllAccounts: BigDecimal,
    val totalContainsEncrypted: Boolean
)