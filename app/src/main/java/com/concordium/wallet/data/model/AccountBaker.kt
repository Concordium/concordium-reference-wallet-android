package com.concordium.wallet.data.model

import java.io.Serializable

data class AccountBaker (
    val bakerId: Integer?,
    val stakedAmount: String
): Serializable
