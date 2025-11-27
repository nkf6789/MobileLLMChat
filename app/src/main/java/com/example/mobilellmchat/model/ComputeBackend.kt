package com.example.mobilellmchat.model

/**
 * 计算后端枚举
 * 用于配置本地模型的推理硬件
 */
enum class ComputeBackend {
    /** CPU 推理（兼容性最好） */
    CPU,

    /** Vulkan GPU 加速（需硬件支持） */
    VULKAN,

    /** 自动检测（优先GPU，回退CPU） */
    AUTO;

    companion object {
        /**
         * 从字符串解析，默认返回 CPU
         */
        fun fromString(value: String?): ComputeBackend {
            return when (value?.uppercase()) {
                "VULKAN" -> VULKAN
                "AUTO" -> AUTO
                else -> CPU
            }
        }
    }
}
