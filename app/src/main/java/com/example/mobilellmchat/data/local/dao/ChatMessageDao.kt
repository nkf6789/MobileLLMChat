package com.example.mobilellmchat.data.local.dao

import androidx.room.*
import com.example.mobilellmchat.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {

    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesByConversation(conversationId: Long): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    suspend fun getMessagesByConversationSync(conversationId: Long): List<ChatMessageEntity>

    @Insert
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Insert
    suspend fun insertMessages(messages: List<ChatMessageEntity>)

    @Update
    suspend fun updateMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesByConversation(conversationId: Long)

    @Query("UPDATE chat_messages SET isLiked = :isLiked WHERE id = :messageId")
    suspend fun updateLike(messageId: Long, isLiked: Boolean)

    @Query("UPDATE chat_messages SET isFavorited = :isFavorited WHERE id = :messageId")
    suspend fun updateFavorite(messageId: Long, isFavorited: Boolean)
}
