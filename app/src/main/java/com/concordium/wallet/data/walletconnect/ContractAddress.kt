package com.concordium.wallet.data.walletconnect

import com.google.gson.annotations.SerializedName

data class ContractAddress(
    val index: Int,
    @SerializedName("subindex")
    val subIndex: Int
)
