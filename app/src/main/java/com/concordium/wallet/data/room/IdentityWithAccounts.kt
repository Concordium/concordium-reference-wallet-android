package com.concordium.wallet.data.room

import androidx.room.Embedded
import androidx.room.Relation
import java.io.Serializable

data class IdentityWithAccounts(
    @Embedded
    val identity: Identity,
    @Relation(
        parentColumn = "id",
        entityColumn = "identity_id"
    )
    val accounts: MutableList<Account> = mutableListOf()
) : Serializable
