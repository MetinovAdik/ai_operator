package com.myfreax.audiorecorder.openai

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

data class OpenAIChatRequest(
    val model: String,
    val messages: List<Message>
)

data class Message(
    val role: String,
    val content: String
)

data class OpenAIChatResponse(
    val id: String,
    val messages: List<Message>,
    val choices: List<Choice>
)

data class Choice(
    val message: Message
)

interface OpenAIChatApiService {
    @Headers("Authorization: Bearer sk-proj-NGw7sji4WVd9hMdPzCxRT3BlbkFJ47dKC1Lt7DIXUBGLJZyr")
    @POST("v1/chat/completions")
    fun createChatCompletion(@Body request: OpenAIChatRequest): Call<OpenAIChatResponse>
}
