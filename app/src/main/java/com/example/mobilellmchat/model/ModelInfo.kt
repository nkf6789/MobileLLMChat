package com.example.mobilellmchat.model

/**
 * 模型信息数据类
 * 用于UI展示当前使用的模型状态
 */
data class ModelInfo(
    /** 模型显示名称 */
    val name: String,

    /** 模型类型 */
    val type: ModelType,

    /** 计算后端（仅本地模型有效） */
    val backend: ComputeBackend = ComputeBackend.CPU,

    /** 模型文件大小（MB，可选） */
    val sizeInMB: Int = 0,

    /** 是否已准备就绪 */
    val isReady: Boolean = true
) {
    /**
     * 生成状态描述文本
     */
    fun getStatusText(): String {
        return when (type) {
            ModelType.REMOTE -> "云端模型 - $name"
            ModelType.LOCAL -> {
                val backendText = when (backend) {
                    ComputeBackend.VULKAN -> "GPU加速"
                    ComputeBackend.CPU -> "CPU推理"
                    ComputeBackend.AUTO -> "自动检测"
                }
                "本地模型 - $name ($backendText)"
            }
        }
    }
}
