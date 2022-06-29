package com.concordium.wallet.core.gson

import com.concordium.wallet.data.model.RawJson
import com.google.gson.JsonParser
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

class RawJsonTypeAdapter : TypeAdapter<RawJson>() {

    override fun read(reader: JsonReader?): RawJson {
        if (reader == null) {
            return RawJson("")
        }
        return RawJson(JsonParser.parseReader(reader).toString())
    }

    override fun write(writer: JsonWriter?, value: RawJson?) {
        if (writer != null && value != null) {
            writer.jsonValue(value.json)
        }
    }
}