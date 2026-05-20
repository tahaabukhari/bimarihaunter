package com.bimarihaunter.db

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class OutbreakConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()

        return try {
            val listType = object : TypeToken<List<String>>() {}.type
            gson.fromJson(value, listType)
        } catch (e: Exception) {
            // Fall back to semicolon-delimited legacy data
            value.split(";").map { it.trim() }.filter { it.isNotBlank() }
        }
    }
}
