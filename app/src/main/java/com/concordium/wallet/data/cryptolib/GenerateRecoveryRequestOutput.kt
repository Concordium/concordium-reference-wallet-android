package com.concordium.wallet.data.cryptolib

data class GenerateRecoveryRequestOutput (
    var idRecoveryRequest: IdRecoveryRequest
)

data class IdRecoveryRequest (
    var v: Int,
    var value: Value
)

data class Value (
    var idCredPub: String,
    var proof: String,
    var timestamp: Int
)
