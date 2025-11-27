package com.example.mobilellmchat.model

/**
 * 模型类型枚举
 * 用于区分远端API和本地端侧模型
 */
enum class ModelType {
    /** 远端大模型（豆包API） */
    REMOTE,

    /** 本地端侧模型（llama.cpp） */
    LOCAL
}
