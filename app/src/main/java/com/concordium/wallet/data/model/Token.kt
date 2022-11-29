package com.concordium.wallet.data.model

import java.io.Serializable

data class Token (
    val id: String,
    var token: String,
    val totalSupply: String,
    var tokenMetadata: TokenMetadata?,
    var isSelected: Boolean = false,
    var contractIndex: String,
    var isCCDToken: Boolean = false,
    var totalBalance: Long = 0,
    var atDisposal: Long = 0,
    var contractName: String = "",
    var symbol: String = ""
): Serializable