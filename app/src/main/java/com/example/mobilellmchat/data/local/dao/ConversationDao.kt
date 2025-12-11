package com.example.mobilellmchat.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.mobilellmchat.data.local.entity.ChatMessageEntity
import com.example.mobilellmchat.data.local.entity.ConversationEntity
import com.example.mobilellmchat.model.FavoriteMessageWithConversation
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    // --- Conversations ---
    @Query("SELECT * FROM conversations ORDER BY timestamp DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getConversationById(id: Long): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity): Long

    @Update
    suspend fun updateConversation(conversation: ConversationEntity)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversation(id: Long)

    // --- Messages ---
    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesByConversation(conversationId: Long): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    suspend fun getMessagesByConversationSync(conversationId: Long): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Query("UPDATE chat_messages SET isLiked = :isLiked WHERE id = :id")
    suspend fun updateLikeStatus(id: Long, isLiked: Boolean)

    @Query("UPDATE chat_messages SET isFavorited = :isFavorited WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Long, isFavorited: Boolean)

    // ✅ 新增：查询所有收藏消息（带会话信息）
    @Query("""
        SELECT 
            m.id,
            m.content,
            m.timestamp,
            m.conversationId,
            c.title as conversationTitle
        FROM chat_messages m
        INNER JOIN conversations c ON m.conversationId = c.id
        WHERE m.isFavorited = 1 AND m.role = 'assistant'
        ORDER BY m.timestamp DESC
    """)
    fun getAllFavoritedMessagesWithConversation(): Flow<List<FavoriteMessageWithConversation>>

    // ✅ 新增：根据消息ID获取会话ID
    @Query("SELECT conversationId FROM chat_messages WHERE id = :messageId")
    suspend fun getConversationIdByMessageId(messageId: Long): Long?
}