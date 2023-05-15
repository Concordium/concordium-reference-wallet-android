package com.concordium.wallet.core.gson

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import kotlin.jvm.internal.Reflection
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

/**
 * GSON doesn't understand Kotlin’s non-null types.
 *
 * If we try to deserialize a null value into a non-null type, GSON will do so without errors. This can easily cause unexpected behaviour.
 *
 * This adapter adds the ability to catch if a null value is assigned to a non-nullable field.
 * @return [TypeAdapterFactory]
 *
 * [JsonParseException] if null is assigned to a non-nullable field
 */
class NullableTypeAdapterFactory : TypeAdapterFactory {

    override fun <T : Any> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        val delegate = gson.getDelegateAdapter(this, type)

        // If the class isn't kotlin, don't use the custom type adapter
        if (type.rawType.declaredAnnotations.none { it.annotationClass.qualifiedName == "kotlin.Metadata" }) {
            return null
        }
        val kotlinClass: KClass<Any> = Reflection.createKotlinClass(type.rawType)
        val notNullableFields = kotlinClass.memberProperties.filter { !it.returnType.isMarkedNullable }

        return object : TypeAdapter<T>() {

            override fun write(out: JsonWriter, value: T?) = delegate.write(out, value)

            override fun read(input: JsonReader): T? {
                val value: T? = delegate.read(input)

                if (value != null) {
                    // Ensure none of its non-nullable fields were deserialized to null
                    notNullableFields.forEach {
                        if(it.get(value) == null){
                            throw JsonParseException("Value of non-nullable member [${it.name}] cannot be null")
                        }
                    }
                }
                return value
            }
        }
    }
}
