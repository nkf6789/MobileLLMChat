package com.example.mobilellmchat.model

data class ChatRequest(
    val model: String,              // 模型 ID
    val messages: List<Message>,    // 对话历史
    val stream: Boolean = false     // 是否流式输出
)
