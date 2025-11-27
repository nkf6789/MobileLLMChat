package com.example.mobilellmchat.model

import java.io.Serializable

/**
 * 模型元数据
 *
 * @property name 模型文件名（如 qwen2-0.5b-instruct-q4_0.gguf）
 * @property displayName 显示名称（如 Qwen2-0.5B 指令模型）
 * @property size 文件大小（字节）
 * @property url 下载地址
 * @property sha256 SHA256 校验值
 * @property version 模型版本
 * @property description 模型描述
 * @property requiredRam 最低运行内存要求（MB）
 * @property quantization 量化类型（Q4_0/Q4_K_M 等）
 */
data class ModelMetadata(
    val name: String,
    val displayName: String,
    val size: Long,
    val url: String,
    val sha256: String,
    val version: String = "1.0",
    val description: String = "",
    val requiredRam: Int = 512,
    val quantization: String = "Q4_0"
) : Serializable {

    companion object {
        /**
         * 预定义模型列表
         *
         * 🔗 模型来源：
         * - Qwen2: https://huggingface.co/Qwen/Qwen2-0.5B-Instruct-GGUF
         * - Phi-2: https://huggingface.co/microsoft/phi-2-gguf
         * - TinyLlama: https://huggingface.co/TinyLlama/TinyLlama-1.1B-Chat-v1.0-GGUF
         */
        fun getAvailableModels(): List<ModelMetadata> {
            return listOf(
                ModelMetadata(
                    name = "qwen2-0.5b-instruct-q4_0.gguf",
                    displayName = "Qwen2-0.5B 指令模型",
                    size = 352_000_000, // ~336 MB
                    url = "https://huggingface.co/Qwen/Qwen2-0.5B-Instruct-GGUF/resolve/main/qwen2-0_5b-instruct-q4_0.gguf",
                    sha256 = "待补充", // 实际使用时需要验证
                    requiredRam = 512,
                    quantization = "Q4_0",
                    description = "轻量级中文对话模型，适合手机运行"
                ),
                ModelMetadata(
                    name = "qwen2-1.5b-instruct-q4_0.gguf",
                    displayName = "Qwen2-1.5B 指令模型",
                    size = 934_000_000, // ~891 MB
                    url = "https://huggingface.co/Qwen/Qwen2-1.5B-Instruct-GGUF/resolve/main/qwen2-1_5b-instruct-q4_0.gguf",
                    sha256 = "待补充",
                    requiredRam = 2048,
                    quantization = "Q4_0",
                    description = "中等规模中文对话模型，效果更好"
                ),
                ModelMetadata(
                    name = "phi-2-q4_0.gguf",
                    displayName = "Phi-2 模型",
                    size = 1_600_000_000, // ~1.5 GB
                    url = "https://huggingface.co/microsoft/phi-2-gguf/resolve/main/phi-2-q4_0.gguf",
                    sha256 = "待补充",
                    requiredRam = 3072,
                    quantization = "Q4_0",
                    description = "微软开源小模型，英文能力强"
                ),
                ModelMetadata(
                    name = "tinyllama-1.1b-chat-q4_0.gguf",
                    displayName = "TinyLlama-1.1B 对话模型",
                    size = 669_000_000, // ~638 MB
                    url = "https://huggingface.co/TinyLlama/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.Q4_0.gguf",
                    sha256 = "待补充",
                    requiredRam = 1536,
                    quantization = "Q4_0",
                    description = "超轻量级英文对话模型"
                )
            )
        }

        /**
         * 根据文件名获取元数据
         */
        fun getByName(name: String): ModelMetadata? {
            return getAvailableModels().find { it.name == name }
        }

        /**
         * 格式化文件大小
         */
        fun formatSize(bytes: Long): String {
            return when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> "${bytes / 1024} KB"
                bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
                else -> String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
            }
        }
    }

    /**
     * 获取格式化后的文件大小
     */
    fun getFormattedSize(): String = formatSize(size)

    /**
     * ✅ 获取格式化后的 RAM 需求
     */
    fun getFormattedRam(): String {
        return when {
            requiredRam >= 1024 -> "%.1f GB RAM".format(requiredRam / 1024.0)
            else -> "$requiredRam MB RAM"
        }
    }
}
