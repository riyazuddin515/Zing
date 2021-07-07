package com.riyazuddin.zing.other

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.riyazuddin.zing.data.entities.User
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

    @TypeConverter
    fun userToJson(user: User): String = Gson().toJson(user)

    @TypeConverter
    fun jsonToUser(user: String) = Gson().fromJson(user, User::class.java)
}