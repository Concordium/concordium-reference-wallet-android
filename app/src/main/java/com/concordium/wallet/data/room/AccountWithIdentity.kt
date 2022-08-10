package com.concordium.wallet.data.room

import androidx.room.Embedded
import androidx.room.Relation
import java.io.Serializable


data class AccountWithIdentity(
    @Embedded
    var account: Account,
    @Relation(
        parentColumn = "identity_id",
        entityColumn = "id"
    )
    val identity: Identity
) : Serializable