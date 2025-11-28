package com.example.mobilellmchat.utils

import android.content.Context
import android.util.Log
import java.io.File

/**
 * 模型文件管理器
 * 负责管理模型文件的存储、下载和清理
 */
class ModelFileManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "ModelFileManager"
        private const val MODELS_DIR = "models"
        private const val TEMP_DOWNLOAD_SUFFIX = ".download"

        @Volatile
        private var instance: ModelFileManager? = null

        fun getInstance(context: Context): ModelFileManager {
            return instance ?: synchronized(this) {
                instance ?: ModelFileManager(context.applicationContext).also {
                    instance = it
                }
            }
        }

        // ==================== 静态方法（供 ServiceFactory 使用）====================

        /**
         * 检查模型是否已下载（静态方法）
         */
        @JvmStatic
        fun isModelDownloaded(context: Context, modelName: String): Boolean {
            return getInstance(context).isModelDownloaded(modelName)
        }

        /**
         * 获取模型路径（静态方法）
         */
        @JvmStatic
        fun getModelPath(context: Context, modelName: String): String {
            return getInstance(context).getModelFile(modelName).absolutePath
        }
    }

    /**
     * 获取模型存储目录
     */
    fun getModelDirectory(): File {
        val modelDir = File(context.filesDir, MODELS_DIR)
        if (!modelDir.exists()) {
            modelDir.mkdirs()
        }
        return modelDir
    }

    /**
     * 获取临时下载文件
     */
    fun getTempDownloadFile(modelName: String): File {
        return File(getModelDirectory(), "$modelName$TEMP_DOWNLOAD_SUFFIX")
    }

    /**
     * 获取最终模型文件
     */
    fun getModelFile(modelName: String): File {
        return File(getModelDirectory(), modelName)
    }

    /**
     * 检查模型是否已下载
     */
    fun isModelDownloaded(modelName: String): Boolean {
        val modelFile = getModelFile(modelName)
        return modelFile.exists() && modelFile.length() > 0
    }

    /**
     * 获取模型完整路径（实例方法，供 SettingsActivity 使用）
     */
    fun getModelPath(modelName: String): String {
        return getModelFile(modelName).absolutePath
    }

    /**
     * 获取已下载的模型列表
     */
    fun getDownloadedModels(): List<ModelFileInfo> {
        val modelDir = getModelDirectory()
        return modelDir.listFiles()
            ?.filter { it.isFile && it.extension == "gguf" && !it.name.endsWith(TEMP_DOWNLOAD_SUFFIX) }
            ?.map { file ->
                ModelFileInfo(
                    name = file.name,
                    size = file.length(),
                    path = file.absolutePath,
                    lastModified = file.lastModified()
                )
            }
            ?.sortedByDescending { it.lastModified }
            ?: emptyList()
    }

    /**
     * 删除模型文件
     */
    fun deleteModel(modelName: String): Boolean {
        val modelFile = getModelFile(modelName)
        val tempFile = getTempDownloadFile(modelName)

        var success = true
        if (modelFile.exists()) {
            success = modelFile.delete()
            Log.d(TAG, "删除模型文件: $modelName, 结果: $success")
        }
        if (tempFile.exists()) {
            tempFile.delete()
            Log.d(TAG, "删除临时文件: $modelName$TEMP_DOWNLOAD_SUFFIX")
        }
        return success
    }

    /**
     * 清理所有临时下载文件
     */
    fun cleanAllTempDownloads(): Int {
        val modelDir = getModelDirectory()
        val tempFiles = modelDir.listFiles()
            ?.filter { it.name.endsWith(TEMP_DOWNLOAD_SUFFIX) }
            ?: emptyList()

        var deletedCount = 0
        tempFiles.forEach { file ->
            if (file.delete()) {
                deletedCount++
                Log.d(TAG, "清理临时文件: ${file.name}")
            }
        }
        return deletedCount
    }

    /**
     * 获取存储信息
     */
    fun getStorageInfo(): StorageInfo {
        val modelDir = getModelDirectory()
        val models = getDownloadedModels()
        val totalSize = models.sumOf { it.size }

        return StorageInfo(
            totalModels = models.size,
            totalSize = totalSize,
            availableSpace = modelDir.freeSpace
        )
    }

    // ==================== 兼容方法（DownloadActivity 使用）====================

    /**
     * 获取模型目录（别名方法，兼容 DownloadActivity）
     */
    fun getModelDir(): File {
        return getModelDirectory()
    }

    /**
     * 获取临时下载目录（兼容 DownloadActivity）
     */
    fun getTempDir(): File {
        return getModelDirectory() // 临时文件与模型文件在同一目录
    }

    /**
     * 清理临时文件（别名方法，兼容 DownloadActivity）
     */
    fun cleanTempFiles(): Int {
        return cleanAllTempDownloads()
    }

    // ==================== 数据类 ====================

    data class ModelFileInfo(
        val name: String,
        val size: Long,
        val path: String,
        val lastModified: Long
    )

    data class StorageInfo(
        val totalModels: Int,
        val totalSize: Long,
        val availableSpace: Long
    )
}
