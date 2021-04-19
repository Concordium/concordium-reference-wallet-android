package com.concordium.wallet.data.export

import com.concordium.wallet.core.gson.RawJsonTypeAdapter
import com.concordium.wallet.data.model.IdentityObject
import com.concordium.wallet.data.model.IdentityProvider
import com.concordium.wallet.data.model.RawJson
import com.google.gson.annotations.JsonAdapter
import java.io.Serializable

data class IdentityExport(
    val name: String,
    val nextAccountNumber: Int,
    val identityProvider: IdentityProvider,
    val identityObject: IdentityObject,
    @JsonAdapter(RawJsonTypeAdapter::class)
    val privateIdObjectData: RawJson,
    val accounts: List<AccountExport>
) : Serializable