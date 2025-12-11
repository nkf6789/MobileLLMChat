package com.example.mobilellmchat.model

data class Message(
    val id: Long,
    val conversationId: Long,  // ✅ 新增：所属会话ID
    val role: String,
    val content: String,
    val timestamp: Long,
    val isLiked: Boolean,
    val isFavorited: Boolean
)