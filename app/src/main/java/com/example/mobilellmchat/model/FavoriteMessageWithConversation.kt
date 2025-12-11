package com.example.mobilellmchat.model

import androidx.room.ColumnInfo

/**
 * 收藏消息数据类（包含会话信息）
 * 用于收藏页面显示
 */
data class FavoriteMessageWithConversation(
    @ColumnInfo(name = "id")
    val messageId: Long,

    @ColumnInfo(name = "content")
    val messageContent: String,

    @ColumnInfo(name = "timestamp")
    val messageTimestamp: Long,

    @ColumnInfo(name = "conversationId")
    val conversationId: Long,

    @ColumnInfo(name = "conversationTitle")
    val conversationTitle: String
)