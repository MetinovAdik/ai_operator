package com.myfreax.audiorecorder.asr

import TtsApiService
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder
import android.util.Log
import com.myfreax.audiorecorder.openai.Message
import com.myfreax.audiorecorder.openai.OpenAIChatApiService
import com.myfreax.audiorecorder.openai.OpenAIChatRequest
import com.myfreax.audiorecorder.openai.OpenAIChatResponse
import com.myfreax.audiorecorder.openai.OpenAIChatRetrofitClient
import com.myfreax.audiorecorder.tts.TtsService
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.FileOutputStream
import java.io.IOException

class AsrUploadService : Service() {
    private lateinit var apiService: AsrApiService
    private lateinit var openAIApiService: OpenAIChatApiService
    private lateinit var ttsService: TtsService
    private var mediaPlayer: MediaPlayer? = null
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        apiService = RetrofitClient.apiService
        openAIApiService = OpenAIChatRetrofitClient.apiService
        ttsService = TtsService()
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
                    if (translation != null) {
                        // Call TtsService to synthesize speech from the translated text
                        synthesizeSpeech(translation)
                    }
                } else {
                    Log.e("OpenAIChat", "Failed to get translation response from OpenAI: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<OpenAIChatResponse>, t: Throwable) {
                Log.e("OpenAIChat", "Error sending message to OpenAI for translation: ${t.message}")
            }
        })
    }

    private fun synthesizeSpeech(translatedText: String) {
        // Assuming speakerId for Kyrgyz language is available, e.g., 1
        val speakerId = 2
        ttsService.synthesizeSpeech(speakerId, translatedText, onSuccess = { responseBody ->
            // Обработайте успешное получение аудиофайла
            handleAudioResponse(responseBody)
        }, onError = {
            // Обработайте ошибку
            Log.e("TtsService", "Ошибка синтеза речи: ${it.message}")
        })
    }
    private fun handleAudioResponse(responseBody: ResponseBody) {
        val audioFileName = "downloaded_audio.mp3"
        try {
            val file = File(getExternalFilesDir(null), audioFileName)

            responseBody.byteStream().use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            Log.d("TtsService", "Аудиофайл успешно сохранен: $audioFileName")
            playAudio(file.path)
        } catch (e: IOException) {
            Log.e("TtsService", "Ошибка при сохранении аудиофайла: ${e.message}")
        }
    }

    private fun playAudio(filePath: String) {
        mediaPlayer?.release()  // Освобождаем ресурсы предыдущего MediaPlayer, если он был создан
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(filePath)
                prepare()  // Подготовка MediaPlayer к воспроизведению
                start()  // Начало воспроизведения
                Log.d("MediaPlayer", "Воспроизведение начато: $filePath")
            } catch (e: IOException) {
                Log.e("MediaPlayer", "Ошибка при воспроизведении аудио: ${e.message}")
            }
        }
    }
    private fun sendTextToOpenAI(text: String) {
        val messages = listOf(
            Message(role = "system", content = "Translate to English"),
            Message(role = "user", content = text)
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
            Message(role = "system", content = "You are assistant of internet provider, - User is asking for help this is info Plans\n" +
                    "Basic\n\n" +
                    "Speed: up to 50 Mbps\n" +
                    "Price: \$7/month\n" +
                    "Includes: unlimited internet, 24/7 support\n" +
                    "Optimal\n\n" +
                    "Speed: up to 100 Mbps\n" +
                    "Price: \$10/month\n" +
                    "Includes: unlimited internet, free router, 24/7 support\n" +
                    "Premium\n\n" +
                    "Speed: up to 200 Mbps\n" +
                    "Price: \$14/month\n" +
                    "Includes: unlimited internet, premium support, free router, antivirus\n" +
                    "Special Services\n" +
                    "VPN Service\n\n" +
                    "Extra fee: \$4/month\n" +
                    "Provides encrypted internet access\n" +
                    "Parental Control\n\n" +
                    "Extra fee: \$3/month\n" +
                    "Allows parents to monitor and limit internet access\n" +
                    "FAQs\n" +
                    "How can I change my plan?\n\n" +
                    "Change your plan via our website or by contacting support.\n" +
                    "What if my internet is down?\n\n" +
                    "Check your device’s connection to the router, restart both, and contact support if unresolved.\n" +
                    "Can I cancel my plan anytime?\n\n" +
                    "Yes, you can cancel anytime by contacting support; early termination fees may apply. Help User. Respond in the language the user wrote to you in. Always answer as shortly as possible and politely"),
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
