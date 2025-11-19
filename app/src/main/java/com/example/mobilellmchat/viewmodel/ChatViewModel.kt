package com.example.mobilellmchat.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobilellmchat.model.ChatRequest
import com.example.mobilellmchat.model.Message
import com.example.mobilellmchat.utils.Constants
import com.example.mobilellmchat.utils.RetrofitClient
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    private val _messages = MutableLiveData<List<Message>>(emptyList())
    val messages: LiveData<List<Message>> = _messages

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    fun sendMessage(userMessage: String) {
        if (userMessage.isBlank()) return

        // 添加用户消息
        val currentMessages = _messages.value.orEmpty().toMutableList()
        currentMessages.add(Message(role = "user", content = userMessage))
        _messages.value = currentMessages

        // 显示加载状态
        _isLoading.value = true

        // 调用豆包 API
        viewModelScope.launch {
            try {
                // 准备请求（包含对话历史）
                val request = ChatRequest(
                    model = Constants.MODEL_ENDPOINT,
                    messages = currentMessages,
                    stream = false
                )

                // 发送请求
                val response = RetrofitClient.douBaoApi.sendMessage(request)

                // 获取回复
                val aiReply = response.choices.firstOrNull()?.message?.content
                    ?: "抱歉，我没有收到回复。"

                // 添加 AI 回复
                val updatedMessages = _messages.value.orEmpty().toMutableList()
                updatedMessages.add(Message(role = "assistant", content = aiReply))
                _messages.value = updatedMessages

            } catch (e: Exception) {
                // 处理错误
                val errorMessages = _messages.value.orEmpty().toMutableList()
                errorMessages.add(
                    Message(
                        role = "assistant",
                        content = "抱歉，发生了错误：${e.message}"
                    )
                )
                _messages.value = errorMessages
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearMessages() {
        _messages.value = emptyList()
    }
}
