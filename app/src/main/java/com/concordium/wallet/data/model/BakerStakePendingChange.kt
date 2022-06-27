package com.concordium.wallet.data.model

import java.io.Serializable

class BakerStakePendingChange(
    val pendingChangeType: String,
    val estimatedChangeTime: String?
) : Serializable {

    companion object {
        const val CHANGE_REMOVE_POOL = "RemovePool"
        const val CHANGE_REDUCE_BAKER_CAPITAL = "ReduceBakerCapital"
        const val CHANGE_NO_CHANGE = "NoChange"
    }
}
