package com.concordium.wallet.data.cryptolib

data class ParameterToJsonInput(
    val parameter: String,
    val receiveName: String,
    val schema: String,
    val schemaVersion: Int?
)