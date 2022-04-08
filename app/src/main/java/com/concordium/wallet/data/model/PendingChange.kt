package com.concordium.wallet.data.model

import java.io.Serializable

data class PendingChange (
    val change: String, //indicating the kind of change which value is either "reduceStake" indicating that the delegators's stake is reduced at the end of the cooldown period, or "removeStake" indicating that the delegator is being removed at the end of the cooldown period.
    val effectiveTime: String, //the time at which the cooldown ends and the change takes effect, e.g. "2022-03-30T16:43:53.5Z".
    val newStake: String? //This field is present if the value of the field "change" is "reduceStake", and the value is the new stake after the cooldown.
): Serializable
