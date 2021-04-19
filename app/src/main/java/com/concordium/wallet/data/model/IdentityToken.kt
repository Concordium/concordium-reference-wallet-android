package com.concordium.wallet.data.model

data class IdentityToken(
    val accountAddress: String,
    val credential: CredentialWrapper,
    val identityObject: IdentityContainer
)