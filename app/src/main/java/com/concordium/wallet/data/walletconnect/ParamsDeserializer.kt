package com.concordium.wallet.data.walletconnect

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type

class ParamsDeserializer : JsonDeserializer<Params?> {
    @Throws(JsonParseException::class)
    override fun deserialize(
        jElement: JsonElement,
        typeOfT: Type?,
        context: JsonDeserializationContext
    ): Params {
        val jObject = jElement.asJsonObject

        return Params(
            type = jObject["type"]?.asString,
            sender = jObject["sender"]?.asString,
            payload = jObject["payload"]?.asString,
            message = jObject["message"]?.asString,
            payloadObj = null,
            schema = getSchema(context, jObject["schema"])
        )
    }

    /**
     * Get the [Schema] from the [JsonElement]
     * @param context [JsonDeserializationContext]
     * @param schemaElement [JsonElement]
     * @return [Schema]
     *
     * **null** if [schemaElement] is null
     *
     * **null** on [JsonParseException]
     *
     * **null** if **type** or **value** in [Schema] are null
     */
    private fun getSchema(
        context: JsonDeserializationContext,
        schemaElement: JsonElement?
    ): Schema? {
        if (schemaElement == null) return null

        if (schemaElement.isJsonObject) {
            return try {
                val schema: Schema = context.deserialize(schemaElement, Schema::class.java)
                if(schema.type == null || schema.value == null){
                    return null
                }
                return schema
            } catch (ex: JsonParseException) {
                null
            }
        }

        return try {
            Schema(type = "module", value = schemaElement.asString)
        } catch (ex: ClassCastException) {
            null
        } catch (ex: IllegalStateException) {
            null
        }
    }
}