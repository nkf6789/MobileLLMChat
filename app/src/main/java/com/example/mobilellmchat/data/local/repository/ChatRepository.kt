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
 * [MODIFIED] 新增收藏功能支持
 *
 * @author AI-Assisted (Modified)
 * @since Sprint 1
 */
class ChatRepository(private val database: AppDatabase) {

    private val dao = database.conversationDao()

    // ✅ LLM 服务实例
    private var llmService: LLMService? = null

    // ✅ 初始化 LLM 服务
    fun initialize(context: Context) {
        llmService?.release()
        llmService = ServiceFactory.createLLMService(context)
        Log.d("ChatRepository", "LLM 服务已初始化: ${llmService?.getModelInfo()?.name}")
    }

    /**
     * ✅ 重新初始化服务（用于配置更改后）
     */
    fun reinitialize(context: Context) {
        Log.d("ChatRepository", "🔄 重新初始化 LLM 服务...")
        Log.d("ChatRepository", "旧服务实例: ${llmService?.hashCode()}")
        initialize(context)
        Log.d("ChatRepository", "新服务实例: ${llmService?.hashCode()}")
    }

    // ✅ 切换 LLM 服务
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

    suspend fun insertMessage(conversationId: Long, content: String, role: String) {
        val entity = ChatMessageEntity(
            conversationId = conversationId,
            role = role,
            content = content
        )
        dao.insertMessage(entity)
    }

    // ✅ 发送消息（集成 LLM 服务）
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

    // 互动功能
    suspend fun toggleLike(id: Long, currentStatus: Boolean) {
        dao.updateLikeStatus(id, !currentStatus)
    }

    suspend fun toggleFavorite(id: Long, currentStatus: Boolean) {
        dao.updateFavoriteStatus(id, !currentStatus)
    }

    // ✅ 新增：获取所有收藏消息（带会话信息）
    fun getAllFavoritedMessagesWithConversation(): Flow<List<FavoriteMessageWithConversation>> {
        return dao.getAllFavoritedMessagesWithConversation()
    }

    // ✅ 新增：根据消息ID获取会话ID
    suspend fun getConversationIdByMessageId(messageId: Long): Long? {
        return dao.getConversationIdByMessageId(messageId)
    }

    // 数据转换
    private fun ChatMessageEntity.toUiModel(): Message {
        return Message(
            id = this.id,
            conversationId = this.conversationId,  // ✅ 新增
            role = this.role,
            content = this.content,
            timestamp = this.timestamp,
            isLiked = this.isLiked,
            isFavorited = this.isFavorited
        )
    }
}