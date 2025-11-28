package com.example.mobilellmchat.model

/**
 * 模型信息数据类
 * 用于 LLMService.getModelInfo() 返回值
 */
data class ModelInfo(
    val name: String,
    val type: String,
    val backend: String,
    val status: String
)
