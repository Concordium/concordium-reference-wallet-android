package com.concordium.wallet.data.export

import com.concordium.wallet.data.model.AccountData
import com.concordium.wallet.data.model.CredentialWrapper
import com.concordium.wallet.data.model.IdentityAttribute
import java.io.Serializable

data class AccountExport(
    val name: String,
    val address: String,
    val submissionId: String,
    val accountKeys: AccountData,
    val revealedAttributes: HashMap<String, String>,
    val credential: CredentialWrapper,
    val encryptionSecretKey: String
) : Serializable {
}