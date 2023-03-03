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

    @Test
    fun matchTheExpectedOutputWhenTheSchemaIsString(){
        val expectedOutput = "//8CAQAAAAkAAABjaXMyX3dDQ0QBABQAAgAAAAMAAAB1cmwWAgQAAABoYXNoFQIAAAAEAAAATm9uZQIEAAAAU29tZQEBAAAAEyAAAAACAQAAAAQAAAB3cmFwBBQAAgAAAAIAAAB0bxUCAAAABwAAAEFjY291bnQBAQAAAAsIAAAAQ29udHJhY3QBAgAAAAwWAQQAAABkYXRhHQEVBAAAAA4AAABJbnZhbGlkVG9rZW5JZAIRAAAASW5zdWZmaWNpZW50RnVuZHMCDAAAAFVuYXV0aG9yaXplZAIGAAAAQ3VzdG9tAQEAAAAVCQAAAAsAAABQYXJzZVBhcmFtcwIHAAAATG9nRnVsbAIMAAAATG9nTWFsZm9ybWVkAg4AAABDb250cmFjdFBhdXNlZAITAAAASW52b2tlQ29udHJhY3RFcnJvcgITAAAASW52b2tlVHJhbnNmZXJFcnJvcgIaAAAARmFpbGVkVXBncmFkZU1pc3NpbmdNb2R1bGUCHAAAAEZhaWxlZFVwZ3JhZGVNaXNzaW5nQ29udHJhY3QCJQAAAEZhaWxlZFVwZ3JhZGVVbnN1cHBvcnRlZE1vZHVsZVZlcnNpb24C"
        val paramsWithSchemaAsString = "{\"type\":\"Update\"," +
                "\"sender\":\"3KW3RkupTdDBomBqHztpMtSt4ZNUv2RdTbcSBgq7onSxg6hXRJ\"," +
                "\"payload\":\"{\\\"amount\\\":\\\"1000000\\\",\\\"address\\\":{\\\"index\\\":2059,\\\"subindex\\\":0},\\\"receiveName\\\":\\\"cis2_wCCD.wrap\\\",\\\"maxContractExecutionEnergy\\\":30000,\\\"message\\\":\\\"0031667ccdd75d558ef8e3437e1304ebc82e9d1d1c95b32641763c20ca0a6d04fa0000\\\"}\"," +
                "\"schema\":\"//8CAQAAAAkAAABjaXMyX3dDQ0QBABQAAgAAAAMAAAB1cmwWAgQAAABoYXNoFQIAAAAEAAAATm9uZQIEAAAAU29tZQEBAAAAEyAAAAACAQAAAAQAAAB3cmFwBBQAAgAAAAIAAAB0bxUCAAAABwAAAEFjY291bnQBAQAAAAsIAAAAQ29udHJhY3QBAgAAAAwWAQQAAABkYXRhHQEVBAAAAA4AAABJbnZhbGlkVG9rZW5JZAIRAAAASW5zdWZmaWNpZW50RnVuZHMCDAAAAFVuYXV0aG9yaXplZAIGAAAAQ3VzdG9tAQEAAAAVCQAAAAsAAABQYXJzZVBhcmFtcwIHAAAATG9nRnVsbAIMAAAATG9nTWFsZm9ybWVkAg4AAABDb250cmFjdFBhdXNlZAITAAAASW52b2tlQ29udHJhY3RFcnJvcgITAAAASW52b2tlVHJhbnNmZXJFcnJvcgIaAAAARmFpbGVkVXBncmFkZU1pc3NpbmdNb2R1bGUCHAAAAEZhaWxlZFVwZ3JhZGVNaXNzaW5nQ29udHJhY3QCJQAAAEZhaWxlZFVwZ3JhZGVVbnN1cHBvcnRlZE1vZHVsZVZlcnNpb24C\"}"

        val paramsFromStringSchema = getParameters(paramsWithSchemaAsString)

        Assert.assertEquals(
            paramsFromStringSchema?.schema?.value,
            expectedOutput
        )
    }

    @Test
    fun matchTheExpectedOutputWhenTheSchemaIsObject(){
        val expectedOutput = "//8CAQAAAAkAAABjaXMyX3dDQ0QBABQAAgAAAAMAAAB1cmwWAgQAAABoYXNoFQIAAAAEAAAATm9uZQIEAAAAU29tZQEBAAAAEyAAAAACAQAAAAQAAAB3cmFwBBQAAgAAAAIAAAB0bxUCAAAABwAAAEFjY291bnQBAQAAAAsIAAAAQ29udHJhY3QBAgAAAAwWAQQAAABkYXRhHQEVBAAAAA4AAABJbnZhbGlkVG9rZW5JZAIRAAAASW5zdWZmaWNpZW50RnVuZHMCDAAAAFVuYXV0aG9yaXplZAIGAAAAQ3VzdG9tAQEAAAAVCQAAAAsAAABQYXJzZVBhcmFtcwIHAAAATG9nRnVsbAIMAAAATG9nTWFsZm9ybWVkAg4AAABDb250cmFjdFBhdXNlZAITAAAASW52b2tlQ29udHJhY3RFcnJvcgITAAAASW52b2tlVHJhbnNmZXJFcnJvcgIaAAAARmFpbGVkVXBncmFkZU1pc3NpbmdNb2R1bGUCHAAAAEZhaWxlZFVwZ3JhZGVNaXNzaW5nQ29udHJhY3QCJQAAAEZhaWxlZFVwZ3JhZGVVbnN1cHBvcnRlZE1vZHVsZVZlcnNpb24C"
        val paramsWithSchemaAsObject = "{\"schema\":{" +
                "\"type\":\"module\"," +
                "\"value\":\"//8CAQAAAAkAAABjaXMyX3dDQ0QBABQAAgAAAAMAAAB1cmwWAgQAAABoYXNoFQIAAAAEAAAATm9uZQIEAAAAU29tZQEBAAAAEyAAAAACAQAAAAQAAAB3cmFwBBQAAgAAAAIAAAB0bxUCAAAABwAAAEFjY291bnQBAQAAAAsIAAAAQ29udHJhY3QBAgAAAAwWAQQAAABkYXRhHQEVBAAAAA4AAABJbnZhbGlkVG9rZW5JZAIRAAAASW5zdWZmaWNpZW50RnVuZHMCDAAAAFVuYXV0aG9yaXplZAIGAAAAQ3VzdG9tAQEAAAAVCQAAAAsAAABQYXJzZVBhcmFtcwIHAAAATG9nRnVsbAIMAAAATG9nTWFsZm9ybWVkAg4AAABDb250cmFjdFBhdXNlZAITAAAASW52b2tlQ29udHJhY3RFcnJvcgITAAAASW52b2tlVHJhbnNmZXJFcnJvcgIaAAAARmFpbGVkVXBncmFkZU1pc3NpbmdNb2R1bGUCHAAAAEZhaWxlZFVwZ3JhZGVNaXNzaW5nQ29udHJhY3QCJQAAAEZhaWxlZFVwZ3JhZGVVbnN1cHBvcnRlZE1vZHVsZVZlcnNpb24C\"}}"

        val paramsFromObjectSchema = getParameters(paramsWithSchemaAsObject)

        Assert.assertEquals(
            paramsFromObjectSchema?.schema?.value,
            expectedOutput
        )
    }

    @Test
    fun throwErrorForInvalidJson() {
        val invalidJson = "not a json"
        Assert.assertThrows(JsonSyntaxException::class.java) {
            getParameters(invalidJson)
        }
    }

    @Test
    fun setSchemaAsNullInResultIfNoSchemaPresentInInput() {
        val invalidJson = "{\"type\":\"Update\"," +
                "\"sender\":\"3KW3RkupTdDBomBqHztpMtSt4ZNUv2RdTbcSBgq7onSxg6hXRJ\"}"
        val params = getParameters(invalidJson)

        Assert.assertNull(params!!.schema)
    }

    @Test
    fun returnNullIfJsonIsNull() {
        val nullInput: String? = null
        Assert.assertNull(getParameters(nullInput))
    }

    @Test
    fun returnNullIfJsonIsEmpty() {
        val emptyInput = ""
        Assert.assertNull(getParameters(emptyInput))
    }

    @Test
    fun setSchemaAsNullInResultIfSchemaObjectIsFlawed() {
        val paramsWithSchemaAsFlowedObject = "{\"schema\":{" +
                "\"te\":\"module\"," +
                "\"value\":\"//8CAQAAAAkAAABjaXMyX3dDQ0QBABQAA\"}}"

        val params = getParameters(paramsWithSchemaAsFlowedObject)

        Assert.assertNull(params!!.schema)
    }

    private fun getParameters(input: String?): Params? {
        val jsonBuilder = GsonBuilder()
        jsonBuilder.registerTypeAdapter(
            Params::class.java,
            ParamsDeserializer()
        )
        return jsonBuilder.create().fromJson(input, Params::class.java)
    }
}