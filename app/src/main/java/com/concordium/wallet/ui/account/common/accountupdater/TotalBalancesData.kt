package com.concordium.wallet.ui.account.common.accountupdater

data class TotalBalancesData(
    val totalBalanceForAllAccounts: Long,
    val totalAtDisposalForAllAccounts: Long,
    val totalStakedForAllAccounts: Long,
    val totalContainsEncrypted: Boolean
)