package com.example.mobilellmchat.utils

import android.content.Context
import android.util.Log
import java.io.File

/**
 * 模型文件管理工具类
 *
 * 职责：
 * - 管理本地模型文件的存储位置
 * - 检查模型文件是否存在
 * - 提供模型文件路径
 * - 列出和删除模型文件
 *
 * 存储策略：
 * - 使用应用私有目录 (internal storage)
 * - 路径：/data/data/com.example.mobilellmchat/files/models/
 * - 卸载应用时自动清理
 *
 * @author AI-Assisted
 * @since Sprint 1
 * [MODIFIED] Sprint 2 - 添加实例方法以兼容 SettingsActivity
 */
class ModelFileManager(private val context: Context) {

    companion object {
        private const val TAG = "ModelFileManager"
        private const val MODELS_DIR_NAME = "models"

        // ==================== 静态方法（保持向后兼容）====================

        /**
         * 获取模型存储目录（静态方法）
         */
        @JvmStatic
        fun getModelsDir(context: Context): File {
            val modelsDir = File(context.filesDir, MODELS_DIR_NAME)

            if (!modelsDir.exists()) {
                val created = modelsDir.mkdirs()
                if (created) {
                    Log.d(TAG, "创建模型目录: ${modelsDir.absolutePath}")
                } else {
                    Log.e(TAG, "创建模型目录失败: ${modelsDir.absolutePath}")
                }
            }

            return modelsDir
        }

        /**
         * 获取指定模型文件的完整路径（静态方法）
         */
        @JvmStatic
        fun getModelPath(context: Context, modelName: String): String {
            return File(getModelsDir(context), modelName).absolutePath
        }

        /**
         * 检查模型是否已下载（静态方法）
         */
        @JvmStatic
        fun isModelDownloaded(context: Context, modelName: String): Boolean {
            val modelFile = File(getModelsDir(context), modelName)
            val exists = modelFile.exists() && modelFile.length() > 0

            if (exists) {
                Log.d(TAG, "模型已存在: $modelName (${formatFileSize(modelFile.length())})")
            }

            return exists
        }

        /**
         * 格式化文件大小
         */
        @JvmStatic
        private fun formatFileSize(bytes: Long): String {
            return when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
                bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
                else -> "%.2f GB".format(bytes / (1024.0 * 1024 * 1024))
            }
        }
    }

    // ==================== 实例方法（SettingsActivity 使用）====================

    /**
     * 获取模型存储目录
     */
    fun getModelDirectory(): File {
        return getModelsDir(context)
    }

    /**
     * 获取指定模型的完整路径
     */
    fun getModelPath(modelName: String): String {
        return getModelPath(context, modelName)
    }

    /**
     * 检查模型是否已下载
     */
    fun isModelDownloaded(modelName: String): Boolean {
        return isModelDownloaded(context, modelName)
    }

    /**
     * 获取模型文件对象
     */
    fun getModelFile(modelName: String): File {
        return File(getModelDirectory(), modelName)
    }

    /**
     * 列出所有已下载的模型文件
     */
    fun listDownloadedModels(): List<File> {
        val modelsDir = getModelDirectory()

        return modelsDir.listFiles { file ->
            file.isFile && file.name.endsWith(".gguf", ignoreCase = true)
        }?.toList() ?: emptyList()
    }

    /**
     * 获取已下载模型的信息列表
     */
    fun getDownloadedModelsInfo(): List<ModelFileInfo> {
        return listDownloadedModels().map { file ->
            ModelFileInfo(
                name = file.name,
                path = file.absolutePath,
                sizeInBytes = file.length(),
                sizeFormatted = formatFileSize(file.length())
            )
        }.sortedByDescending { it.sizeInBytes }
    }

    /**
     * 删除指定模型文件
     */
    fun deleteModel(modelName: String): Boolean {
        val modelFile = File(getModelDirectory(), modelName)

        return if (modelFile.exists()) {
            val deleted = modelFile.delete()
            if (deleted) {
                Log.d(TAG, "已删除模型: $modelName")
            } else {
                Log.e(TAG, "删除模型失败: $modelName")
            }
            deleted
        } else {
            Log.w(TAG, "模型文件不存在，无需删除: $modelName")
            false
        }
    }

    /**
     * 获取模型文件大小（MB）
     */
    fun getModelSize(modelName: String): Double {
        val file = File(getModelDirectory(), modelName)
        if (!file.exists()) return 0.0
        return file.length() / (1024.0 * 1024.0)
    }

    /**
     * 获取模型目录的总占用空间
     */
    fun getTotalModelsSize(): Long {
        return listDownloadedModels().sumOf { it.length() }
    }

    /**
     * 清空所有模型文件
     */
    fun clearAllModels(): Int {
        val models = listDownloadedModels()
        var deletedCount = 0

        models.forEach { file ->
            if (file.delete()) {
                deletedCount++
                Log.d(TAG, "已删除: ${file.name}")
            }
        }

        Log.d(TAG, "清空完成，共删除 $deletedCount 个模型文件")
        return deletedCount
    }

    /**
     * 模型文件信息数据类
     */
    data class ModelFileInfo(
        val name: String,
        val path: String,
        val sizeInBytes: Long,
        val sizeFormatted: String
    )
}
