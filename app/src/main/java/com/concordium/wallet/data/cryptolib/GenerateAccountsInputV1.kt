package com.concordium.wallet.data.cryptolib

import com.concordium.wallet.data.model.GlobalParams
import com.concordium.wallet.data.model.IdentityObject

data class GenerateAccountsInputV1(
    val global: GlobalParams,
    val identityObject: IdentityObject
    //@JsonAdapter(RawJsonTypeAdapter::class)
    //val privateIdObjectData: RawJson
)