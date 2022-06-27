package com.concordium.wallet.data.export

import com.concordium.wallet.core.gson.RawJsonTypeAdapter
import com.concordium.wallet.data.model.AccountData
import com.concordium.wallet.data.model.CredentialWrapper
import com.concordium.wallet.data.model.RawJson
import com.google.gson.annotations.JsonAdapter
import java.io.Serializable

data class AccountExport(
    val name: String,
    val address: String,
    val submissionId: String,
    val accountKeys: AccountData,
    @JsonAdapter(RawJsonTypeAdapter::class)
    val commitmentsRandomness: RawJson?,
    val revealedAttributes: HashMap<String, String>,
    val credential: CredentialWrapper,
    val encryptionSecretKey: String
) : Serializable {
}