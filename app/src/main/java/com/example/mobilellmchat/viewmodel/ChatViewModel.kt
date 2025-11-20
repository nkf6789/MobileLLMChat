package com.example.mobilellmchat.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.mobilellmchat.api.DouBaoApiService
import com.example.mobilellmchat.data.local.repository.ChatRepository
import com.example.mobilellmchat.data.local.entity.ConversationEntity
import com.example.mobilellmchat.model.ChatRequest
import com.example.mobilellmchat.model.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatViewModel(
    private val repository: ChatRepository,
    private val apiService: DouBaoApiService
) : ViewModel() {

    // 1. 会话列表 (Flow -> LiveData)
    val conversations: LiveData<List<ConversationEntity>> = repository.getAllConversations()
        .catch { e -> Log.e("ChatViewModel", "Error loading conversations", e) }
        .asLiveData()

    // 2. 当前选中的会话 ID
    private val _currentConversationId = MutableLiveData<Long>()
    val currentConversationId: LiveData<Long> get() = _currentConversationId

    // 3. 消息列表 (根据 conversationId 变化自动切换数据源)
    val messages: LiveData<List<Message>> = _currentConversationId.switchMap { id ->
        repository.getMessagesForConversation(id).asLiveData()
    }

    // 4. UI 状态
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _toastMessage = MutableLiveData<String>()
    val toastMessage: LiveData<String> get() = _toastMessage

    init {
        // 初始化时加载最近的会话，或者等待 Activity 指示
        Log.d("ChatViewModel", "ViewModel Initialized")
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
            // 简单的逻辑：如果删除了当前会话，重置当前ID（Activity应监听并处理）
            if (_currentConversationId.value == conversation.id) {
                // 逻辑可以是切换到第一条，或者置空
            }
        }
    }

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

                // 1. 保存用户消息到数据库
                repository.insertMessage(conversationId, content, "user")

                // 2. 获取历史记录 (数据库实体)
                val history = repository.getMessagesByConversationSync(conversationId)

                // 3. 【关键修复】将数据库实体转换为 API 模型
                val apiMessages = history.map { dbMsg ->
                    com.example.mobilellmchat.model.ApiMessage(
                        role = dbMsg.role,
                        content = dbMsg.content
                    )
                }

                // 4. 构造请求
                val request = ChatRequest(
                    model = "doubao-seed-1-6-flash-250828",
                    messages = apiMessages // 现在类型匹配了
                )

                // 5. 调用 API
                val response = apiService.sendMessage(request)

                val aiContent = response.choices.firstOrNull()?.message?.content
                    ?: "无回复内容"

                repository.insertMessage(conversationId, aiContent, "assistant")

                // 更新会话时间
                val currentConv = repository.getConversationById(conversationId)
                currentConv?.let { repository.updateConversation(it) }

            } catch (e: Exception) {
                Log.e("ChatViewModel", "Send failed", e)
                _toastMessage.value = "发送失败: ${e.message}"
            } finally {
                _isLoading.value = false
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
}
