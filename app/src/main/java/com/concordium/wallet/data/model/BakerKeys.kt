package com.concordium.wallet.data.model

import java.io.Serializable

data class BakerKeys(
    var bakerId: Int?,
    val aggregationSignKey: String,
    val aggregationVerifyKey: String,
    val electionPrivateKey: String,
    val electionVerifyKey: String,
    val signatureSignKey: String,
    val signatureVerifyKey: String
): Serializable