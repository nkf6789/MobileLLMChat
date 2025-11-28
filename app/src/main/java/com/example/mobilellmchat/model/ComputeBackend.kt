package com.example.mobilellmchat.model

/**
 * 计算后端枚举
 */
enum class ComputeBackend {
    CPU,      // CPU 计算
    VULKAN,   // GPU (Vulkan)
    AUTO      // 自动选择
}
