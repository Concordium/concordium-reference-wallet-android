package com.concordium.wallet.data.model

import java.io.Serializable

data class DelegationTarget (
    var delegateType: String = "", //the type of delegation which value is either "L-Pool" or "Baker"
    var bakerId: Long? = null //If the value "delegateType" is "Baker", then "bakerId" is the ID of the target pool, otherwise not present.
): Serializable {
    companion object {
        val TYPE_DELEGATE_TO_L_POOL = "L-Pool"
        val TYPE_DELEGATE_TO_BAKER = "Baker"
    }
}
