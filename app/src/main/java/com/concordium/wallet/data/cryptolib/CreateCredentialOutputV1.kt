package com.concordium.wallet.data.cryptolib

import com.concordium.wallet.core.gson.RawJsonTypeAdapter
import com.concordium.wallet.data.model.AccountData
import com.concordium.wallet.data.model.CredentialWrapper
import com.concordium.wallet.data.model.RawJson
import com.google.gson.annotations.JsonAdapter

data class CreateCredentialOutputV1(
    val credential: CredentialWrapper,
    val accountKeys: AccountData,
    val accountAddress: String,
    val encryptionPublicKey: String,
    val encryptionSecretKey: String,
    @JsonAdapter(RawJsonTypeAdapter::class)
    val commitmentsRandomness: RawJson
)