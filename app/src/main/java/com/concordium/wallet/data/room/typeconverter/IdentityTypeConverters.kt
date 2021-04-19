package com.concordium.wallet.data.room.typeconverter

import androidx.room.TypeConverter
import com.concordium.wallet.App
import com.concordium.wallet.data.model.IdentityObject
import com.concordium.wallet.data.model.IdentityProvider


class IdentityTypeConverters {

    @TypeConverter
    fun jsonToIdentityProvider(value: String): IdentityProvider {
        val gson = App.appCore.gson
        return gson.fromJson(value, IdentityProvider::class.java)
    }

    @TypeConverter
    fun identityProviderToJson(identityProvider: IdentityProvider): String {
        val gson = App.appCore.gson
        return gson.toJson(identityProvider)
    }

    @TypeConverter
    fun jsonToIdentityObject(value: String): IdentityObject {
        val gson = App.appCore.gson
        return gson.fromJson(value, IdentityObject::class.java)
    }

    @TypeConverter
    fun identityObjectToJson(identityObject: IdentityObject): String {
        val gson = App.appCore.gson
        return gson.toJson(identityObject)
    }

}