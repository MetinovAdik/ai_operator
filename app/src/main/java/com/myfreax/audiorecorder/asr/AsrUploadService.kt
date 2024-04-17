package com.myfreax.audiorecorder.asr

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.myfreax.audiorecorder.openai.Message
import com.myfreax.audiorecorder.openai.OpenAIChatApiService
import com.myfreax.audiorecorder.openai.OpenAIChatRequest
import com.myfreax.audiorecorder.openai.OpenAIChatResponse
import com.myfreax.audiorecorder.openai.OpenAIChatRetrofitClient
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AsrUploadService : Service() {
    private lateinit var apiService: AsrApiService
    private lateinit var openAIApiService: OpenAIChatApiService
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        apiService = RetrofitClient.apiService
        openAIApiService = OpenAIChatRetrofitClient.apiService
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val filePath = intent?.getStringExtra("file_path")
        if (filePath != null) {
            uploadFile(filePath)
        } else {
            Log.e("ASRUpload", "No file path provided in intent extras")
        }
        return START_NOT_STICKY
    }

    private fun uploadFile(filePath: String) {
        val file = File(filePath)
        if (!file.exists()) {
            Log.e("ASRUpload", "File does not exist: $filePath")
            return
        }

        val requestFile = RequestBody.create(MultipartBody.FORM, file)
        val body = MultipartBody.Part.createFormData("audio", file.name, requestFile)

        apiService.uploadAudioFile(body).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    Log.d("ASRUpload", "File uploaded successfully: ${response.body()?.text}")
                    handleTranslationAndQuestionAnswering(response.body()!!.text)
                } else {
                    Log.e("ASRUpload", "Upload failed: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Log.e("ASRUpload", "Error uploading file: ${t.message}")
            }
        })
    }

    private fun handleTranslationAndQuestionAnswering(translatedText: String) {
        val context = "if it is Kyrgyz language, translate to English, if English translate to Kyrgyz"
        val fullText = "$context $translatedText"
        sendTextToOpenAI(fullText)
    }
    private fun translateToKyrgyz(text: String) {
        val messages = listOf(
            Message(role = "system", content = "Translate to Kyrgyz"),
            Message(role = "user", content = text)
        )
        val request = OpenAIChatRequest(model = "gpt-4-turbo-2024-04-09", messages = messages)
        openAIApiService.createChatCompletion(request).enqueue(object : Callback<OpenAIChatResponse> {
            override fun onResponse(call: Call<OpenAIChatResponse>, response: Response<OpenAIChatResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val translation = response.body()?.choices?.firstOrNull()?.message?.content
                    Log.d("OpenAIChat", "Translation to Kyrgyz received: $translation")
                } else {
                    Log.e("OpenAIChat", "Failed to get translation response from OpenAI: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<OpenAIChatResponse>, t: Throwable) {
                Log.e("OpenAIChat", "Error sending message to OpenAI for translation: ${t.message}")
            }
        })
    }
    private fun sendTextToOpenAI(text: String) {
        val messages = listOf(
            Message(role = "system", content = "Translate to English"),
            Message(role = "user", content = text )
        )
        val request = OpenAIChatRequest(model = "gpt-4-turbo-2024-04-09", messages = messages)
        openAIApiService.createChatCompletion(request).enqueue(object : Callback<OpenAIChatResponse> {
            override fun onResponse(call: Call<OpenAIChatResponse>, response: Response<OpenAIChatResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    Log.d("OpenAIChat", "Translation received: ${response.body()?.choices?.firstOrNull()?.message?.content}")
                    val translation = response.body()?.choices?.firstOrNull()?.message?.content
                    if (translation != null) {
                        sendTextToGPT3(translation)
                    }
                } else {
                    Log.e("OpenAIChat", "Failed to get response from OpenAI: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<OpenAIChatResponse>, t: Throwable) {
                Log.e("OpenAIChat", "Error sending message to OpenAI: ${t.message}")
            }
        })
    }
    private fun sendTextToGPT3(text: String) {
        val messages = listOf(
            Message(role = "system", content = "You are assistant of internet provider, - User is asking for help  this is info Plans\\n\" +\n" +
                    "       \"Basic\\n\" +\n" +
                    "       \"\\n\" +\n" +
                    "       \"Speed: up to 50 Mbps\\n\" +\n" +
                    "       \"Price: \\\$7/month\\n\" +\n" +
                    "       \"Includes: unlimited internet, 24/7 support\\n\" +\n" +
                    "       \"Optimal\\n\" +\n" +
                    "       \"\\n\" +\n" +
                    "       \"Speed: up to 100 Mbps\\n\" +\n" +
                    "       \"Price: \\\$10/month\\n\" +\n" +
                    "       \"Includes: unlimited internet, free router, 24/7 support\\n\" +\n" +
                    "       \"Premium\\n\" +\n" +
                    "       \"\\n\" +\n" +
                    "       \"Speed: up to 200 Mbps\\n\" +\n" +
                    "       \"Price: \\\$14/month\\n\" +\n" +
                    "       \"Includes: unlimited internet, premium support, free router, antivirus\\n\" +\n" +
                    "       \"Special Services\\n\" +\n" +
                    "       \"VPN Service\\n\" +\n" +
                    "       \"\\n\" +\n" +
                    "       \"Extra fee: \\\$4/month\\n\" +\n" +
                    "       \"Provides encrypted internet access\\n\" +\n" +
                    "       \"Parental Control\\n\" +\n" +
                    "       \"\\n\" +\n" +
                    "       \"Extra fee: \\\$3/month\\n\" +\n" +
                    "       \"Allows parents to monitor and limit internet access\\n\" +\n" +
                    "       \"FAQs\\n\" +\n" +
                    "       \"How can I change my plan?\\n\" +\n" +
                    "       \"\\n\" +\n" +
                    "       \"Change your plan via our website or by contacting support.\\n\" +\n" +
                    "       \"What if my internet is down?\\n\" +\n" +
                    "       \"\\n\" +\n" +
                    "       \"Check your device’s connection to the router, restart both, and contact support if unresolved.\\n\" +\n" +
                    "       \"Can I cancel my plan anytime?\\n\" +\n" +
                    "       \"\\n\" +\n" +
                    "       \"Yes, you can cancel anytime by contacting support; early termination fees may apply. Help User. Respond in the language the user wrote to you in. Always answer as shortly as possible and politely"),
            Message(role = "user", content = text)
        )
        val request = OpenAIChatRequest(model = "gpt-3.5-turbo-0125", messages = messages)
        openAIApiService.createChatCompletion(request).enqueue(object : Callback<OpenAIChatResponse> {
            override fun onResponse(call: Call<OpenAIChatResponse>, response: Response<OpenAIChatResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    Log.d("OpenAIChat", "Message received from gpt3: ${response.body()?.choices?.firstOrNull()?.message?.content}")
                    val answer = response.body()?.choices?.firstOrNull()?.message?.content
                    if (answer != null) {
                        translateToKyrgyz(answer)
                    }
                } else {
                    Log.e("OpenAIChat", "Failed to get response from OpenAI: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<OpenAIChatResponse>, t: Throwable) {
                Log.e("OpenAIChat", "Error sending message to OpenAI: ${t.message}")
            }
        })
    }

    private fun loggingInterceptor() = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
}
