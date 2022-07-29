package com.concordium.wallet.data.model

import java.io.Serializable

data class IdentityCreationData(
    val identityProvider: IdentityProvider,
    val idObjectRequest: RawJson,
    val privateIdObjectDataEncrypted: String,
    val identityName: String,
    val encryptedAccountData: String,
    val accountAddress: String
) : Serializable