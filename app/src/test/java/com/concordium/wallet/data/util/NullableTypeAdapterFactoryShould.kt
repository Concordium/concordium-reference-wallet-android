package com.concordium.wallet.data.util

import com.concordium.wallet.core.gson.NullableTypeAdapterFactory
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import org.junit.Assert
import org.junit.Test

/**
 * Unit Test for the [NullableTypeAdapterFactory]
 */
class NullableTypeAdapterFactoryShould {

    data class TestObject(
        val type: String,
        val sender: String,
        val nullableValue: String?
    )

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapterFactory(NullableTypeAdapterFactory())
        .create()

    @Test
    fun returnObjectWhenDataIsValid() {
        val validJsonString = "{" +
                "\"type\": \"test type\"," +
                "\"sender\": \"test sender\"}"

        val validObject =
            TestObject(type = "test type", sender = "test sender", nullableValue = null)

        val testObject = gson.fromJson(validJsonString, TestObject::class.java)

        Assert.assertEquals(
            testObject, validObject
        )
    }

    @Test
    fun throwErrorWhenNonNullValueIsNotPresentInInput() {
        val invalidJsonString = "{" +
                "\"type\": \"test type\",}"

        Assert.assertThrows(JsonParseException::class.java) {
            gson.fromJson(invalidJsonString, TestObject::class.java)
        }
    }

    @Test
    fun throwErrorWhenNonNullValueIsNull() {
        val invalidJsonString = "{" +
                "\"type\": \"test type\"," +
                "\"sender\": null }"

        Assert.assertThrows(JsonParseException::class.java) {
            gson.fromJson(invalidJsonString, TestObject::class.java)
        }
    }

    @Test
    fun throwErrorWhenGsonWithoutTheAdapterWillNot() {
        val invalidJsonString = "{" +
                "\"type\": \"test type\"," +
                "\"sender\": null }"

        Assert.assertThrows(JsonParseException::class.java) {
            gson.fromJson(invalidJsonString, TestObject::class.java)
        }

        val testObject: TestObject = Gson().fromJson(invalidJsonString, TestObject::class.java)

        //testObject.sender is null for Gson without the adapter
        Assert.assertNull(testObject.sender)
    }
}
