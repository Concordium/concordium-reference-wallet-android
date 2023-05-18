package com.concordium.wallet.core.gson

import com.concordium.wallet.util.toBigDecimal
import com.concordium.wallet.util.toPlainStringStripped
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.math.BigDecimal

class BigDecimalTypeAdapter : TypeAdapter<BigDecimal>() {
    override fun write(out: JsonWriter?, value: BigDecimal?) {
        if (out != null) {
            if (value != null) {
                out.value(value.toPlainStringStripped())
            } else {
                out.nullValue()
            }
        }
    }

    override fun read(input: JsonReader?): BigDecimal? {
        if (input != null) {
            return input.nextString().toBigDecimal()
        }
        return null
    }
}