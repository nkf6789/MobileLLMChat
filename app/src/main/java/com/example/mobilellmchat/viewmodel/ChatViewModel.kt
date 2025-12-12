package com.example.mobilellmchat.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.mobilellmchat.data.local.repository.ChatRepository
import com.example.mobilellmchat.data.local.entity.ConversationEntity
import com.example.mobilellmchat.model.Message
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * ChatViewModel
 *
 * [MODIFIED] 新增流式发送支持 + 停止生成功能
 *
 * @author AI-Assisted (Modified)
 * @since Sprint 2 - Stream Support with Stop Function
 */
class ChatViewModel(
    application: Application,
    private val repository: ChatRepository
) : AndroidViewModel(application) {

    // 1. 会话列表
    val conversations: LiveData<List<ConversationEntity>> = repository.getAllConversations()
        .catch { e -> Log.e(TAG, "Error loading conversations", e) }
        .asLiveData()

    // 2. 当前选中的会话 ID
    private val _currentConversationId = MutableLiveData<Long>()
    val currentConversationId: LiveData<Long> get() = _currentConversationId

    // 3. 消息列表
    val messages: LiveData<List<Message>> = _currentConversationId.switchMap { id ->
        repository.getMessagesForConversation(id).asLiveData()
    }

    // 4. UI 状态
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _toastMessage = MutableLiveData<String>()
    val toastMessage: LiveData<String> get() = _toastMessage

    // 流式消息状态
    private val _streamingMessageId = MutableLiveData<Long?>()
    val streamingMessageId: LiveData<Long?> get() = _streamingMessageId

    private val _streamingContent = MutableLiveData<String>()
    val streamingContent: LiveData<String> get() = _streamingContent

    // ✅ 新增：用于取消流式生成的 Job
    private var streamingJob: Job? = null

    init {
        Log.d(TAG, "ViewModel Initialized")
    }

    // ========== 业务逻辑方法 ==========

    fun switchConversation(conversationId: Long) {
        _currentConversationId.value = conversationId
    }

    fun createNewConversation() {
        viewModelScope.launch {
            try {
                val newTitle = "新对话 ${System.currentTimeMillis() / 1000}"
                val newId = repository.createConversation(newTitle)
                _currentConversationId.value = newId
            } catch (e: Exception) {
                _toastMessage.value = "创建会话失败: ${e.message}"
            }
        }
    }

    fun deleteConversation(conversation: ConversationEntity) {
        viewModelScope.launch {
            repository.deleteConversation(conversation.id)
            if (_currentConversationId.value == conversation.id) {
                // 删除当前会话后可以重置 ID
            }
        }
    }

    /**
     * 流式发送消息
     */
    fun sendMessageStream(content: String) {
        val conversationId = _currentConversationId.value
        if (conversationId == null) {
            _toastMessage.value = "请先选择或创建一个会话"
            return
        }

        if (content.isBlank()) return

        // ✅ 保存 Job 引用，用于停止生成
        streamingJob = viewModelScope.launch {
            try {
                _isLoading.value = true
                _streamingContent.value = ""

                val (messageId, textFlow) = repository.sendMessageWithLLMStream(
                    context = getApplication(),
                    conversationId = conversationId,
                    userMessage = content
                )

                _streamingMessageId.value = messageId
                val fullContent = StringBuilder()

                // 收集文本流
                textFlow.collect { token ->
                    fullContent.append(token)
                    _streamingContent.value = fullContent.toString()

                    // 实时更新数据库
                    repository.updateMessageContent(messageId, fullContent.toString())
                }

                // 流式完成
                _streamingMessageId.value = null
                _isLoading.value = false
                streamingJob = null  // ✅ 清空 Job

                Log.d(TAG, "✅ 流式消息发送完成")

            } catch (e: kotlinx.coroutines.CancellationException) {
                // ✅ 用户主动停止
                Log.d(TAG, "🛑 用户停止了生成")
                _streamingMessageId.value = null
                _isLoading.value = false
                streamingJob = null
                _toastMessage.value = "已停止生成"

            } catch (e: Exception) {
                _isLoading.value = false
                _streamingMessageId.value = null
                streamingJob = null
                Log.e(TAG, "流式发送失败", e)
                _toastMessage.value = "发送失败: ${e.message}"
            }
        }
    }

    /**
     * ✅ 新增：停止流式生成
     */
    fun stopGeneration() {
        streamingJob?.cancel()
        streamingJob = null

        _streamingMessageId.value = null
        _isLoading.value = false

        Log.d(TAG, "🛑 已请求停止生成")
        _toastMessage.value = "正在停止..."
    }

    /**
     * 非流式发送（保留原有逻辑，可选使用）
     */
    fun sendMessage(content: String) {
        val conversationId = _currentConversationId.value
        if (conversationId == null) {
            _toastMessage.value = "请先选择或创建一个会话"
            return
        }

        if (content.isBlank()) return

        viewModelScope.launch {
            try {
                _isLoading.value = true

                repository.sendMessageWithLLM(
                    context = getApplication(),
                    conversationId = conversationId,
                    userMessage = content
                )

                _isLoading.value = false

            } catch (e: Exception) {
                _isLoading.value = false
                Log.e(TAG, "发送消息失败", e)
                _toastMessage.value = "发送失败: ${e.message}"
            }
        }
    }

    // 互动功能
    fun toggleLike(message: Message) {
        viewModelScope.launch {
            repository.toggleLike(message.id, message.isLiked)
        }
    }

    fun toggleFavorite(message: Message) {
        viewModelScope.launch {
            repository.toggleFavorite(message.id, message.isFavorited)
        }
    }

    companion object {
        private const val TAG = "ChatViewModel"
    }
}