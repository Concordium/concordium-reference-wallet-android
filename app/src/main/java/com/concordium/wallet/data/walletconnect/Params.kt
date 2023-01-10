package com.concordium.wallet.data.walletconnect

import com.concordium.wallet.data.model.ProofOfIdentityStatement
import com.google.gson.Gson
import java.io.Serializable

data class Params(
    val type: String?,
    val sender: String?,
    var payload: String?,
    var message: String?,
    var payloadObj: Payload?,
    var accountAddress: String?,
    var statement: List<ProofOfIdentityStatement>?,
    var challenge: String?,
    var schema: String?
) : Serializable {
    fun parsePayload(): Payload? {
        return try {
            Gson().fromJson(payload, Payload::class.java)
        } catch (ex: java.lang.Exception) {
            null
        }
    }
}
