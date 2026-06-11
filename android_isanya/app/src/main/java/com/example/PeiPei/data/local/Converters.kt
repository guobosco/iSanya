// 文件说明：Room 类型转换器，在数据库与 Kotlin 类型之间做序列化/反序列化。

package com.example.Lulu.data.local

import androidx.room.TypeConverter
import com.example.Lulu.data.model.Comment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun fromTimestampMap(value: String?): Map<Long, String> {
        val mapType = object : TypeToken<Map<Long, String>>() {}.type
        return gson.fromJson(value, mapType) ?: emptyMap()
    }

    @TypeConverter
    fun toTimestampMap(map: Map<Long, String>?): String {
        return gson.toJson(map ?: emptyMap<Long, String>())
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromStringList(value: String?): List<String> {
        if (value == null) return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        @Suppress("UNCHECKED_CAST")
        val list = gson.fromJson<List<String>>(value, type)
        return list ?: emptyList()
    }

    @TypeConverter
    fun toStringList(list: List<String>?): String {
        return gson.toJson(list)
    }

    @TypeConverter
    fun fromDateList(value: String?): List<Date> {
        if (value == null) return emptyList()
        val type = object : TypeToken<List<Date>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun toDateList(list: List<Date>?): String {
        return gson.toJson(list)
    }

    @TypeConverter
    fun fromCommentList(value: String?): List<Comment> {
        if (value == null) return emptyList()
        val type = object : TypeToken<List<Comment>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun toCommentList(list: List<Comment>?): String {
        return gson.toJson(list)
    }

    @TypeConverter
    fun fromIntList(value: String?): List<Int> {
        if (value == null) return emptyList()
        val type = object : TypeToken<List<Int>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun toIntList(list: List<Int>?): String {
        return gson.toJson(list)
    }

    @TypeConverter
    fun fromLongList(value: String?): List<Long> {
        if (value == null) return emptyList()
        val type = object : TypeToken<List<Long>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun toLongList(list: List<Long>?): String {
        return gson.toJson(list)
    }

    @TypeConverter
    fun fromStringMap(value: String?): Map<String, String> {
        if (value == null) return emptyMap()
        val type = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson(value, type) ?: emptyMap()
    }

    @TypeConverter
    fun toStringMap(map: Map<String, String>?): String {
        return gson.toJson(map ?: emptyMap<String, String>())
    }
}
