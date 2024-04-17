package com.myfreax.audiorecorder

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AsrUploadService : Service() {
    private lateinit var apiService: AsrApiService
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    override fun onCreate() {
        super.onCreate()
        apiService = RetrofitClient.apiService
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
                    // Additional logging if needed
                    Log.d("ASRUpload", "Success response: ${response.body()}")
                } else {
                    Log.e("ASRUpload", "Upload failed: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Log.e("ASRUpload", "Error uploading file: ${t.message}")
            }
        })
    }

    private fun loggingInterceptor() = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
}
