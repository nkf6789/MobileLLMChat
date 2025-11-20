package com.example.mobilellmchat.model

data class ChatRequest(
    val model: String,
    val messages: List<ApiMessage>, // 这里明确指定使用 ApiMessage
    val stream: Boolean = false
)
