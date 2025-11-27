package com.example.mobilellmchat.data.local.repository

import android.content.Context
import android.util.Log
import com.example.mobilellmchat.data.local.AppDatabase
import com.example.mobilellmchat.data.local.entity.ChatMessageEntity
import com.example.mobilellmchat.data.local.entity.ConversationEntity
import com.example.mobilellmchat.model.ApiMessage
import com.example.mobilellmchat.model.LLMService
import com.example.mobilellmchat.model.Message
import com.example.mobilellmchat.utils.ServiceFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * ChatRepository - 聊天数据仓库
 *
 * [MODIFIED] 新增 LLM 服务集成
 *
 * @author AI-Assisted (Modified)
 * @since Sprint 1
 */
class ChatRepository(private val database: AppDatabase) {

    private val dao = database.conversationDao()

    // ✅ 新增：LLM 服务实例（可切换）
    private var llmService: LLMService? = null

    // ✅ 新增：初始化 LLM 服务
    fun initialize(context: Context) {
        llmService = ServiceFactory.createLLMService(context)
        Log.d("ChatRepository", "LLM 服务已初始化: ${llmService?.getModelInfo()?.name}")
    }

    // ✅ 新增：切换 LLM 服务
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

    // ✅ 新增：发送消息（集成 LLM 服务）
    suspend fun sendMessageWithLLM(
        context: Context,
        conversationId: Long,
        userMessage: String
    ): String {
        // 确保服务已初始化
        if (llmService == null) {
            initialize(context)
        }

        // 1. 保存用户消息
        insertMessage(conversationId, userMessage, "user")

        // 2. 获取历史记录
        val history = getMessagesByConversationSync(conversationId)

        // 3. 转换为 API 消息格式
        val apiMessages = history.map { dbMsg ->
            ApiMessage(
                role = dbMsg.role,
                content = dbMsg.content
            )
        }

        // 4. 调用 LLM 服务
        val reply = llmService!!.chat(apiMessages)

        // 5. 保存 AI 回复
        insertMessage(conversationId, reply, "assistant")

        // 6. 更新会话时间
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

    // 数据转换
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
