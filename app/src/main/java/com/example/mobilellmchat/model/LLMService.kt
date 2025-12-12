package com.example.mobilellmchat.model

import kotlinx.coroutines.flow.Flow

/**
 * LLM 服务接口
 *
 * [MODIFIED] 新增流式输出支持
 *
 * @author AI-Assisted (Modified)
 * @since Sprint 1 (Updated Sprint 2)
 */
interface LLMService {

    /**
     * 发送聊天消息（非流式）
     */
    suspend fun chat(messages: List<ApiMessage>): String

    /**
     * ✅ 新增：流式聊天接口
     *
     * @param messages 对话历史
     * @return Flow<String> 逐字返回的文本流
     */
    suspend fun chatStream(messages: List<ApiMessage>): Flow<String>

    /**
     * 获取模型信息
     */
    fun getModelInfo(): ModelInfo

    /**
     * 停止生成
     */
    fun stopGeneration()

    /**
     * 释放资源（仅本地模型需要）
     */
    fun release() {
        // 默认空实现
    }
}