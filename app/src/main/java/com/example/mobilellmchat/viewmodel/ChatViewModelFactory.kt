package com.example.mobilellmchat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.mobilellmchat.api.DouBaoApiService
import com.example.mobilellmchat.data.local.repository.ChatRepository

class ChatViewModelFactory(
    private val repository: ChatRepository,
    private val apiService: DouBaoApiService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(repository, apiService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
