package com.example.mobilellmchat.service

import android.content.Context
import android.util.Log
import com.example.mobilellmchat.llm.LlamaWrapper
import com.example.mobilellmchat.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * 本地 LLM 推理服务
 *
 * [MODIFIED] 新增流式输出支持
 *
 * @author AI-Assisted (Modified)
 * @since Sprint 1 (Updated Sprint 2)
 */
class LocalLLMService(
    private val context: Context,
    private val modelPath: String,
    private val computeBackend: ComputeBackend,
    private val cpuThreads: Int,
    private val temperature: Float,
    private val maxTokens: Int
) : LLMService {

    companion object {
        private const val TAG = "LocalLLMService"
    }

    private val llamaWrapper = LlamaWrapper(context)
    private var isInitialized = false

    init {
        Log.d(TAG, "📦 LocalLLMService 初始化")
        Log.d(TAG, "   模型路径: $modelPath")
        Log.d(TAG, "   计算后端: $computeBackend")
        Log.d(TAG, "   CPU线程: $cpuThreads")
        Log.d(TAG, "   温度: $temperature")
        Log.d(TAG, "   最大Token: $maxTokens")

        loadModelAsync()
    }

    private fun loadModelAsync() {
        Thread {
            try {
                Log.d(TAG, "开始加载模型...")
                isInitialized = llamaWrapper.loadModel(
                    modelPath = modelPath,
                    cpuThreads = cpuThreads
                )
                if (isInitialized) {
                    Log.d(TAG, "✅ 模型加载成功")
                } else {
                    Log.e(TAG, "❌ 模型加载失败")
                }
            } catch (e: Exception) {
                Log.e(TAG, "模型加载异常", e)
                isInitialized = false
            }
        }.start()
    }

    /**
     * 非流式聊天（保持原有逻辑）
     */
    override suspend fun chat(messages: List<ApiMessage>): String = withContext(Dispatchers.IO) {
        if (!isInitialized) {
            throw IllegalStateException("模型未加载或加载失败，请检查模型文件")
        }

        try {
            val prompt = buildPrompt(messages)
            Log.d(TAG, "🔍 开始推理")

            val response = llamaWrapper.generate(
                prompt = prompt,
                temperature = temperature,
                maxTokens = maxTokens
            )

            Log.d(TAG, "✅ 推理完成，响应长度: ${response.length}")
            response

        } catch (e: Exception) {
            Log.e(TAG, "❌ 推理失败", e)
            throw Exception("本地推理失败: ${e.message}")
        }
    }

    /**
     * ✅ 新增：流式聊天方法
     */
    override suspend fun chatStream(messages: List<ApiMessage>): Flow<String> = flow {
        if (!isInitialized) {
            throw IllegalStateException("模型未加载或加载失败，请检查模型文件")
        }

        try {
            val prompt = buildPrompt(messages)
            Log.d(TAG, "🌊 开始流式推理")

            // LlamaWrapper.generateStream() 返回 Iterable<String>
            for (token in llamaWrapper.generateStream(prompt, temperature, maxTokens)) {
                Log.d(TAG, "📝 生成 token: $token")
                emit(token)  // 逐个发送 token
            }

            Log.d(TAG, "✅ 流式推理完成")

        } catch (e: Exception) {
            Log.e(TAG, "❌ 流式推理失败", e)
            throw Exception("流式推理失败: ${e.message}")
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 构建 Prompt（自动识别模型格式）
     */
    private fun buildPrompt(messages: List<ApiMessage>): String {
        val modelName = modelPath.substringAfterLast("/").lowercase()

        return if (modelName.contains("qwen")) {
            buildQwen2Prompt(messages)
        } else {
            buildTinyLlamaPrompt(messages)
        }
    }

    /**
     * TinyLlama 格式（Zephyr）
     */
    private fun buildTinyLlamaPrompt(messages: List<ApiMessage>): String {
        val sb = StringBuilder()

        val systemMessage = messages.firstOrNull { it.role == "system" }
        if (systemMessage != null) {
            sb.append("<|system|>\n")
            sb.append(systemMessage.content)
            sb.append("</s>\n")
        } else {
            sb.append("<|system|>\n")
            sb.append("You are a helpful assistant.")
            sb.append("</s>\n")
        }

        messages.filter { it.role != "system" }.forEach { msg ->
            sb.append("<|${msg.role}|>\n")
            sb.append(msg.content)
            sb.append("</s>\n")
        }

        sb.append("<|assistant|>\n")
        return sb.toString()
    }

    /**
     * Qwen2 格式（ChatML）
     */
    private fun buildQwen2Prompt(messages: List<ApiMessage>): String {
        val sb = StringBuilder()

        val systemMessage = messages.firstOrNull { it.role == "system" }
        if (systemMessage != null) {
            sb.append("<|im_start|>system\n")
            sb.append(systemMessage.content)
            sb.append("<|im_end|>\n")
        } else {
            sb.append("<|im_start|>system\n")
            sb.append("You are a helpful assistant.")
            sb.append("<|im_end|>\n")
        }

        messages.filter { it.role != "system" }.forEach { msg ->
            sb.append("<|im_start|>${msg.role}\n")
            sb.append(msg.content)
            sb.append("<|im_end|>\n")
        }

        sb.append("<|im_start|>assistant\n")
        return sb.toString()
    }

    override fun getModelInfo(): ModelInfo {
        return ModelInfo(
            name = modelPath.substringAfterLast("/"),
            type = "本地模型 (llama.cpp)",
            backend = computeBackend.name,
            status = if (isInitialized) "已加载" else "加载中/失败"
        )
    }

    override fun stopGeneration() {
        llamaWrapper.stopGeneration()
    }

    override fun release() {
        llamaWrapper.release()
        isInitialized = false
        Log.d(TAG, "✅ LocalLLMService 资源已释放")
    }
}