package com.example.mobilellmchat.model

import com.google.gson.annotations.SerializedName

/**
 * 流式响应事件数据类
 *
 * 用于解析豆包 API 的 Server-Sent Events (SSE) 响应
 *
 * @author AI-Assisted
 * @since Sprint 2 - Stream Support
 */

/**
 * SSE 事件包装
 */
data class StreamEvent(
    val type: String,                    // 事件类型
    val delta: String? = null,           // 文本增量
    @SerializedName("item_id")
    val itemId: String? = null,
    @SerializedName("output_index")
    val outputIndex: Int? = null,
    @SerializedName("content_index")
    val contentIndex: Int? = null,
    @SerializedName("sequence_number")
    val sequenceNumber: Int? = null
)

/**
 * 流式响应块（完整的 data: {...} 格式）
 */
data class StreamChunk(
    val choices: List<StreamChoice>? = null,
    val created: Long? = null,
    val id: String? = null,
    val model: String? = null,
    @SerializedName("service_tier")
    val serviceTier: String? = null,
    @SerializedName("object")
    val objectType: String? = null
)

data class StreamChoice(
    val delta: StreamDelta? = null,
    val index: Int = 0,
    @SerializedName("finish_reason")
    val finishReason: String? = null
)

data class StreamDelta(
    val content: String? = null,           // 实际文本内容
    @SerializedName("reasoning_content")
    val reasoningContent: String? = null,  // 思维链内容（可忽略）
    val role: String? = null
)