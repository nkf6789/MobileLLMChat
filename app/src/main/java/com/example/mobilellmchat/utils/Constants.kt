package com.example.mobilellmchat.utils

object Constants {
    // 豆包 API 配置
    const val DOUBAO_BASE_URL = "https://ark.cn-beijing.volces.com/api/v3/"

    // ⚠️ 请替换成你的实际 API Key
    const val DOUBAO_API_KEY = "3eb16ff0-7721-4ca8-a63c-6854d8839fdf"

    // 模型 ID（需要根据你的豆包控制台配置）
    const val MODEL_ENDPOINT = "doubao-seed-1-6-flash-250828"

    // 网络请求超时设置
    const val CONNECT_TIMEOUT = 30L
    const val READ_TIMEOUT = 30L
    const val WRITE_TIMEOUT = 30L
}
