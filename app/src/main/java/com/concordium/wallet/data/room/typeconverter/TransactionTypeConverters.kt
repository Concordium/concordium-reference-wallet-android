package com.concordium.wallet.data.room.typeconverter

import androidx.room.TypeConverter
import com.concordium.wallet.App
import com.concordium.wallet.data.model.*


class TransactionTypeConverters {

    @TypeConverter
    fun jsonToTransactionType(value: String): TransactionType? {
        val gson = App.appCore.gson
        return gson.fromJson(value, TransactionType::class.java)
    }

    @TypeConverter
    fun transactionTypeToJson(amount: TransactionType?): String {
        val gson = App.appCore.gson
        return gson.toJson(amount)
    }


    @TypeConverter
    fun jsonToAccountNonce(value: String): AccountNonce? {
        val gson = App.appCore.gson
        return gson.fromJson(value, AccountNonce::class.java)
    }

    @TypeConverter
    fun accountNonceToJson(amount: AccountNonce?): String {
        val gson = App.appCore.gson
        return gson.toJson(amount)
    }

}