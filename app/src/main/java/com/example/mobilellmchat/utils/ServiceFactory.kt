package com.example.mobilellmchat.utils

import android.content.Context
import android.util.Log
import com.example.mobilellmchat.model.LLMService
import com.example.mobilellmchat.model.ModelConfig
import com.example.mobilellmchat.model.ModelType
import com.example.mobilellmchat.network.RemoteLLMService
import com.example.mobilellmchat.network.RetrofitClient

/**
 * LLM 服务工厂类
 *
 * @author AI-Assisted
 * @since Sprint 1
 */
object ServiceFactory {

    private const val TAG = "ServiceFactory"

    /**
     * 创建 LLM 服务实例
     */
    fun createLLMService(context: Context): LLMService {
        val prefs = AppPreferences(context)

        return when (prefs.modelType) {
            ModelType.REMOTE -> {
                Log.d(TAG, "创建远端服务：豆包 API")
                createRemoteService()
            }

            ModelType.LOCAL -> {
                Log.d(TAG, "创建本地服务：${prefs.localModelName}")
                createLocalService(context, prefs)
                    ?: run {
                        Log.w(TAG, "本地模型不可用，回退到远端服务")
                        prefs.modelType = ModelType.REMOTE
                        createRemoteService()
                    }
            }
        }
    }

    /**
     * 创建远端 LLM 服务
     */
    private fun createRemoteService(): LLMService {
        return RemoteLLMService(
            apiService = RetrofitClient.douBaoApi
        )
    }

    /**
     * 创建本地 LLM 服务（Sprint 4 实现）
     */
    private fun createLocalService(context: Context, prefs: AppPreferences): LLMService? {
        val modelName = prefs.localModelName

        if (!ModelFileManager.isModelDownloaded(context, modelName)) {
            Log.e(TAG, "模型文件不存在: $modelName")
            return null
        }

        val modelPath = ModelFileManager.getModelPath(context, modelName)

        val config = ModelConfig(
            modelPath = modelPath,
            backend = prefs.computeBackend,
            threads = prefs.cpuThreads,
            temperature = prefs.temperature,
            topP = prefs.topP,
            maxTokens = prefs.maxTokens,
            contextLength = prefs.contextLength
        )

        Log.d(TAG, "本地模型配置: $config")

        // ⚠️ Sprint 4 实现
        Log.w(TAG, "LocalLLMService 尚未实现")
        return null
    }

    /**
     * 创建特定类型的服务
     */
    fun createServiceByType(context: Context, modelType: ModelType): LLMService {
        val prefs = AppPreferences(context)
        prefs.modelType = modelType
        return createLLMService(context)
    }

    /**
     * 验证本地模型是否可用
     */
    fun isLocalModelAvailable(context: Context): Boolean {
        val prefs = AppPreferences(context)
        return ModelFileManager.isModelDownloaded(context, prefs.localModelName)
    }

    /**
     * 获取当前模型信息
     */
    fun getCurrentModelInfo(context: Context): String {
        val prefs = AppPreferences(context)
        return when (prefs.modelType) {
            ModelType.REMOTE -> "云端模型 (豆包)"
            ModelType.LOCAL -> {
                if (isLocalModelAvailable(context)) {
                    "本地模型 (${prefs.localModelName})"
                } else {
                    "本地模型 (未下载)"
                }
            }
        }
    }
}
