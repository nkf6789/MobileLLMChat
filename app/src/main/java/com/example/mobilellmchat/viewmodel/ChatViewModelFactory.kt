package com.example.mobilellmchat.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.mobilellmchat.data.local.repository.ChatRepository

/**
 * ChatViewModelFactory
 *
 * [MODIFIED] 移除 apiService 参数，使用 ServiceFactory
 *
 * @author AI-Assisted (Modified)
 * @since Sprint 1
 */
class ChatViewModelFactory(
    private val application: Application,
    private val repository: ChatRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            // ✅ 初始化 LLM 服务
            repository.initialize(application)

            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
