package com.concordium.wallet.data.model

import java.io.Serializable
import java.math.BigInteger

data class AccountReleaseSchedule(
    val schedule: List<Schedule>,
    val total: BigInteger
) : Serializable
