package com.example.mobilellmchat.model

data class Message(
    val content: String,      // 消息内容
    val role: String,         // "user" 或 "assistant"
    val timestamp: Long = System.currentTimeMillis()
) {
    // 计算属性：用于适配器判断消息类型
    val isUser: Boolean
        get() = role == "user"
}
