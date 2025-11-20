package com.example.mobilellmchat.data.local.repository

import com.example.mobilellmchat.data.local.AppDatabase
import com.example.mobilellmchat.data.local.entity.ChatMessageEntity
import com.example.mobilellmchat.data.local.entity.ConversationEntity
import com.example.mobilellmchat.model.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChatRepository(private val database: AppDatabase) {

    private val dao = database.conversationDao()

    // --- Conversations ---
    fun getAllConversations(): Flow<List<ConversationEntity>> = dao.getAllConversations()

    suspend fun createConversation(title: String): Long {
        return dao.insertConversation(ConversationEntity(title = title))
    }

    suspend fun deleteConversation(id: Long) {
        dao.deleteConversation(id)
    }

    suspend fun getConversationById(id: Long): ConversationEntity? {
        return dao.getConversationById(id)
    }

    suspend fun updateConversation(conversation: ConversationEntity) {
        dao.updateConversation(conversation)
    }

    // --- Messages ---
    // 将数据库实体转换为 UI 模型
    fun getMessagesForConversation(conversationId: Long): Flow<List<Message>> {
        return dao.getMessagesByConversation(conversationId).map { entities ->
            entities.map { it.toUiModel() }
        }
    }

    suspend fun getMessagesByConversationSync(conversationId: Long): List<ChatMessageEntity> {
        return dao.getMessagesByConversationSync(conversationId)
    }

    suspend fun insertMessage(conversationId: Long, content: String, role: String) {
        val entity = ChatMessageEntity(
            conversationId = conversationId,
            role = role,
            content = content
        )
        dao.insertMessage(entity)
    }

    suspend fun toggleLike(id: Long, currentStatus: Boolean) {
        dao.updateLikeStatus(id, !currentStatus)
    }

    suspend fun toggleFavorite(id: Long, currentStatus: Boolean) {
        dao.updateFavoriteStatus(id, !currentStatus)
    }

    private fun ChatMessageEntity.toUiModel(): Message {
        return Message(
            id = this.id,
            role = this.role,
            content = this.content,
            timestamp = this.timestamp,
            isLiked = this.isLiked,
            isFavorited = this.isFavorited
        )
    }
}
