package com.riyazuddin.zing.other

import androidx.room.TypeConverter
import com.google.gson.Gson
import java.util.*

class Converter {

    @TypeConverter
    fun fromDateToLong(date: Date?) = date?.time

    @TypeConverter
    fun fromLongToDate(dateLong: Long?) = dateLong?.let {
        Date(it)
    }

    @TypeConverter
    fun listToJson(value: List<String>): String = Gson().toJson(value)

    @TypeConverter
    fun jsonToList(value: String) = Gson().fromJson(value, Array<String>::class.java).toList()
}