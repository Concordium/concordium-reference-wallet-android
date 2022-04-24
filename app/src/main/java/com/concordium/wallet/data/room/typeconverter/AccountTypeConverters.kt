package com.concordium.wallet.data.room.typeconverter

import androidx.room.TypeConverter
import com.concordium.wallet.App
import com.concordium.wallet.data.model.*
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

class AccountTypeConverters {

    @TypeConverter
    fun jsonToList(value: String): List<IdentityAttribute> {
        val gson = App.appCore.gson
        val listType: Type = object : TypeToken<ArrayList<IdentityAttribute>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun listToJson(list: List<IdentityAttribute>): String {
        val gson = App.appCore.gson
        return gson.toJson(list)
    }

    @TypeConverter
    fun jsonToCredential(value: String): Credential {
        val gson = App.appCore.gson
        return gson.fromJson(value, Credential::class.java)
    }

    @TypeConverter
    fun credentialToJson(credential: Credential): String {
        val gson = App.appCore.gson
        return gson.toJson(credential)
    }



    @TypeConverter
    fun jsonToAccountDelegation(value: String?): AccountDelegation? {
        if(value == null){
            return null
        }
        val gson = App.appCore.gson
        return gson.fromJson(value, AccountDelegation::class.java)
    }

    @TypeConverter
    fun accountDelegationToJson(delegation: AccountDelegation?): String? {
        if(delegation == null){
            return null
        }
        val gson = App.appCore.gson
        return gson.toJson(delegation)
    }


    @TypeConverter
    fun jsonToAccountDelegation(value: String?): AccountDelegation? {
        if(value == null){
            return null
        }
        val gson = App.appCore.gson
        return gson.fromJson(value, AccountDelegation::class.java)
    }

    @TypeConverter
    fun accountDelegationToJson(delegation: AccountDelegation?): String? {
        if(delegation == null){
            return null
        }
        val gson = App.appCore.gson
        return gson.toJson(delegation)
    }

    @TypeConverter
    fun jsonToAccountBaker(value: String?): AccountBaker? {
        if(value == null){
            return null
        }
        val gson = App.appCore.gson
        return gson.fromJson(value, AccountBaker::class.java)
    }

    @TypeConverter
    fun accountBakerToJson(baker: AccountBaker?): String? {
        if(baker == null){
            return null
        }
        val gson = App.appCore.gson
        return gson.toJson(baker)
    }

    @TypeConverter
    fun jsonToCredentialWrapper(value: String?): CredentialWrapper? {
        if(value == null){
            return null
        }
        val gson = App.appCore.gson
        return gson.fromJson(value, CredentialWrapper::class.java)
    }

    @TypeConverter
    fun credentialWrapperToJson(credential: CredentialWrapper?): String? {
        if(credential == null){
            return null
        }
        val gson = App.appCore.gson
        return gson.toJson(credential)
    }

    @TypeConverter
    fun jsonToAccountEncryptedAmount(value: String): AccountEncryptedAmount? {
        val gson = App.appCore.gson
        return gson.fromJson(value, AccountEncryptedAmount::class.java)
    }

    @TypeConverter
    fun accountEncryptedAmountToJson(amount: AccountEncryptedAmount?): String {
        val gson = App.appCore.gson
        return gson.toJson(amount)
    }

    @TypeConverter
    fun jsonToAccountReleaseSchedule(value: String): AccountReleaseSchedule? {
        val gson = App.appCore.gson
        return gson.fromJson(value, AccountReleaseSchedule::class.java)
    }

    @TypeConverter
    fun accountReleaseScheduleToJson(schedule: AccountReleaseSchedule?): String {
        val gson = App.appCore.gson
        return gson.toJson(schedule)
    }
}