package com.example.mobilellmchat.data.local.repository

import com.example.mobilellmchat.data.local.AppDatabase
import com.example.mobilellmchat.data.local.entity.ChatMessageEntity
import com.example.mobilellmchat.data.local.entity.ConversationEntity
import com.example.mobilellmchat.model.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChatRepository(private val database: AppDatabase) {

    private val conversationDao = database.conversationDao()
    private val messageDao = database.chatMessageDao()

    // ========== 会话管理 ==========

    fun getAllConversations(): Flow<List<ConversationEntity>> {
        return conversationDao.getAllConversations()
    }

    suspend fun getConversationById(conversationId: Long): ConversationEntity? {
        return conversationDao.getConversationById(conversationId)
    }

    suspend fun createConversation(title: String): Long {
        val conversation = ConversationEntity(title = title)
        return conversationDao.insertConversation(conversation)
    }

    suspend fun updateConversation(conversation: ConversationEntity) {
        conversationDao.updateConversation(conversation.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteConversation(conversationId: Long) {
        conversationDao.deleteConversationById(conversationId)
    }

    // ========== 消息管理 ==========

    fun getMessagesByConversation(conversationId: Long): Flow<List<Message>> {
        return messageDao.getMessagesByConversation(conversationId).map { entities ->
            entities.map { it.toMessage() }
        }
    }

    suspend fun getMessagesByConversationSync(conversationId: Long): List<Message> {
        return messageDao.getMessagesByConversationSync(conversationId).map { it.toMessage() }
    }

    suspend fun saveMessage(conversationId: Long, message: Message) {
        val entity = ChatMessageEntity(
            conversationId = conversationId,
            content = message.content,
            role = message.role,
            timestamp = message.timestamp
        )
        messageDao.insertMessage(entity)
    }

    suspend fun saveMessages(conversationId: Long, messages: List<Message>) {
        val entities = messages.map { message ->
            ChatMessageEntity(
                conversationId = conversationId,
                content = message.content,
                role = message.role,
                timestamp = message.timestamp
            )
        }
        messageDao.insertMessages(entities)
    }

    suspend fun clearConversationMessages(conversationId: Long) {
        messageDao.deleteMessagesByConversation(conversationId)
    }

    // ========== 工具方法 ==========

    private fun ChatMessageEntity.toMessage(): Message {
        return Message(
            content = this.content,
            role = this.role,
            timestamp = this.timestamp
        )
    }
}
