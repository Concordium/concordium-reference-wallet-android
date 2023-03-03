package com.concordium.wallet.data.util

import com.concordium.wallet.data.walletconnect.Params
import com.concordium.wallet.data.walletconnect.ParamsDeserializer
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import org.junit.Assert
import org.junit.Test

/**
 * Unit Test for the [ParamsDeserializer]
 */
class ParamsDeserializerShould {
    /**
     * The old dApp sends the Schema as a String and the new dApp sends the Schema as Object.
     * The app should handle booth cases.
     * The response should be the same in booth cases no mather the type of input
     */
    @Test
    fun returnSameOutputWhenSameSchemaIsPassedAsStringAndObject() {
        val paramsWithSchemaAsString = "{\"type\":\"Update\"," +
                "\"sender\":\"3KW3RkupTdDBomBqHztpMtSt4ZNUv2RdTbcSBgq7onSxg6hXRJ\"," +
                "\"payload\":\"{\\\"amount\\\":\\\"1000000\\\",\\\"address\\\":{\\\"index\\\":2059,\\\"subindex\\\":0},\\\"receiveName\\\":\\\"cis2_wCCD.wrap\\\",\\\"maxContractExecutionEnergy\\\":30000,\\\"message\\\":\\\"0031667ccdd75d558ef8e3437e1304ebc82e9d1d1c95b32641763c20ca0a6d04fa0000\\\"}\"," +
                "\"schema\":\"//8CAQAAAAkAAABjaXMyX3dDQ0QBABQAAgAAAAMAAAB1cmwWAgQAAABoYXNoFQIAAAAEAAAATm9uZQIEAAAAU29tZQEBAAAAEyAAAAACAQAAAAQAAAB3cmFwBBQAAgAAAAIAAAB0bxUCAAAABwAAAEFjY291bnQBAQAAAAsIAAAAQ29udHJhY3QBAgAAAAwWAQQAAABkYXRhHQEVBAAAAA4AAABJbnZhbGlkVG9rZW5JZAIRAAAASW5zdWZmaWNpZW50RnVuZHMCDAAAAFVuYXV0aG9yaXplZAIGAAAAQ3VzdG9tAQEAAAAVCQAAAAsAAABQYXJzZVBhcmFtcwIHAAAATG9nRnVsbAIMAAAATG9nTWFsZm9ybWVkAg4AAABDb250cmFjdFBhdXNlZAITAAAASW52b2tlQ29udHJhY3RFcnJvcgITAAAASW52b2tlVHJhbnNmZXJFcnJvcgIaAAAARmFpbGVkVXBncmFkZU1pc3NpbmdNb2R1bGUCHAAAAEZhaWxlZFVwZ3JhZGVNaXNzaW5nQ29udHJhY3QCJQAAAEZhaWxlZFVwZ3JhZGVVbnN1cHBvcnRlZE1vZHVsZVZlcnNpb24C\"}"

        val paramsWithSchemaAsObject = "{\"schema\":{" +
                "\"type\":\"module\"," +
                "\"value\":\"//8CAQAAAAkAAABjaXMyX3dDQ0QBABQAAgAAAAMAAAB1cmwWAgQAAABoYXNoFQIAAAAEAAAATm9uZQIEAAAAU29tZQEBAAAAEyAAAAACAQAAAAQAAAB3cmFwBBQAAgAAAAIAAAB0bxUCAAAABwAAAEFjY291bnQBAQAAAAsIAAAAQ29udHJhY3QBAgAAAAwWAQQAAABkYXRhHQEVBAAAAA4AAABJbnZhbGlkVG9rZW5JZAIRAAAASW5zdWZmaWNpZW50RnVuZHMCDAAAAFVuYXV0aG9yaXplZAIGAAAAQ3VzdG9tAQEAAAAVCQAAAAsAAABQYXJzZVBhcmFtcwIHAAAATG9nRnVsbAIMAAAATG9nTWFsZm9ybWVkAg4AAABDb250cmFjdFBhdXNlZAITAAAASW52b2tlQ29udHJhY3RFcnJvcgITAAAASW52b2tlVHJhbnNmZXJFcnJvcgIaAAAARmFpbGVkVXBncmFkZU1pc3NpbmdNb2R1bGUCHAAAAEZhaWxlZFVwZ3JhZGVNaXNzaW5nQ29udHJhY3QCJQAAAEZhaWxlZFVwZ3JhZGVVbnN1cHBvcnRlZE1vZHVsZVZlcnNpb24C\"}}"

        val paramsFromStringSchema = getParameters(paramsWithSchemaAsString)
        val paramsFromObjectSchema = getParameters(paramsWithSchemaAsObject)

        Assert.assertEquals(
            paramsFromStringSchema?.schema?.value,
            paramsFromObjectSchema?.schema?.value
        )
    }

    /**
     * If the parameters have invalid JSON, it should throw [JsonSyntaxException]
     */
    @Test
    fun throwErrorForInvalidJson() {
        val invalidJson = "not a json"
        Assert.assertThrows(JsonSyntaxException::class.java) {
            getParameters(invalidJson)
        }
    }

    /**
     * The Schema should be null if it is not present in the request
     */
    @Test
    fun setSchemaAsNullInResultIfNoSchemaPresentInInput() {
        val invalidJson = "{\"type\":\"Update\"," +
                "\"sender\":\"3KW3RkupTdDBomBqHztpMtSt4ZNUv2RdTbcSBgq7onSxg6hXRJ\"}"
        val params = getParameters(invalidJson)

        Assert.assertNull(params!!.schema)
    }

    /**
     * If the input is null, the response should also be null
     */
    @Test
    fun returnNullIfJsonIsNull() {
        val nullInput: String? = null
        Assert.assertNull(getParameters(nullInput))
    }

    /**
     * If the input is empty, the response should be null
     */
    @Test
    fun returnNullIfJsonIsEmpty() {
        val emptyInput = ""
        Assert.assertNull(getParameters(emptyInput))
    }

    /**
     * If the schema object in the request is flawed then the schema in the response should be null
     */
    @Test
    fun setSchemaAsNullInResultIfSchemaObjectIsFlowed() {
        val paramsWithSchemaAsFlowedObject = "{\"schema\":{" +
                "\"te\":\"module\"," +
                "\"value\":\"//8CAQAAAAkAAABjaXMyX3dDQ0QBABQAA\"}}"

        val params = getParameters(paramsWithSchemaAsFlowedObject)

        Assert.assertNull(params!!.schema)
    }

    /**
     * Get the [Params] using the [ParamsDeserializer]
     * @param input the String containing the JSON
     */
    private fun getParameters(input: String?): Params? {
        val jsonBuilder = GsonBuilder()
        jsonBuilder.registerTypeAdapter(
            Params::class.java,
            ParamsDeserializer()
        )
        return jsonBuilder.create().fromJson(input, Params::class.java)
    }
}