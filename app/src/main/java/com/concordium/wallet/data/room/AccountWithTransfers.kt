package com.concordium.wallet.data.room

import androidx.room.Embedded
import androidx.room.Relation
import java.io.Serializable

data class AccountWithTransfers(
    @Embedded
    val account: Account,
    @Relation(
        parentColumn = "id",
        entityColumn = "account_id"
    )
    val transfers: List<Transfer>
) : Serializable
