package com.concordium.wallet.data.walletconnect

import com.google.gson.Gson
import java.io.Serializable

data class Params(
    val type: String?,
    val sender: String?,
    var payload: String?,
    var message: String?,
    var payloadObj: Payload?,
    var schema: String?,
    var schemaObj: Schema?
) : Serializable {
    fun parsePayload(): Payload? {
        return try {
            Gson().fromJson(payload, Payload::class.java)
        } catch (ex: java.lang.Exception) {
            null
        }
    }

    fun parseSchema(): Schema? {
        if(schema == null) return null
        return try {
            if (schema!!.startsWith("{")) {
                Gson().fromJson(schema, Schema::class.java)
            }
            Schema(type = "module", value = schema!!)
        } catch (ex: java.lang.Exception) {
            null
        }
    }
}
