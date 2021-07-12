package com.riyazuddin.zing.repositories.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.riyazuddin.zing.data.entities.LastMessage
import com.riyazuddin.zing.other.Converter

@Database(entities = [LastMessage::class], version = 2)
@TypeConverters(Converter::class)
abstract class ChatDatabase : RoomDatabase() {

    abstract fun getLastMessagesDao(): LastMessageDao

}