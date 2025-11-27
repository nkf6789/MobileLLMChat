package com.example.mobilellmchat.model

/**
 * 本地模型配置类
 * 用于传递给 llama.cpp 的推理参数
 */
data class ModelConfig(
    /** 模型文件路径 */
    val modelPath: String,

    /** 计算后端 */
    val backend: ComputeBackend = ComputeBackend.AUTO,

    /** CPU线程数（1-8） */
    val threads: Int = 4,

    /** 上下文长度（token数） */
    val contextLength: Int = 2048,

    /** 温度参数（0.0-2.0，越高越随机） */
    val temperature: Float = 0.7f,

    /** Top-P采样（0.0-1.0） */
    val topP: Float = 0.9f,

    /** 最大生成token数 */
    val maxTokens: Int = 512
) {
    init {
        require(threads in 1..8) { "线程数必须在 1-8 之间" }
        require(temperature in 0.0f..2.0f) { "温度参数必须在 0.0-2.0 之间" }
        require(topP in 0.0f..1.0f) { "Top-P 必须在 0.0-1.0 之间" }
    }

    companion object {
        /**
         * 创建默认配置
         * @param modelPath 模型文件路径
         */
        fun default(modelPath: String) = ModelConfig(
            modelPath = modelPath,
            backend = ComputeBackend.AUTO,
            threads = 4
        )

        /**
         * 创建高性能配置（适合天玑9300+）
         */
        fun highPerformance(modelPath: String) = ModelConfig(
            modelPath = modelPath,
            backend = ComputeBackend.VULKAN,  // 强制使用GPU
            threads = 8,                       // 最大线程数
            contextLength = 4096,              // 更长上下文
            temperature = 0.7f
        )
    }
}
