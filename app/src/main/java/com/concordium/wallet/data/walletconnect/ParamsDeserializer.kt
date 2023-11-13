package com.concordium.wallet.data.walletconnect

import android.util.Base64
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.IllegalArgumentException
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
        if (schemaElement == null || schemaElement.isJsonNull) return null

        if (schemaElement.isJsonObject) {
            try {
                val schema: Schema.ValueSchema =
                    context.deserialize(schemaElement, Schema.ValueSchema::class.java)
                if (schema.type == null || schema.value == null) {
                    return null
                }
                return schema
            } catch (ex: JsonParseException) {
                try {
                    val schema: Schema.BrokenSchema =
                        context.deserialize(schemaElement, Schema.BrokenSchema::class.java)
                    val schemaType = when (schema.type) {
                        "ModuleSchema" -> "module"
                        "TypeSchema" -> "parameter"
                        else -> throw IllegalArgumentException("invalid schema type '${schema.type}")
                    }
                    val schemaValueBytes = schema.value.data.map { it.toByte() }.toByteArray()
                    val schemaValue = Base64.encodeToString(schemaValueBytes, Base64.NO_WRAP)
                    return Schema.ValueSchema(type = schemaType, value = schemaValue)
                } catch (ex: JsonParseException) {
                    return null
                }
            }
        }

        return try {
            Schema.ValueSchema(type = "module", value = schemaElement.asString)
        } catch (ex: ClassCastException) {
            null
        } catch (ex: IllegalStateException) {
            null
        }
    }
}