package com.example.mobilellmchat.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.mobilellmchat.model.ComputeBackend
import com.example.mobilellmchat.model.ModelType

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "app_preferences",
        Context.MODE_PRIVATE
    )

    // ========== 模型类型 ==========

    var modelType: ModelType
        get() {
            val value = prefs.getString("model_type", ModelType.REMOTE.name)
            return try {
                ModelType.valueOf(value ?: ModelType.REMOTE.name)
            } catch (e: IllegalArgumentException) {
                ModelType.REMOTE
            }
        }
        set(value) = prefs.edit().putString("model_type", value.name).apply()

    // ========== 本地模型配置 ==========

    var localModelPath: String
        get() = prefs.getString("local_model_path", "") ?: ""
        set(value) = prefs.edit().putString("local_model_path", value).apply()

    var contextLength: Int
        get() = prefs.getInt("context_length", 2048)
        set(value) = prefs.edit().putInt("context_length", value).apply()

    var cpuThreads: Int
        get() = prefs.getInt("cpu_threads", 4)
        set(value) = prefs.edit().putInt("cpu_threads", value).apply()

    var computeBackend: ComputeBackend
        get() {
            val value = prefs.getString("compute_backend", ComputeBackend.CPU.name)
            return try {
                ComputeBackend.valueOf(value ?: ComputeBackend.CPU.name)
            } catch (e: IllegalArgumentException) {
                ComputeBackend.CPU
            }
        }
        set(value) = prefs.edit().putString("compute_backend", value.name).apply()

    var temperature: Float
        get() = prefs.getFloat("temperature", 0.7f)
        set(value) = prefs.edit().putFloat("temperature", value).apply()

    var topP: Float
        get() = prefs.getFloat("top_p", 0.9f)
        set(value) = prefs.edit().putFloat("top_p", value).apply()

    var topK: Int
        get() = prefs.getInt("top_k", 40)
        set(value) = prefs.edit().putInt("top_k", value).apply()

    var repeatPenalty: Float
        get() = prefs.getFloat("repeat_penalty", 1.1f)
        set(value) = prefs.edit().putFloat("repeat_penalty", value).apply()

    // ========== 云端模型配置 ========== ✅ 修改这部分

    var apiKey: String
        get() = prefs.getString("api_key", "3eb16ff0-7721-4ca8-a63c-6854d8839fdf") ?: ""
        set(value) = prefs.edit().putString("api_key", value).apply()

    var baseUrl: String
        get() {
            val url = prefs.getString("base_url", "https://ark.cn-beijing.volces.com/api/v3/") ?: ""
            // ✅ 自动补末尾斜杠
            return if (url.endsWith("/")) url else "$url/"
        }
        set(value) {
            // ✅ 保存时也自动补斜杠
            val normalized = if (value.endsWith("/")) value else "$value/"
            prefs.edit().putString("base_url", normalized).apply()
        }

    var modelName: String
        get() = prefs.getString("model_name", "ep-20241226114801-lxkhw") ?: ""
        set(value) = prefs.edit().putString("model_name", value).apply()

    var maxTokens: Int
        get() = prefs.getInt("max_tokens", 512)
        set(value) = prefs.edit().putInt("max_tokens", value).apply()

    // ========== 应用设置 ==========

    var enableStreaming: Boolean
        get() = prefs.getBoolean("enable_streaming", true)
        set(value) = prefs.edit().putBoolean("enable_streaming", value).apply()

    var enableAutoSave: Boolean
        get() = prefs.getBoolean("enable_auto_save", true)
        set(value) = prefs.edit().putBoolean("enable_auto_save", value).apply()

    // ========== 辅助方法 ==========

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    fun hasValidApiKey(): Boolean {
        return apiKey.isNotEmpty()
    }

    fun hasValidLocalModel(): Boolean {
        return localModelPath.isNotEmpty()
    }
}
