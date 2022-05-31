package com.concordium.wallet.data.model

import java.io.Serializable

data class PendingChange (
    val change: String, //indicating the kind of change which value is either "reduceStake" indicating that the delegators's stake is reduced at the end of the cooldown period, or "removeStake" indicating that the delegator is being removed at the end of the cooldown period.
    val newStake: String?, //This field is present if the value of the field "change" is "reduceStake", and the value is the new stake after the cooldown.
    val estimatedChangeTime: String? //(optional, present if protocol version 4 or later): an estimate of when the change actually takes effect, which is the first payday after the cooldown ends, e.g. "2022-03-30T16:43:53.5Z"
): Serializable {

    companion object {
        const val CHANGE_REDUCE_STAKE = "ReduceStake"
        const val CHANGE_REMOVE_STAKE = "RemoveStake"
        const val CHANGE_NO_CHANGE = "NoChange"
    }
}
