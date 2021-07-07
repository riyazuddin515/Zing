package com.riyazuddin.zing.repositories.local

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.riyazuddin.zing.data.entities.LastMessage

@Dao
interface LastMessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLastMessage(lastMessage: LastMessage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLastMessages(lastMessages: List<LastMessage>)

    @Query("SELECT * FROM lastMessages ORDER BY date DESC")
    fun getAllLastMessages(): LiveData<List<LastMessage>>

    @Query("SELECT COUNT(*) FROM lastMessages WHERE chatThread == :chatThread")
    suspend fun checkChatThreadAlreadyExists(chatThread: String): Int

    @Query("DELETE FROM lastMessages")
    suspend fun deleteTableData()

    @Query("SELECT * FROM lastMessages WHERE chatThread == :chatThread")
    suspend fun getLastMessage(chatThread: String): LastMessage

    @Query("UPDATE lastMessages SET status = :seen WHERE chatThread == :chatThread")
    suspend fun updateLastMessageAsSeen(chatThread: String, seen: String)

    @Query("SELECT * FROM lastMessages ORDER BY date DESC LIMIT 1")
    suspend fun getLastLastMessage(): LastMessage?

}