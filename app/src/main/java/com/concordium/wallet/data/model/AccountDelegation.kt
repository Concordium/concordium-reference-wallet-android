package com.concordium.wallet.data.model

import java.io.Serializable
import java.math.BigInteger

data class AccountDelegation(
    val restakeEarnings: Boolean, //a boolean indicating whether earnings are
    val stakedAmount: BigInteger, //the amount that is currently staked
    val delegationTarget: DelegationTarget,
    val pendingChange: PendingChange //if present indicates that the delegator is in a cooldown due to removal or a change of stake. If present, the value is an object with the fields
) : Serializable
