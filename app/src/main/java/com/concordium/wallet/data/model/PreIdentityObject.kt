package com.concordium.wallet.data.model

import com.concordium.wallet.core.gson.RawJsonTypeAdapter
import com.google.gson.annotations.JsonAdapter
import java.io.Serializable

data class PreIdentityObject(
    @JsonAdapter(RawJsonTypeAdapter::class)
    val choiceArData: RawJson,
    val pubInfoForIp: PubInfoForIp,
    val idCredSecCommitment: String,
    @JsonAdapter(RawJsonTypeAdapter::class)
    val ipArData: RawJson,
    val prfKeyCommitmentWithIP: String,
    @JsonAdapter(RawJsonTypeAdapter::class)
    val prfKeySharingCoeffCommitments: RawJson,
    val proofsOfKnowledge: String,
    val idCredPub: String
) : Serializable