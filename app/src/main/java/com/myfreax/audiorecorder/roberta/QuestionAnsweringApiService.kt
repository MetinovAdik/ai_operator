package com.myfreax.audiorecorder.roberta

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
data class QuestionAnsweringRequestBody(
    val question: String,
    val context: String
)
data class QuestionAnsweringResponse(
    val score: Float,
    val start: Int,
    val end: Int,
    val answer: String
)
interface QuestionAnsweringApiService {
    @Headers("Authorization: Bearer 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")
    @POST("/models/deepset/roberta-base-squad2")
    fun postQuestion(
        @Body requestBody: QuestionAnsweringRequestBody
    ): Call<QuestionAnsweringResponse>
}