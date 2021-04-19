package com.concordium.wallet.data.cryptolib

import com.concordium.wallet.data.model.AccountData
import com.concordium.wallet.data.model.CredentialWrapper

data class CreateCredentialOutput(
    val accountAddress: String,
    val accountKeys: AccountData,
    val credential: CredentialWrapper,
    val encryptionPublicKey: String,
    val encryptionSecretKey: String
)