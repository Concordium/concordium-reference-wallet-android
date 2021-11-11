package com.concordium.wallet.data.cryptolib

import com.concordium.wallet.core.gson.RawJsonTypeAdapter
import com.concordium.wallet.data.model.AccountData
import com.concordium.wallet.data.model.CredentialWrapper
import com.concordium.wallet.data.model.RawJson
import com.google.gson.annotations.JsonAdapter

data class CreateCredentialOutput(
    val accountAddress: String,
    val accountKeys: AccountData,
    val credential: CredentialWrapper,
    val encryptionPublicKey: String,
    val encryptionSecretKey: String,
    @JsonAdapter(RawJsonTypeAdapter::class)
    val commitmentsRandomness: RawJson

)