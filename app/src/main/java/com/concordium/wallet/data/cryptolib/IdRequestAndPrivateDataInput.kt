package com.concordium.wallet.data.cryptolib

import com.concordium.wallet.data.model.ArsInfo
import com.concordium.wallet.data.model.GlobalParams
import com.concordium.wallet.data.model.IdentityProviderInfo

// equivalent to CreateIDRequest.swift
data class IdRequestAndPrivateDataInput(
    val ipInfo: IdentityProviderInfo,
    val global: GlobalParams?,
    val arsInfos: Map<String, ArsInfo>

)