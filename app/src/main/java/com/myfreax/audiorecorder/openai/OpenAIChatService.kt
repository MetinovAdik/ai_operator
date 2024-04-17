package com.myfreax.audiorecorder.openai

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class OpenAIChatService : Service() {
    companion object {
        private const val TAG = "OpenAIChatService"
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    fun sendChatToOpenAI(messages: List<Message>) {
        val request = OpenAIChatRequest("gpt-4-turbo-2024-04-09", messages)
        OpenAIChatRetrofitClient.apiService.createChatCompletion(request).enqueue(object : Callback<OpenAIChatResponse> {
            override fun onResponse(call: Call<OpenAIChatResponse>, response: Response<OpenAIChatResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val chatResponse = response.body()!!
                    Log.d(TAG, "Chat response: ${chatResponse.messages.last().content}")
                } else {
                    Log.e(TAG, "Failed to get response: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<OpenAIChatResponse>, t: Throwable) {
                Log.e(TAG, "Error sending chat message: ${t.message}")
            }
        })
    }
}