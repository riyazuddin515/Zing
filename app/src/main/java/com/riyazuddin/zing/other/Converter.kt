package com.riyazuddin.zing.other

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.riyazuddin.zing.data.entities.ReplyToMessage
import com.riyazuddin.zing.data.entities.User
import java.util.*

class Converter {

    private val gson = Gson()

    @TypeConverter
    fun fromDateToLong(date: Date?) = date?.time

    @TypeConverter
    fun fromLongToDate(dateLong: Long?) = dateLong?.let {
        Date(it)
    }

    @TypeConverter
    fun listToJson(value: List<String>): String = Gson().toJson(value)

    @TypeConverter
    fun jsonToList(value: String) = gson.fromJson(value, Array<String>::class.java).toList()

    @TypeConverter
    fun userToJson(user: User): String = gson.toJson(user)

    @TypeConverter
    fun jsonToUser(user: String): User = gson.fromJson(user, User::class.java)

    @TypeConverter
    fun replyToMessageToJson(replyToMessage: ReplyToMessage): String = gson.toJson(replyToMessage)

    @TypeConverter
    fun jsonToReplyToMessage(replyToMessage: String): ReplyToMessage =
        gson.fromJson(replyToMessage, ReplyToMessage::class.java)
}