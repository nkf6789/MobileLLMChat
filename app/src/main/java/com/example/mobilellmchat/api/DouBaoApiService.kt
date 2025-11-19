package com.example.mobilellmchat.api

import com.example.mobilellmchat.model.ChatRequest
import com.example.mobilellmchat.model.ChatResponse
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface DouBaoApiService {

    @Headers("Content-Type: application/json")
    @POST("chat/completions")
    suspend fun sendMessage(
        @Body request: ChatRequest
    ): ChatResponse
}
