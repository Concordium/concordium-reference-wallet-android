package com.concordium.wallet.data.model

import java.io.Serializable
import java.math.BigDecimal

// The class must have default value for each field,
// otherwise Gson fails to use defaults and sets not-nullable fields to null.
data class Token (
    val id: String = "",
    var token: String = "",
    val totalSupply: String = "",
    var tokenMetadata: TokenMetadata? = null,
    var isSelected: Boolean = false,
    var contractIndex: String = "",
    var subIndex: String = "",
    var isCCDToken: Boolean = false,
    var totalBalance: BigDecimal = BigDecimal.ZERO,
    var atDisposal: BigDecimal = BigDecimal.ZERO,
    var contractName: String = "",
    var symbol: String = ""
): Serializable