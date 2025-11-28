package com.example.mobilellmchat.utils

object Constants {
    // 豆包 API 配置
    const val DOUBAO_BASE_URL = "https://ark.cn-beijing.volces.com/api/v3/"

    // ⚠️ 安全提示：
    // 1. 建议在设置页面让用户输入 API Key
    // 2. 这里仅作为默认值使用
    // 3. 生产环境应通过环境变量或加密存储管理
    const val DOUBAO_API_KEY = "3eb16ff0-7721-4ca8-a63c-6854d8839fdf"

    const val MODEL_ENDPOINT = "doubao-seed-1-6-flash-250828"

    // 网络请求超时设置
    const val CONNECT_TIMEOUT = 30L
    const val READ_TIMEOUT = 30L
    const val WRITE_TIMEOUT = 30L
}
