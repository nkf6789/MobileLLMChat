package com.example.mobilellmchat.api

import com.example.mobilellmchat.model.ChatRequest
import com.example.mobilellmchat.model.ChatResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Streaming

/**
 * 豆包 API 服务接口
 *
 * [MODIFIED] 新增流式接口
 *
 * @author AI-Assisted (Modified)
 * @since Sprint 1 (Updated Sprint 2)
 */
interface DouBaoApiService {

    /**
     * 发送聊天消息（非流式）
     */
    @POST("chat/completions")
    suspend fun sendMessage(@Body request: ChatRequest): ChatResponse

    /**
     * ✅ 新增：流式聊天接口
     *
     * @Streaming 注解告诉 Retrofit 不要一次性加载整个响应
     * @return ResponseBody 原始响应体，用于逐行读取 SSE
     */
    @Streaming
    @POST("chat/completions")
    suspend fun sendMessageStream(@Body request: ChatRequest): Response<ResponseBody>
}