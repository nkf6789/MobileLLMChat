package com.example.mobilellmchat.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.mobilellmchat.data.local.AppDatabase
import com.example.mobilellmchat.data.local.entity.ConversationEntity
import com.example.mobilellmchat.data.local.repository.ChatRepository
import com.example.mobilellmchat.model.ChatRequest
import com.example.mobilellmchat.model.Message
import com.example.mobilellmchat.utils.Constants
import com.example.mobilellmchat.utils.RetrofitClient
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ChatRepository

    // 当前会话ID
    private val _currentConversationId = MutableLiveData<Long?>()
    val currentConversationId: LiveData<Long?> = _currentConversationId

    // 所有会话列表
    private val _conversations = MutableLiveData<List<ConversationEntity>>()
    val conversations: LiveData<List<ConversationEntity>> = _conversations

    // 当前会话的消息列表
    private val _messages = MutableLiveData<List<Message>>(emptyList())
    val messages: LiveData<List<Message>> = _messages

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ChatRepository(database)

        // 加载所有会话
        loadConversations()
    }

    // ========== 会话管理 ==========

    private fun loadConversations() {
        viewModelScope.launch {
            repository.getAllConversations().collectLatest { list ->
                _conversations.value = list

                // 如果没有当前会话且有会话列表,自动选择第一个
                if (_currentConversationId.value == null && list.isNotEmpty()) {
                    switchConversation(list.first().id)
                }
            }
        }
    }

    fun createNewConversation(title: String = "新对话") {
        viewModelScope.launch {
            val conversationId = repository.createConversation(title)
            switchConversation(conversationId)
        }
    }

    fun switchConversation(conversationId: Long) {
        _currentConversationId.value = conversationId
        viewModelScope.launch {
            // 加载该会话的历史消息
            repository.getMessagesByConversation(conversationId).collectLatest { messageList ->
                _messages.value = messageList
            }
        }
    }

    fun deleteConversation(conversationId: Long) {
        viewModelScope.launch {
            repository.deleteConversation(conversationId)

            // 如果删除的是当前会话,切换到其他会话
            if (_currentConversationId.value == conversationId) {
                val remainingConversations = _conversations.value?.filterNot { it.id == conversationId }
                if (!remainingConversations.isNullOrEmpty()) {
                    switchConversation(remainingConversations.first().id)
                } else {
                    _currentConversationId.value = null
                    _messages.value = emptyList()
                }
            }
        }
    }

    fun renameConversation(conversationId: Long, newTitle: String) {
        viewModelScope.launch {
            val conversation = repository.getConversationById(conversationId)
            conversation?.let {
                repository.updateConversation(it.copy(title = newTitle))
            }
        }
    }

    // ========== 消息发送 ==========

    fun sendMessage(userMessage: String) {
        if (userMessage.isBlank()) return

        val conversationId = _currentConversationId.value

        // 如果没有当前会话,先创建一个
        if (conversationId == null) {
            viewModelScope.launch {
                val newConversationId = repository.createConversation("新对话")
                _currentConversationId.value = newConversationId
                sendMessageToConversation(newConversationId, userMessage)
            }
        } else {
            sendMessageToConversation(conversationId, userMessage)
        }
    }

    private fun sendMessageToConversation(conversationId: Long, userMessage: String) {
        viewModelScope.launch {
            // 创建用户消息
            val userMsg = Message(role = "user", content = userMessage)

            // 保存用户消息到数据库
            repository.saveMessage(conversationId, userMsg)

            // ❌ 删除这3行手动更新的代码
            // val currentMessages = _messages.value.orEmpty().toMutableList()
            // currentMessages.add(userMsg)
            // _messages.value = currentMessages

            // 显示加载状态
            _isLoading.value = true

            try {
                // ✅ 从数据库获取最新消息（包含刚保存的用户消息）
                val currentMessages = repository.getMessagesByConversationSync(conversationId)

                // 准备请求(包含对话历史)
                val request = ChatRequest(
                    model = Constants.MODEL_ENDPOINT,
                    messages = currentMessages,
                    stream = false
                )

                // 发送请求
                val response = RetrofitClient.douBaoApi.sendMessage(request)

                // 获取回复
                val aiReply = response.choices.firstOrNull()?.message?.content
                    ?: "抱歉,我没有收到回复。"

                val aiMsg = Message(role = "assistant", content = aiReply)

                // 保存AI回复到数据库（Flow会自动触发UI更新）
                repository.saveMessage(conversationId, aiMsg)

            } catch (e: Exception) {
                // 处理错误
                val errorMsg = Message(
                    role = "assistant",
                    content = "抱歉,发生了错误:${e.message}"
                )
                repository.saveMessage(conversationId, errorMsg)
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun clearMessages() {
        val conversationId = _currentConversationId.value ?: return
        viewModelScope.launch {
            repository.clearConversationMessages(conversationId)
        }
    }
}
