package com.concordium.wallet.data.util
import com.concordium.wallet.data.walletconnect.Params
import com.concordium.wallet.data.walletconnect.ParamsDeserializer
import com.google.gson.GsonBuilder
import org.junit.Assert
import org.junit.Test


class ParamsDeserializerUnitTest {

    @Test
    fun testParamsDeserializer() {
        val paramsWithSchemaAsString = "{\"type\":\"Update\"," +
                "\"sender\":\"3KW3RkupTdDBomBqHztpMtSt4ZNUv2RdTbcSBgq7onSxg6hXRJ\"," +
                "\"payload\":\"{\\\"amount\\\":\\\"1000000\\\",\\\"address\\\":{\\\"index\\\":2059,\\\"subindex\\\":0},\\\"receiveName\\\":\\\"cis2_wCCD.wrap\\\",\\\"maxContractExecutionEnergy\\\":30000,\\\"message\\\":\\\"0031667ccdd75d558ef8e3437e1304ebc82e9d1d1c95b32641763c20ca0a6d04fa0000\\\"}\"," +
                "\"schema\":\"//8CAQAAAAkAAABjaXMyX3dDQ0QBABQAAgAAAAMAAAB1cmwWAgQAAABoYXNoFQIAAAAEAAAATm9uZQIEAAAAU29tZQEBAAAAEyAAAAACAQAAAAQAAAB3cmFwBBQAAgAAAAIAAAB0bxUCAAAABwAAAEFjY291bnQBAQAAAAsIAAAAQ29udHJhY3QBAgAAAAwWAQQAAABkYXRhHQEVBAAAAA4AAABJbnZhbGlkVG9rZW5JZAIRAAAASW5zdWZmaWNpZW50RnVuZHMCDAAAAFVuYXV0aG9yaXplZAIGAAAAQ3VzdG9tAQEAAAAVCQAAAAsAAABQYXJzZVBhcmFtcwIHAAAATG9nRnVsbAIMAAAATG9nTWFsZm9ybWVkAg4AAABDb250cmFjdFBhdXNlZAITAAAASW52b2tlQ29udHJhY3RFcnJvcgITAAAASW52b2tlVHJhbnNmZXJFcnJvcgIaAAAARmFpbGVkVXBncmFkZU1pc3NpbmdNb2R1bGUCHAAAAEZhaWxlZFVwZ3JhZGVNaXNzaW5nQ29udHJhY3QCJQAAAEZhaWxlZFVwZ3JhZGVVbnN1cHBvcnRlZE1vZHVsZVZlcnNpb24C\"}"

        val paramsWithSchemaAsObject = "{\"schema\":{" +
                "\"type\":\"module\"," +
                "\"value\":\"//8CAQAAAAkAAABjaXMyX3dDQ0QBABQAAgAAAAMAAAB1cmwWAgQAAABoYXNoFQIAAAAEAAAATm9uZQIEAAAAU29tZQEBAAAAEyAAAAACAQAAAAQAAAB3cmFwBBQAAgAAAAIAAAB0bxUCAAAABwAAAEFjY291bnQBAQAAAAsIAAAAQ29udHJhY3QBAgAAAAwWAQQAAABkYXRhHQEVBAAAAA4AAABJbnZhbGlkVG9rZW5JZAIRAAAASW5zdWZmaWNpZW50RnVuZHMCDAAAAFVuYXV0aG9yaXplZAIGAAAAQ3VzdG9tAQEAAAAVCQAAAAsAAABQYXJzZVBhcmFtcwIHAAAATG9nRnVsbAIMAAAATG9nTWFsZm9ybWVkAg4AAABDb250cmFjdFBhdXNlZAITAAAASW52b2tlQ29udHJhY3RFcnJvcgITAAAASW52b2tlVHJhbnNmZXJFcnJvcgIaAAAARmFpbGVkVXBncmFkZU1pc3NpbmdNb2R1bGUCHAAAAEZhaWxlZFVwZ3JhZGVNaXNzaW5nQ29udHJhY3QCJQAAAEZhaWxlZFVwZ3JhZGVVbnN1cHBvcnRlZE1vZHVsZVZlcnNpb24C\"}}"

        val jsonBuilder = GsonBuilder()
        jsonBuilder.registerTypeAdapter(
            Params::class.java,
            ParamsDeserializer()
        )

        val paramsFromStringSchema =
            jsonBuilder.create().fromJson(paramsWithSchemaAsString, Params::class.java)
        val paramsFromObjectSchema =
            jsonBuilder.create().fromJson(paramsWithSchemaAsObject, Params::class.java)

        Assert.assertEquals(
            paramsFromStringSchema.schema?.value,
            paramsFromObjectSchema.schema?.value
        )
    }
}