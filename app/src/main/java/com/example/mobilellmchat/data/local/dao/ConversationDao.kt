package com.example.mobilellmchat.data.local.dao

import androidx.room.*
import com.example.mobilellmchat.data.local.entity.ChatMessageEntity
import com.example.mobilellmchat.data.local.entity.ConversationEntity
import com.example.mobilellmchat.model.FavoriteMessageWithConversation
import kotlinx.coroutines.flow.Flow

/**
 * ConversationDao
 *
 * [MODIFIED] 新增获取单条消息的方法
 *
 * @author AI-Assisted (Modified)
 * @since Sprint 1 (Updated Sprint 2)
 */
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

    /**
     * ✅ 新增：根据 ID 获取单条消息
     */
    @Query("SELECT * FROM chat_messages WHERE id = :id")
    suspend fun getMessageById(id: Long): ChatMessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    /**
     * ✅ 新增：更新消息（用于流式更新内容）
     */
    @Update
    suspend fun updateMessage(message: ChatMessageEntity)

    @Query("UPDATE chat_messages SET isLiked = :isLiked WHERE id = :id")
    suspend fun updateLikeStatus(id: Long, isLiked: Boolean)

    @Query("UPDATE chat_messages SET isFavorited = :isFavorited WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Long, isFavorited: Boolean)

    // 收藏功能
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

    @Query("SELECT conversationId FROM chat_messages WHERE id = :messageId")
    suspend fun getConversationIdByMessageId(messageId: Long): Long?
}