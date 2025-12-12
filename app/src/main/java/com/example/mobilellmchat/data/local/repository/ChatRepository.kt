package com.example.mobilellmchat.data.local.repository

import android.content.Context
import android.util.Log
import com.example.mobilellmchat.data.local.AppDatabase
import com.example.mobilellmchat.data.local.entity.ChatMessageEntity
import com.example.mobilellmchat.data.local.entity.ConversationEntity
import com.example.mobilellmchat.model.ApiMessage
import com.example.mobilellmchat.model.FavoriteMessageWithConversation
import com.example.mobilellmchat.model.LLMService
import com.example.mobilellmchat.model.Message
import com.example.mobilellmchat.utils.ServiceFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * ChatRepository - 聊天数据仓库
 *
 * [MODIFIED] 新增流式发送方法
 *
 * @author AI-Assisted (Modified)
 * @since Sprint 1 (Updated Sprint 2)
 */
class ChatRepository(private val database: AppDatabase) {

    private val dao = database.conversationDao()
    private var llmService: LLMService? = null

    fun initialize(context: Context) {
        llmService?.release()
        llmService = ServiceFactory.createLLMService(context)
        Log.d("ChatRepository", "LLM 服务已初始化: ${llmService?.getModelInfo()?.name}")
    }

    fun reinitialize(context: Context) {
        Log.d("ChatRepository", "🔄 重新初始化 LLM 服务...")
        initialize(context)
    }

    fun switchLLMService(newService: LLMService) {
        llmService = newService
        Log.d("ChatRepository", "已切换到: ${newService.getModelInfo().name}")
    }

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
    fun getMessagesForConversation(conversationId: Long): Flow<List<Message>> {
        return dao.getMessagesByConversation(conversationId).map { entities ->
            entities.map { it.toUiModel() }
        }
    }

    suspend fun getMessagesByConversationSync(conversationId: Long): List<ChatMessageEntity> {
        return dao.getMessagesByConversationSync(conversationId)
    }

    suspend fun insertMessage(conversationId: Long, content: String, role: String): Long {
        val entity = ChatMessageEntity(
            conversationId = conversationId,
            role = role,
            content = content
        )
        return dao.insertMessage(entity)
    }

    /**
     * ✅ 新增：更新消息内容（用于流式追加）
     */
    suspend fun updateMessageContent(messageId: Long, newContent: String) {
        val message = dao.getMessageById(messageId) ?: return
        val updatedMessage = message.copy(content = newContent)
        dao.updateMessage(updatedMessage)
    }

    /**
     * 发送消息（非流式）- 保持原有逻辑
     */
    suspend fun sendMessageWithLLM(
        context: Context,
        conversationId: Long,
        userMessage: String
    ): String {
        if (llmService == null) {
            initialize(context)
        }

        insertMessage(conversationId, userMessage, "user")
        val history = getMessagesByConversationSync(conversationId)
        val apiMessages = history.map { dbMsg ->
            ApiMessage(role = dbMsg.role, content = dbMsg.content)
        }
        val reply = llmService!!.chat(apiMessages)
        insertMessage(conversationId, reply, "assistant")

        val currentConv = getConversationById(conversationId)
        currentConv?.let { updateConversation(it) }

        return reply
    }

    /**
     * ✅ 新增：流式发送消息
     *
     * @return Pair<Long, Flow<String>> - (消息ID, 文本流)
     */
    suspend fun sendMessageWithLLMStream(
        context: Context,
        conversationId: Long,
        userMessage: String
    ): Pair<Long, Flow<String>> {
        if (llmService == null) {
            initialize(context)
        }

        // 1. 保存用户消息
        insertMessage(conversationId, userMessage, "user")

        // 2. 创建一个空的 AI 消息占位
        val aiMessageId = insertMessage(conversationId, "", "assistant")

        // 3. 获取历史记录
        val history = getMessagesByConversationSync(conversationId)
        val apiMessages = history.map { dbMsg ->
            ApiMessage(role = dbMsg.role, content = dbMsg.content)
        }

        // 4. 返回消息ID和文本流
        val textFlow = llmService!!.chatStream(apiMessages)

        return Pair(aiMessageId, textFlow)
    }

    // 互动功能
    suspend fun toggleLike(id: Long, currentStatus: Boolean) {
        dao.updateLikeStatus(id, !currentStatus)
    }

    suspend fun toggleFavorite(id: Long, currentStatus: Boolean) {
        dao.updateFavoriteStatus(id, !currentStatus)
    }

    fun getAllFavoritedMessagesWithConversation(): Flow<List<FavoriteMessageWithConversation>> {
        return dao.getAllFavoritedMessagesWithConversation()
    }

    suspend fun getConversationIdByMessageId(messageId: Long): Long? {
        return dao.getConversationIdByMessageId(messageId)
    }

    // 数据转换
    private fun ChatMessageEntity.toUiModel(): Message {
        return Message(
            id = this.id,
            conversationId = this.conversationId,
            role = this.role,
            content = this.content,
            timestamp = this.timestamp,
            isLiked = this.isLiked,
            isFavorited = this.isFavorited,
            isStreaming = false  // 默认非流式
        )
    }
}