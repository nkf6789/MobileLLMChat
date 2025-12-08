package com.example.mobilellmchat.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 管理Assets中的预置模型
 * 负责首次运行时将模型从assets复制到内部存储
 */
object ModelAssetManager {

    private const val TAG = "ModelAssetManager"

    // ✅ 预置模型配置
    private const val ASSET_MODEL_NAME = "tinyllama-1.1b-chat-q4_0.gguf"
    private const val ASSET_MODEL_PATH = "models/$ASSET_MODEL_NAME"

    /**
     * 检查并复制预置模型（如果尚未复制）
     *
     * @return 模型的最终路径，如果失败返回null
     */
    suspend fun ensurePreinstalledModel(context: Context): String? = withContext(Dispatchers.IO) {
        try {
            val fileManager = ModelFileManager.getInstance(context)
            val targetFile = fileManager.getModelFile(ASSET_MODEL_NAME)

            // 如果模型已存在，直接返回路径
            if (targetFile.exists() && targetFile.length() > 0) {
                Log.d(TAG, "✅ 预置模型已存在: ${targetFile.absolutePath}")
                return@withContext targetFile.absolutePath
            }

            Log.d(TAG, "📦 开始复制预置模型...")

            // 从assets复制到内部存储
            context.assets.open(ASSET_MODEL_PATH).use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalBytes = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytes += bytesRead

                        // 每10MB打印一次进度
                        if (totalBytes % (10 * 1024 * 1024) == 0L) {
                            Log.d(TAG, "📋 已复制: ${totalBytes / (1024 * 1024)} MB")
                        }
                    }

                    Log.d(TAG, "✅ 预置模型复制完成: ${totalBytes / (1024 * 1024)} MB")
                }
            }

            targetFile.absolutePath

        } catch (e: Exception) {
            Log.e(TAG, "❌ 复制预置模型失败", e)
            null
        }
    }

    /**
     * 获取预置模型名称（供UI显示）
     */
    fun getPreinstalledModelName(): String = ASSET_MODEL_NAME

    /**
     * 检查预置模型是否已准备好
     */
    fun isPreinstalledModelReady(context: Context): Boolean {
        val fileManager = ModelFileManager.getInstance(context)
        val targetFile = fileManager.getModelFile(ASSET_MODEL_NAME)
        return targetFile.exists() && targetFile.length() > 0
    }
}