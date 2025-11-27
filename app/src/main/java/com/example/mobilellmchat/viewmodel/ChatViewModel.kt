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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * ChatViewModel
 *
 * [MODIFIED] 使用 Repository 的 sendMessageWithLLM 方法
 *
 * @author AI-Assisted (Modified)
 * @since Sprint 1
 */
class ChatViewModel(
    application: Application,
    private val repository: ChatRepository
) : AndroidViewModel(application) {

    // 1. 会话列表
    val conversations: LiveData<List<ConversationEntity>> = repository.getAllConversations()
        .catch { e -> Log.e("ChatViewModel", "Error loading conversations", e) }
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

    init {
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
            if (_currentConversationId.value == conversation.id) {
                // 删除当前会话后可以重置 ID
            }
        }
    }

    /**
     * [MODIFIED] 发送消息 - 使用新的 Repository 方法
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

                // ✅ 调用 Repository 的新方法（内部使用 LLMService）
                repository.sendMessageWithLLM(
                    context = getApplication(),
                    conversationId = conversationId,
                    userMessage = content
                )

                _isLoading.value = false

            } catch (e: Exception) {
                _isLoading.value = false
                Log.e("ChatViewModel", "发送消息失败", e)
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
}
