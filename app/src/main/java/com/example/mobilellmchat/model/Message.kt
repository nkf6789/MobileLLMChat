package com.example.mobilellmchat.model

/**
 * 消息数据类
 *
 * [MODIFIED] 新增 isStreaming 字段支持流式更新
 *
 * @author AI-Assisted (Modified)
 * @since Sprint 1 (Updated Sprint 2)
 */
data class Message(
    val id: Long,
    val conversationId: Long,
    val role: String,
    val content: String,
    val timestamp: Long,
    val isLiked: Boolean,
    val isFavorited: Boolean,
    val isStreaming: Boolean = false  // ✅ 新增：标记是否正在流式输出
)