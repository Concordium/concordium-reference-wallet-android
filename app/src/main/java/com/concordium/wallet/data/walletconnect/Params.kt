package com.concordium.wallet.data.walletconnect

import com.google.gson.Gson
import java.io.Serializable

data class Params(
    val type: String?,
    val sender: String?,
    var payload: String?,
    val message: String?,
    var payloadObj: Payload?
) : Serializable {
    fun parsePayload(): Payload? {
        return try {
            Gson().fromJson(payload, Payload::class.java)
        } catch (ex: java.lang.Exception) {
            null
        }
    }
}
