package com.example.mobilellmchat.network

import android.util.Log
import com.example.mobilellmchat.api.DouBaoApiService
import com.example.mobilellmchat.model.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * 远端大模型服务实现
 *
 * [MODIFIED] 新增流式输出支持
 *
 * @param apiService 豆包 API 服务接口
 *
 * @author AI-Assisted (Modified)
 * @since Sprint 1 (Updated Sprint 2)
 */
class RemoteLLMService(
    private val apiService: DouBaoApiService
) : LLMService {

    companion object {
        private const val TAG = "RemoteLLMService"
        private const val DEFAULT_MODEL = "doubao-seed-1-6-flash-250828"
    }

    private val gson = Gson()

    /**
     * 发送聊天消息（非流式）- 保持原有逻辑
     */
    override suspend fun chat(messages: List<ApiMessage>): String = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "发送非流式请求，消息数量: ${messages.size}")

            val request = ChatRequest(
                model = DEFAULT_MODEL,
                messages = messages,
                stream = false  // 非流式
            )

            val response = apiService.sendMessage(request)
            val reply = response.choices.firstOrNull()?.message?.content
                ?: throw Exception("API 响应中没有有效内容")

            Log.d(TAG, "收到回复，长度: ${reply.length} 字符")
            reply

        } catch (e: retrofit2.HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e(TAG, "HTTP 错误 ${e.code()}: $errorBody", e)
            throw Exception("API 请求失败 (${e.code()}): ${e.message()}")

        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "网络连接失败", e)
            throw Exception("无法连接到服务器，请检查网络")

        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "请求超时", e)
            throw Exception("请求超时，请稍后重试")

        } catch (e: Exception) {
            Log.e(TAG, "未知错误", e)
            throw Exception("发送消息失败: ${e.message}")
        }
    }

    /**
     * ✅ 新增：流式聊天方法
     *
     * 解析 SSE 格式的响应，逐字返回文本
     */
    override suspend fun chatStream(messages: List<ApiMessage>): Flow<String> = flow {
        try {
            Log.d(TAG, "🌊 发送流式请求，消息数量: ${messages.size}")

            // 构建流式请求
            val request = ChatRequest(
                model = DEFAULT_MODEL,
                messages = messages,
                stream = true  // ✅ 开启流式
            )

            // 发送请求
            val response = apiService.sendMessageStream(request)

            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "❌ HTTP 错误 ${response.code()}: $errorBody")
                throw Exception("API 请求失败 (${response.code()})")
            }

            val body = response.body() ?: throw Exception("响应体为空")

            // 逐行读取 SSE 流
            body.byteStream().bufferedReader().use { reader ->
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    val currentLine = line ?: continue

                    // 跳过空行
                    if (currentLine.isBlank()) continue

                    // 检查是否为结束标记
                    if (currentLine.trim() == "data: [DONE]") {
                        Log.d(TAG, "✅ 流式响应结束")
                        break
                    }

                    // 解析 SSE 格式: "data: {...}"
                    if (currentLine.startsWith("data: ")) {
                        val jsonStr = currentLine.substring(6).trim()

                        try {
                            val chunk = gson.fromJson(jsonStr, StreamChunk::class.java)

                            // 提取实际的文本内容
                            val content = chunk.choices?.firstOrNull()?.delta?.content

                            if (!content.isNullOrEmpty()) {
                                Log.d(TAG, "📝 收到文本片段: $content")
                                emit(content)  // ✅ 发送文本片段
                            }

                        } catch (e: Exception) {
                            Log.w(TAG, "⚠️ 解析 JSON 失败: $jsonStr", e)
                            // 继续处理下一行，不中断流
                        }
                    }
                }
            }

            Log.d(TAG, "🎉 流式处理完成")

        } catch (e: retrofit2.HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e(TAG, "❌ HTTP 错误: $errorBody", e)
            throw Exception("API 请求失败: ${e.message()}")

        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "❌ 网络连接失败", e)
            throw Exception("无法连接到服务器，请检查网络")

        } catch (e: Exception) {
            Log.e(TAG, "❌ 流式处理失败", e)
            throw Exception("流式响应处理失败: ${e.message}")
        }
    }.flowOn(Dispatchers.IO)  // 在 IO 线程执行

    override fun getModelInfo(): ModelInfo {
        return ModelInfo(
            name = "豆包大模型",
            type = "REMOTE",
            backend = "Cloud API",
            status = "Ready"
        )
    }

    override fun stopGeneration() {
        Log.w(TAG, "远端 API 不支持停止生成")
    }
}