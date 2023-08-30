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
        val gson = GsonBuilder()
            .registerTypeAdapterFactory(NullableTypeAdapterFactory())
            .create()
        return try {
            gson.fromJson(payload, Payload.UpdateTransaction::class.java)
        } catch (ex: Exception) {
            try {
                gson.fromJson(payload, Payload.SimpleTransaction::class.java)
            } catch (ex: Exception) {
                Log.e(ex.toString())
                null
            }
        }
    }
}
