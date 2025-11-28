package com.example.mobilellmchat.service

import com.example.mobilellmchat.model.ApiMessage
import com.example.mobilellmchat.model.ComputeBackend
import com.example.mobilellmchat.model.LLMService
import com.example.mobilellmchat.model.ModelInfo

/**
 * 本地 LLM 推理服务（骨架实现）
 *
 * ⚠️ 当前状态：占位实现
 * 📋 TODO：集成 llama.cpp 的 JNI 绑定
 */
class LocalLLMService(
    private val modelPath: String,
    private val computeBackend: ComputeBackend,
    private val cpuThreads: Int,
    private val temperature: Float,
    private val maxTokens: Int
) : LLMService {

    private var isModelLoaded = false

    init {
        println("📦 LocalLLMService 初始化")
        println("   模型路径: $modelPath")
        println("   计算后端: $computeBackend")
        println("   CPU线程: $cpuThreads")
        println("   温度: $temperature")
        println("   最大Token: $maxTokens")
    }

    override suspend fun chat(messages: List<ApiMessage>): String {
        // ⚠️ 占位实现
        val prompt = messages.lastOrNull()?.content ?: "无输入"

        return """
        🤖 [本地模型占位回复]
        
        你的问题: $prompt
        
        ⚠️ 本地推理功能尚未实现
        
        📋 需要完成的步骤:
        1. 集成 llama.cpp Android 版本
        2. 编写 JNI 绑定代码
        3. 编译 native 库
        4. 实现真实推理逻辑
        
        📊 当前配置:
        - 模型: ${modelPath.substringAfterLast("/")}
        - 后端: $computeBackend
        - 线程: $cpuThreads
        - 温度: $temperature
        """.trimIndent()
    }

    override fun getModelInfo(): ModelInfo {
        return ModelInfo(
            name = modelPath.substringAfterLast("/"),
            type = "本地模型（占位）",
            backend = computeBackend.name,
            status = if (isModelLoaded) "已加载" else "未加载"
        )
    }

    override fun stopGeneration() {
        println("⚠️ LocalLLMService: stopGeneration() 未实现")
    }

    override fun release() {
        isModelLoaded = false
        println("ℹ️ LocalLLMService: 资源已释放")
    }
}
