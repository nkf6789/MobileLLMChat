package com.example.mobilellmchat.model

/**
 * LLM 服务抽象接口
 *
 * 定义大模型服务的统一调用规范，支持：
 * - 远端 API 服务（豆包）
 * - 本地端侧模型（llama.cpp）
 *
 * 职责：
 * 1. 发送对话消息并获取回复
 * 2. 提供模型信息查询
 * 3. 支持生成控制（停止）
 *
 * @author AI-Assisted
 * @since Sprint 1
 */
interface LLMService {

    /**
     * 发送聊天消息并获取 AI 回复
     *
     * @param messages 对话历史上下文（包含用户和AI的历史消息）
     * @return AI 生成的回复文本
     * @throws Exception 网络错误、模型加载失败等异常
     *
     * 示例：
     * ```kotlin
     * val messages = listOf(
     *     ApiMessage(role = "user", content = "你好")
     * )
     * val reply = llmService.chat(messages)
     * // reply: "你好！有什么我可以帮助你的吗？"
     * ```
     */
    suspend fun chat(messages: List<ApiMessage>): String

    /**
     * 获取当前模型信息
     *
     * @return 模型配置和状态信息
     *
     * 用途：
     * - UI 展示当前使用的模型
     * - 调试时查看配置
     */
    fun getModelInfo(): ModelInfo

    /**
     * 停止当前正在进行的生成任务（可选实现）
     *
     * 使用场景：
     * - 用户点击"停止生成"按钮
     * - 切换到其他对话
     *
     * 注意：
     * - 远端 API 可能不支持中途停止
     * - 本地模型可以立即终止推理
     */
    fun stopGeneration() {
        // 默认空实现，子类可选择性重写
    }

    /**
     * 释放资源（可选实现）
     *
     * 使用场景：
     * - 应用退出时
     * - 切换模型前
     *
     * 注意：
     * - 本地模型需要释放加载的模型文件
     * - 远端服务通常不需要
     */
    fun release() {
        // 默认空实现
    }
}
