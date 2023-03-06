package com.concordium.wallet.data.walletconnect

import com.concordium.wallet.core.gson.NullableTypeAdapterFactory
import com.concordium.wallet.util.Log
import com.google.gson.GsonBuilder
import java.io.Serializable

data class Params(
    val type: String?,
    val sender: String?,
    var payload: String?,
    var message: String?,
    var payloadObj: Payload?,
    var schema: Schema?
) : Serializable {
    fun parsePayload(): Payload? {
        return try {
            val gson = GsonBuilder()
                .registerTypeAdapterFactory(NullableTypeAdapterFactory())
                .create()
            gson.fromJson(payload, Payload::class.java)
        } catch (ex: java.lang.Exception) {
            Log.e(ex.toString())
            null
        }
    }
}
