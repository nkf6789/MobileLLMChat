package com.example.mobilellmchat.model

data class Message(
    val role: String,      // "user" 或 "assistant"
    val content: String    // 消息内容
) {
    // 辅助属性，用于界面显示
    val isUser: Boolean
        get() = role == "user"
}
