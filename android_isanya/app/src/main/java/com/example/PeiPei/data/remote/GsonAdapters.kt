// 文件说明：Gson 自定义 TypeAdapter，处理特殊 JSON 序列化规则。

package com.example.Lulu.data.remote

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.util.Date

/**
 * Gson TypeAdapter for converting between Long (timestamp) and Date.
 * Handles both numeric timestamps (Long) and nulls.
 */
class DateTypeAdapter : TypeAdapter<Date>() {
    override fun write(out: JsonWriter, value: Date?) {
        if (value == null) {
            out.nullValue()
        } else {
            out.value(value.time)
        }
    }

    override fun read(`in`: JsonReader): Date? {
        if (`in`.peek() == JsonToken.NULL) {
            `in`.nextNull()
            return null
        }
        return try {
            val timestamp = `in`.nextLong()
            Date(timestamp)
        } catch (e: Exception) {
            // Fallback for non-numeric dates if API changes schema
            `in`.skipValue()
            null
        }
    }
}
