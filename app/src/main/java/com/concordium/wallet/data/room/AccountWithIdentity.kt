package com.concordium.wallet.data.room

import androidx.room.*
import com.concordium.wallet.data.model.*
import com.concordium.wallet.data.room.typeconverter.AccountTypeConverters
import java.io.Serializable


data class AccountWithIdentity(
    @Embedded
    val account: Account,
    @Relation(
        parentColumn = "identity_id",
        entityColumn = "id"
    )
    val identity: Identity
) : Serializable