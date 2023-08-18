package com.concordium.wallet.data.model

data class CIS2TokensMetadata(
    val contractName: String,
    val metadata: List<CIS2TokensMetadataItem>
)

data class CIS2TokensMetadataItem(
    val metadataChecksum: String,
    val metadataURL: String,
    val tokenId: String
)
