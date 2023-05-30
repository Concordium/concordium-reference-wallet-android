package com.concordium.wallet.core.gson

import com.concordium.wallet.util.toBigInteger
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.math.BigInteger

class BigIntegerTypeAdapter : TypeAdapter<BigInteger>() {
    override fun write(out: JsonWriter?, value: BigInteger?) {
        if (out != null) {
            if (value != null) {
                out.value(value.toString())
            } else {
                out.nullValue()
            }
        }
    }

    override fun read(input: JsonReader?): BigInteger? {
        if (input != null) {
            return input.nextString().toBigInteger()
        }
        return null
    }
}