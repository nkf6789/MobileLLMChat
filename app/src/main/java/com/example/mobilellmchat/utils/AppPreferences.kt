package com.example.mobilellmchat.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.mobilellmchat.model.ComputeBackend
import com.example.mobilellmchat.model.ModelType

/**
 * 应用配置管理类
 *
 * 使用 SharedPreferences 持久化存储：
 * - 模型类型选择（远端/本地）
 * - 本地模型配置参数
 * - GPU/CPU 计算设置
 *
 * 特性：
 * - 线程安全（SharedPreferences 内部已同步）
 * - 类型安全（属性委托）
 * - 默认值处理
 *
 * @author AI-Assisted
 * @since Sprint 1
 * [MODIFIED] Sprint 2 - 优化属性访问，移除冗余方法
 */
class AppPreferences(context: Context) {

    companion object {
        private const val PREFS_NAME = "mobile_llm_chat_prefs"

        // Key 常量
        private const val KEY_MODEL_TYPE = "model_type"
        private const val KEY_LOCAL_MODEL_NAME = "local_model_name"
        private const val KEY_LOCAL_MODEL_PATH = "local_model_path"
        private const val KEY_COMPUTE_BACKEND = "compute_backend"
        private const val KEY_CPU_THREADS = "cpu_threads"
        private const val KEY_TEMPERATURE = "temperature"
        private const val KEY_TOP_P = "top_p"
        private const val KEY_MAX_TOKENS = "max_tokens"
        private const val KEY_CONTEXT_LENGTH = "context_length"
        private const val KEY_FIRST_LAUNCH = "first_launch"

        // 默认值
        private const val DEFAULT_MODEL_NAME = "qwen2-0.5b-instruct-q4_k_m.gguf"
        private const val DEFAULT_CPU_THREADS = 4
        private const val DEFAULT_TEMPERATURE = 0.7f
        private const val DEFAULT_TOP_P = 0.9f
        private const val DEFAULT_MAX_TOKENS = 512
        private const val DEFAULT_CONTEXT_LENGTH = 2048
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    // ==================== 模型类型配置 ====================

    /**
     * 当前使用的模型类型
     * 默认：REMOTE（远端豆包API）
     */
    var modelType: ModelType
        get() {
            val typeString = prefs.getString(KEY_MODEL_TYPE, ModelType.REMOTE.name)
            return try {
                ModelType.valueOf(typeString ?: ModelType.REMOTE.name)
            } catch (e: IllegalArgumentException) {
                ModelType.REMOTE
            }
        }
        set(value) {
            prefs.edit().putString(KEY_MODEL_TYPE, value.name).apply()
        }

    /**
     * 本地模型文件名
     * 默认：qwen2-0.5b-instruct-q4_k_m.gguf
     */
    var localModelName: String
        get() = prefs.getString(KEY_LOCAL_MODEL_NAME, DEFAULT_MODEL_NAME)
            ?: DEFAULT_MODEL_NAME
        set(value) {
            prefs.edit().putString(KEY_LOCAL_MODEL_NAME, value).apply()
        }

    /**
     * 本地模型完整路径
     * 用于 SettingsActivity 保存选中的模型路径
     */
    var localModelPath: String
        get() = prefs.getString(KEY_LOCAL_MODEL_PATH, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_LOCAL_MODEL_PATH, value).apply()
            // 同步更新 modelName
            if (value.isNotEmpty()) {
                localModelName = value.substringAfterLast("/")
            }
        }

    // ==================== 计算后端配置 ====================

    /**
     * 计算后端类型
     * 默认：AUTO（自动检测）
     */
    var computeBackend: ComputeBackend
        get() {
            val backendString = prefs.getString(KEY_COMPUTE_BACKEND, ComputeBackend.AUTO.name)
            return ComputeBackend.fromString(backendString)
        }
        set(value) {
            prefs.edit().putString(KEY_COMPUTE_BACKEND, value.name).apply()
        }

    /**
     * GPU 加速开关（便捷属性）
     * 读取：判断 computeBackend 是否为 VULKAN
     * 写入：设置为 VULKAN 或 CPU
     */
    var useGPU: Boolean
        get() = computeBackend == ComputeBackend.VULKAN
        set(value) {
            computeBackend = if (value) ComputeBackend.VULKAN else ComputeBackend.CPU
        }

    /**
     * CPU 线程数（1-8）
     * 默认：4
     */
    var cpuThreads: Int
        get() = prefs.getInt(KEY_CPU_THREADS, DEFAULT_CPU_THREADS)
            .coerceIn(1, 8)
        set(value) {
            prefs.edit().putInt(KEY_CPU_THREADS, value.coerceIn(1, 8)).apply()
        }

    // ==================== 推理参数配置 ====================

    /**
     * 温度参数（0.0-2.0）
     * 默认：0.7
     */
    var temperature: Float
        get() = prefs.getFloat(KEY_TEMPERATURE, DEFAULT_TEMPERATURE)
            .coerceIn(0.0f, 2.0f)
        set(value) {
            prefs.edit().putFloat(KEY_TEMPERATURE, value.coerceIn(0.0f, 2.0f)).apply()
        }

    /**
     * Top-P 采样参数（0.0-1.0）
     * 默认：0.9
     */
    var topP: Float
        get() = prefs.getFloat(KEY_TOP_P, DEFAULT_TOP_P)
            .coerceIn(0.0f, 1.0f)
        set(value) {
            prefs.edit().putFloat(KEY_TOP_P, value.coerceIn(0.0f, 1.0f)).apply()
        }

    /**
     * 最大生成 token 数
     * 默认：512
     */
    var maxTokens: Int
        get() = prefs.getInt(KEY_MAX_TOKENS, DEFAULT_MAX_TOKENS)
        set(value) {
            prefs.edit().putInt(KEY_MAX_TOKENS, value).apply()
        }

    /**
     * 上下文长度
     * 默认：2048
     */
    var contextLength: Int
        get() = prefs.getInt(KEY_CONTEXT_LENGTH, DEFAULT_CONTEXT_LENGTH)
        set(value) {
            prefs.edit().putInt(KEY_CONTEXT_LENGTH, value).apply()
        }

    // ==================== 应用状态 ====================

    /**
     * 是否首次启动
     * 用于显示引导页或下载提示
     */
    var isFirstLaunch: Boolean
        get() = prefs.getBoolean(KEY_FIRST_LAUNCH, true)
        set(value) {
            prefs.edit().putBoolean(KEY_FIRST_LAUNCH, value).apply()
        }

    // ==================== 工具方法 ====================

    /**
     * 重置所有配置为默认值
     */
    fun resetToDefaults() {
        prefs.edit().clear().apply()
    }

    /**
     * 获取当前配置的调试信息
     */
    fun getDebugInfo(): String {
        return """
            |模型类型: $modelType
            |本地模型: $localModelName
            |本地路径: $localModelPath
            |计算后端: $computeBackend
            |CPU线程: $cpuThreads
            |温度: $temperature
            |Top-P: $topP
            |最大Token: $maxTokens
            |上下文长度: $contextLength
        """.trimMargin()
    }
}
