package com.example.mobilellmchat.utils

import android.content.Context
import com.example.mobilellmchat.model.ComputeBackend
import com.example.mobilellmchat.model.ModelType
import com.example.mobilellmchat.model.LLMService
import com.example.mobilellmchat.service.LocalLLMService
import com.example.mobilellmchat.network.RemoteLLMService  // ✅ 确保导入正确

object ServiceFactory {

    fun createLLMService(context: Context): LLMService {
        val preferences = AppPreferences(context)

        return when (preferences.modelType) {
            ModelType.REMOTE -> {
                // ✅ 修复：先创建 DouBaoApiService，再传给 RemoteLLMService
                val apiService = RetrofitClient.create(preferences)
                RemoteLLMService(apiService)
            }
            ModelType.LOCAL -> {
                val modelName = preferences.localModelPath.substringAfterLast("/")
                val fileManager = ModelFileManager.getInstance(context)

                if (!fileManager.isModelDownloaded(modelName)) {
                    throw IllegalStateException("本地模型未下载: $modelName")
                }

                LocalLLMService(
                    modelPath = fileManager.getModelPath(modelName),
                    computeBackend = preferences.computeBackend,
                    cpuThreads = preferences.cpuThreads,
                    temperature = preferences.temperature,
                    maxTokens = preferences.maxTokens
                )
            }
        }
    }

    fun validateServiceConfig(context: Context): String? {
        val preferences = AppPreferences(context)

        return when (preferences.modelType) {
            ModelType.REMOTE -> {
                when {
                    preferences.apiKey.isEmpty() -> "未配置 API Key"
                    preferences.baseUrl.isEmpty() -> "未配置 Base URL"
                    preferences.modelName.isEmpty() -> "未配置模型名称"
                    else -> null
                }
            }
            ModelType.LOCAL -> {
                val modelPath = preferences.localModelPath
                when {
                    modelPath.isEmpty() -> "未选择本地模型"
                    !ModelFileManager.getInstance(context)
                        .isModelDownloaded(modelPath.substringAfterLast("/")) ->
                        "本地模型文件不存在"
                    else -> null
                }
            }
        }
    }

    fun getConfigSummary(context: Context): String {
        val preferences = AppPreferences(context)

        return when (preferences.modelType) {
            ModelType.REMOTE -> {
                """
                模式: 云端模型
                模型: ${preferences.modelName}
                URL: ${preferences.baseUrl}
                """.trimIndent()
            }
            ModelType.LOCAL -> {
                val backend = when (preferences.computeBackend) {
                    ComputeBackend.CPU -> "CPU"
                    ComputeBackend.VULKAN -> "GPU (Vulkan)"
                    ComputeBackend.AUTO -> "自动选择"
                }
                """
                模式: 本地模型
                模型: ${preferences.localModelPath.substringAfterLast("/")}
                后端: $backend
                线程数: ${preferences.cpuThreads}
                温度: ${preferences.temperature}
                """.trimIndent()
            }
        }
    }
}
