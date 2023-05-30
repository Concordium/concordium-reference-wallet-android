package com.concordium.wallet.data.room.typeconverter

import androidx.room.TypeConverter
import com.concordium.wallet.App
import com.concordium.wallet.data.model.TokenMetadata

class ContractTypeConverters {
    @TypeConverter
    fun jsonToTokenMetadata(value: String): TokenMetadata? {
        val gson = App.appCore.gson
        return gson.fromJson(value, TokenMetadata::class.java)
    }

    @TypeConverter
    fun tokenMetadataToJson(tokenMetadata: TokenMetadata?): String {
        val gson = App.appCore.gson
        return gson.toJson(tokenMetadata)
    }
}