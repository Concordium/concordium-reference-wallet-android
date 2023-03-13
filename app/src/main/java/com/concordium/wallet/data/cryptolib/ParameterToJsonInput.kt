package com.concordium.wallet.data.cryptolib

import com.concordium.wallet.data.walletconnect.Schema

data class ParameterToJsonInput(
    val parameter: String,
    val receiveName: String,
    val schema: Schema,
    val schemaVersion: Int?
)