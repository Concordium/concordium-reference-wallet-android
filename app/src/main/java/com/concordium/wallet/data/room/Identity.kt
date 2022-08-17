package com.concordium.wallet.data.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.concordium.wallet.data.model.IdentityObject
import com.concordium.wallet.data.model.IdentityProvider
import com.concordium.wallet.data.room.typeconverter.IdentityTypeConverters
import java.io.Serializable

@Entity(tableName = "identity_table")
@TypeConverters(IdentityTypeConverters::class)
data class Identity(
    @PrimaryKey(autoGenerate = true)
    var id: Int,
    var name: String,
    var status: String,
    var detail: String?,
    @ColumnInfo(name = "code_uri")
    val codeUri: String,
    @ColumnInfo(name = "next_account_number")
    var nextAccountNumber: Int,
    @ColumnInfo(name = "identity_provider")
    var identityProvider: IdentityProvider,
    @ColumnInfo(name = "identity_object")
    var identityObject: IdentityObject?,
    @ColumnInfo(name = "private_id_object_data_encrypted")
    var privateIdObjectDataEncrypted: String
) : Serializable