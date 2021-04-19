package com.concordium.wallet.data.model

import java.io.Serializable

data class AccountReleaseSchedule(
    val schedule: List<Schedule>,
    val total: String
) : Serializable
