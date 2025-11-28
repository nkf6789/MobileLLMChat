package com.example.mobilellmchat.network

import android.util.Log
import com.example.mobilellmchat.api.DouBaoApiService
import com.example.mobilellmchat.model.ApiMessage
import com.example.mobilellmchat.model.ChatRequest
import com.example.mobilellmchat.model.LLMService
import com.example.mobilellmchat.model.ModelInfo
import com.example.mobilellmchat.model.ModelType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 远端大模型服务实现
 *
 * 封装豆包 API 的调用逻辑，实现 LLMService 接口
 *
 * @param apiService 豆包 API 服务接口
 *
 * @author AI-Assisted
 * @since Sprint 1
 */
class RemoteLLMService(
    private val apiService: DouBaoApiService
) : LLMService {

    companion object {
        private const val TAG = "RemoteLLMService"
        private const val DEFAULT_MODEL = "doubao-seed-1-6-flash-250828"
    }

    /**
     * 发送聊天消息并获取 AI 回复
     *
     * @param messages 对话历史（包含角色和内容）
     * @return AI 生成的回复文本
     * @throws Exception 网络错误或 API 错误
     */
    override suspend fun chat(messages: List<ApiMessage>): String = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "发送请求到豆包 API，消息数量: ${messages.size}")

            // 构建请求体
            val request = ChatRequest(
                model = DEFAULT_MODEL,
                messages = messages
            )

            // ✅ 调用正确的 API 方法名
            val response = apiService.sendMessage(request)

            // ✅ 使用正确的字段访问路径
            val reply = response.choices.firstOrNull()?.message?.content
                ?: throw Exception("API 响应中没有有效内容")

            Log.d(TAG, "收到回复，长度: ${reply.length} 字符")

            reply

        } catch (e: retrofit2.HttpException) {
            // HTTP 错误（如 401, 500 等）
            val errorBody = e.response()?.errorBody()?.string()
            Log.e(TAG, "HTTP 错误 ${e.code()}: $errorBody", e)
            throw Exception("API 请求失败 (${e.code()}): ${e.message()}")

        } catch (e: java.net.UnknownHostException) {
            // 网络连接错误
            Log.e(TAG, "网络连接失败", e)
            throw Exception("无法连接到服务器，请检查网络")

        } catch (e: java.net.SocketTimeoutException) {
            // 超时错误
            Log.e(TAG, "请求超时", e)
            throw Exception("请求超时，请稍后重试")

        } catch (e: Exception) {
            // 其他错误
            Log.e(TAG, "未知错误", e)
            throw Exception("发送消息失败: ${e.message}")
        }
    }

    /**
     * 获取模型信息
     */
    override fun getModelInfo(): ModelInfo {
        return ModelInfo(
            name = "豆包大模型",
            type = "REMOTE",           // ✅ 修复：String 类型
            backend = "Cloud API",     // ✅ 修复：新增必需参数
            status = "Ready"           // ✅ 修复：新增必需参数
        )
    }

    /**
     * 停止生成（远端 API 不支持）
     */
    override fun stopGeneration() {
        Log.w(TAG, "远端 API 不支持停止生成")
    }
}
