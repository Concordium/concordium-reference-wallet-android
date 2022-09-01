package com.concordium.wallet.data.model

import com.concordium.wallet.core.gson.RawJsonTypeAdapter
import com.google.gson.Gson
import com.google.gson.annotations.JsonAdapter
import java.io.Serializable

data class CredentialWrapper(
    @JsonAdapter(RawJsonTypeAdapter::class)
    val value: RawJson,
    val v: Int
) : Serializable {
    fun getCredId(): String? {
        return try {
            val credentialContent = Gson().fromJson(value.json, CredentialContent::class.java)
            credentialContent.credential.contents.credId
        } catch (ex: Exception) {
            null
        }
    }

    fun getThreshold(): Int? {
        return try {
            val credentialContent = Gson().fromJson(value.json, CredentialContent::class.java)
            credentialContent.credential.contents.credentialPublicKeys.threshold
        } catch (ex: Exception) {
            null
        }
    }
}

data class CredentialContent(
    val credential: Credential
)

data class Credential(
    val contents: Contents
)

data class Contents(
    val credId: String,
    val credentialPublicKeys: CredentialPublicKeys
)

data class CredentialPublicKeys(
    val threshold: Int
)
