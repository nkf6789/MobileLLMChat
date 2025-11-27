package com.example.mobilellmchat.utils

import android.content.Context
import android.os.StatFs
import android.util.Log
import java.io.File
import java.security.MessageDigest

/**
 * 模型文件管理工具类
 *
 * 职责：
 * - 管理本地模型文件的存储位置
 * - 检查模型文件是否存在
 * - 提供模型文件路径
 * - 列出和删除模型文件
 * - 支持下载管理（临时文件、SHA256校验、存储空间检查）
 *
 * 存储策略：
 * - 使用应用私有目录 (internal storage)
 * - 路径：/data/data/com.example.mobilellmchat/files/models/
 * - 临时下载文件：*.download
 * - 卸载应用时自动清理
 *
 * @author AI-Assisted
 * @since Sprint 1
 * [MODIFIED] Sprint 2 - 添加实例方法以兼容 SettingsActivity
 * [MODIFIED] Sprint 3 - 添加下载管理支持（临时文件、校验、空间检查）
 */
class ModelFileManager(private val context: Context) {

    companion object {
        private const val TAG = "ModelFileManager"
        private const val MODELS_DIR_NAME = "models"
        private const val TEMP_DOWNLOAD_SUFFIX = ".download"
        private const val MIN_FREE_SPACE_BUFFER_MB = 100L // 预留100MB缓冲空间

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
        fun formatFileSize(bytes: Long): String {
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

    // ==================== 下载管理功能（Sprint 3 新增）====================

    /**
     * 检查存储空间是否足够
     *
     * @param requiredBytes 需要的字节数
     * @return 是否足够（含100MB缓冲）
     */
    fun hasEnoughSpace(requiredBytes: Long): Boolean {
        val stat = StatFs(getModelDirectory().path)
        val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
        val requiredWithBuffer = requiredBytes + (MIN_FREE_SPACE_BUFFER_MB * 1024 * 1024)

        val hasSpace = availableBytes >= requiredWithBuffer

        if (!hasSpace) {
            Log.w(TAG, "存储空间不足: 需要 ${formatFileSize(requiredWithBuffer)}, " +
                    "可用 ${formatFileSize(availableBytes)}")
        }

        return hasSpace
    }

    /**
     * 获取可用存储空间（字节）
     */
    fun getAvailableSpace(): Long {
        val stat = StatFs(getModelDirectory().path)
        return stat.availableBlocksLong * stat.blockSizeLong
    }

    /**
     * 获取可用存储空间（格式化字符串）
     */
    fun getAvailableSpaceFormatted(): String {
        return formatFileSize(getAvailableSpace())
    }

    /**
     * 获取总存储空间（字节）
     */
    fun getTotalSpace(): Long {
        val stat = StatFs(getModelDirectory().path)
        return stat.blockCountLong * stat.blockSizeLong
    }

    /**
     * 获取总存储空间（格式化字符串）
     */
    fun getTotalSpaceFormatted(): String {
        return formatFileSize(getTotalSpace())
    }

    /**
     * 获取模型下载临时文件路径
     *
     * @param modelName 模型文件名
     * @return 临时文件的完整路径
     */
    fun getTempDownloadPath(modelName: String): String {
        return File(getModelDirectory(), "$modelName$TEMP_DOWNLOAD_SUFFIX").absolutePath
    }

    /**
     * 获取临时下载文件对象
     */
    fun getTempDownloadFile(modelName: String): File {
        return File(getTempDownloadPath(modelName))
    }

    /**
     * 检查是否存在未完成的下载
     *
     * @param modelName 模型文件名
     * @return 临时文件是否存在
     */
    fun hasPendingDownload(modelName: String): Boolean {
        val tempFile = getTempDownloadFile(modelName)
        return tempFile.exists() && tempFile.length() > 0
    }

    /**
     * 获取未完成下载的已下载大小
     *
     * @param modelName 模型文件名
     * @return 已下载的字节数（0表示无未完成下载）
     */
    fun getPendingDownloadSize(modelName: String): Long {
        val tempFile = getTempDownloadFile(modelName)
        return if (tempFile.exists()) tempFile.length() else 0L
    }

    /**
     * 下载完成后将临时文件重命名为正式文件
     *
     * @param modelName 模型文件名
     * @return 是否成功
     */
    fun finalizeTempDownload(modelName: String): Boolean {
        val tempFile = getTempDownloadFile(modelName)
        val finalFile = getModelFile(modelName)

        return try {
            if (!tempFile.exists()) {
                Log.e(TAG, "临时文件不存在: ${tempFile.absolutePath}")
                return false
            }

            // 如果目标文件已存在，先删除
            if (finalFile.exists()) {
                Log.w(TAG, "目标文件已存在，先删除: ${finalFile.name}")
                finalFile.delete()
            }

            val success = tempFile.renameTo(finalFile)
            if (success) {
                Log.d(TAG, "重命名成功: ${tempFile.name} -> ${finalFile.name}")
            } else {
                Log.e(TAG, "重命名失败: ${tempFile.name}")
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "重命名异常: ${e.message}", e)
            false
        }
    }

    /**
     * 删除临时下载文件
     *
     * @param modelName 模型文件名
     * @return 是否成功
     */
    fun deleteTempDownload(modelName: String): Boolean {
        val tempFile = getTempDownloadFile(modelName)

        return if (tempFile.exists()) {
            val deleted = tempFile.delete()
            if (deleted) {
                Log.d(TAG, "已删除临时文件: ${tempFile.name}")
            } else {
                Log.e(TAG, "删除临时文件失败: ${tempFile.name}")
            }
            deleted
        } else {
            Log.d(TAG, "临时文件不存在，无需删除: $modelName")
            true // 不存在也视为成功
        }
    }

    /**
     * 清理所有临时下载文件
     *
     * @return 删除的文件数量
     */
    fun cleanAllTempDownloads(): Int {
        val modelsDir = getModelDirectory()
        val tempFiles = modelsDir.listFiles { file ->
            file.isFile && file.name.endsWith(TEMP_DOWNLOAD_SUFFIX, ignoreCase = true)
        } ?: emptyArray()

        var deletedCount = 0
        tempFiles.forEach { file ->
            if (file.delete()) {
                deletedCount++
                Log.d(TAG, "已清理临时文件: ${file.name}")
            }
        }

        if (deletedCount > 0) {
            Log.d(TAG, "清理完成，共删除 $deletedCount 个临时文件")
        }

        return deletedCount
    }

    /**
     * 计算文件的 SHA256 哈希值
     *
     * @param filePath 文件路径
     * @return SHA256 字符串（小写十六进制），失败返回 null
     */
    fun calculateSHA256(filePath: String): String? {
        return try {
            val file = File(filePath)
            if (!file.exists() || !file.isFile) {
                Log.e(TAG, "文件不存在或不是文件: $filePath")
                return null
            }

            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }

            val hash = digest.digest().joinToString("") { "%02x".format(it) }
            Log.d(TAG, "SHA256 计算完成: ${file.name} -> $hash")
            hash
        } catch (e: Exception) {
            Log.e(TAG, "SHA256 计算失败: ${e.message}", e)
            null
        }
    }

    /**
     * 验证模型文件完整性
     *
     * @param modelName 模型文件名
     * @param expectedSHA256 期望的 SHA256 值
     * @return 是否通过验证
     */
    fun verifyModelIntegrity(modelName: String, expectedSHA256: String): Boolean {
        // 跳过占位符校验值
        if (expectedSHA256.equals("待补充", ignoreCase = true) ||
            expectedSHA256.isBlank()) {
            Log.w(TAG, "跳过 SHA256 校验（占位符）: $modelName")
            return true
        }

        val modelPath = getModelPath(modelName)
        val actualSHA256 = calculateSHA256(modelPath)

        return if (actualSHA256 == null) {
            Log.e(TAG, "SHA256 计算失败，验证失败: $modelName")
            false
        } else {
            val isValid = actualSHA256.equals(expectedSHA256, ignoreCase = true)
            if (isValid) {
                Log.d(TAG, "SHA256 校验通过: $modelName")
            } else {
                Log.e(TAG, "SHA256 校验失败: $modelName\n" +
                        "期望: $expectedSHA256\n" +
                        "实际: $actualSHA256")
            }
            isValid
        }
    }

    /**
     * 获取存储空间使用情况
     *
     * @return StorageInfo 对象
     */
    fun getStorageInfo(): StorageInfo {
        val total = getTotalSpace()
        val available = getAvailableSpace()
        val used = total - available
        val modelsSize = getTotalModelsSize()

        return StorageInfo(
            totalBytes = total,
            availableBytes = available,
            usedBytes = used,
            modelsSizeBytes = modelsSize,
            totalFormatted = formatFileSize(total),
            availableFormatted = formatFileSize(available),
            usedFormatted = formatFileSize(used),
            modelsSizeFormatted = formatFileSize(modelsSize)
        )
    }

    // ==================== 数据类 ====================

    /**
     * 模型文件信息数据类
     */
    data class ModelFileInfo(
        val name: String,
        val path: String,
        val sizeInBytes: Long,
        val sizeFormatted: String
    )

    /**
     * 存储空间信息数据类
     */
    data class StorageInfo(
        val totalBytes: Long,
        val availableBytes: Long,
        val usedBytes: Long,
        val modelsSizeBytes: Long,
        val totalFormatted: String,
        val availableFormatted: String,
        val usedFormatted: String,
        val modelsSizeFormatted: String
    ) {
        /**
         * 获取使用率百分比（0-100）
         */
        fun getUsagePercentage(): Int {
            return if (totalBytes > 0) {
                ((usedBytes * 100) / totalBytes).toInt()
            } else 0
        }

        /**
         * 获取模型占用率百分比（0-100）
         */
        fun getModelsPercentage(): Int {
            return if (totalBytes > 0) {
                ((modelsSizeBytes * 100) / totalBytes).toInt()
            } else 0
        }
    }
}
