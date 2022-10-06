package com.concordium.wallet.data.model

class CIS2TokensMetadata: ArrayList<CIS2TokensMetadataItem>()

data class CIS2TokensMetadataItem(
    val metadataChecksum: String,
    val metadataURL: String,
    val tokenId: String
)
