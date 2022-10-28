package com.concordium.wallet.data.walletconnect

import com.google.gson.Gson
import java.io.Serializable

data class Params(
    val header: Header,
    val payload: String,
    val type: String
) : Serializable {
    fun parsePayload(): Payload? {
        return try {
            Gson().fromJson(payload.replace("\"", ""), Payload::class.java)
        } catch (ex: java.lang.Exception) {
            null
        }
    }
}
