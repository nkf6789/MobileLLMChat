package com.example.mobilellmchat.utils

import android.content.Context
import android.util.Log
import com.example.mobilellmchat.model.ComputeBackend
import com.example.mobilellmchat.model.ModelType
import com.example.mobilellmchat.model.LLMService
import com.example.mobilellmchat.service.LocalLLMService
import com.example.mobilellmchat.network.RemoteLLMService
import kotlinx.coroutines.runBlocking

object ServiceFactory {

    private const val TAG = "ServiceFactory"

    fun createLLMService(context: Context): LLMService {
        val preferences = AppPreferences(context)

        return when (preferences.modelType) {
            ModelType.REMOTE -> {
                val apiService = RetrofitClient.create(preferences)
                RemoteLLMService(apiService)
            }
            ModelType.LOCAL -> {
                // ✅ 首次运行时自动复制预置模型
                ensurePreinstalledModel(context)

                val modelPath = preferences.localModelPath
                val fileManager = ModelFileManager.getInstance(context)

                // ✅ 如果没有设置模型路径，使用预置模型
                val actualModelPath = if (modelPath.isEmpty()) {
                    val preinstalledModel = ModelAssetManager.getPreinstalledModelName()
                    fileManager.getModelPath(preinstalledModel)
                } else {
                    modelPath
                }

                val modelName = actualModelPath.substringAfterLast("/")

                if (!fileManager.isModelDownloaded(modelName)) {
                    throw IllegalStateException("本地模型未下载: $modelName")
                }

                // ✅ 传递 context 参数
                LocalLLMService(
                    context = context,
                    modelPath = actualModelPath,
                    computeBackend = preferences.computeBackend,
                    cpuThreads = preferences.cpuThreads,
                    temperature = preferences.temperature,
                    maxTokens = preferences.maxTokens
                )
            }
        }
    }

    /**
     * ✅ 确保预置模型已复制（同步方式）
     */
    private fun ensurePreinstalledModel(context: Context) {
        if (!ModelAssetManager.isPreinstalledModelReady(context)) {
            Log.d(TAG, "📦 预置模型未准备好，开始复制...")
            runBlocking {
                ModelAssetManager.ensurePreinstalledModel(context)
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
                // ✅ 首次运行时确保预置模型可用
                ensurePreinstalledModel(context)

                var modelPath = preferences.localModelPath

                // ✅ 如果没有配置，使用预置模型
                if (modelPath.isEmpty()) {
                    val preinstalledModel = ModelAssetManager.getPreinstalledModelName()
                    val fileManager = ModelFileManager.getInstance(context)
                    modelPath = fileManager.getModelPath(preinstalledModel)

                    // 自动设置为默认模型
                    preferences.localModelPath = modelPath
                }

                val modelName = modelPath.substringAfterLast("/")
                when {
                    !ModelFileManager.getInstance(context)
                        .isModelDownloaded(modelName) ->
                        "本地模型文件不存在: $modelName"
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

                val modelPath = preferences.localModelPath.ifEmpty {
                    val preinstalledModel = ModelAssetManager.getPreinstalledModelName()
                    ModelFileManager.getInstance(context).getModelPath(preinstalledModel)
                }

                """
                模式: 本地模型
                模型: ${modelPath.substringAfterLast("/")}
                后端: $backend
                线程数: ${preferences.cpuThreads}
                温度: ${preferences.temperature}
                """.trimIndent()
            }
        }
    }
}