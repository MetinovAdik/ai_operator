package com.myfreax.audiorecorder.tts

import TtsApiRequest



import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TtsService {

    fun synthesizeSpeech(speakerId: Int, text: String, onSuccess: (ResponseBody) -> Unit, onError: (Throwable) -> Unit) {
        // Create an instance of TtsApiRequest
        val request = TtsApiRequest(speaker_id = speakerId, text = text)

        // Make a network call to synthesize speech
        TtsRetrofitClient.apiService.synthesizeSpeech(request).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful && response.body() != null) {
                    // If response is successful and non-null, invoke onSuccess
                    onSuccess(response.body()!!)
                } else {
                    // If response is unsuccessful, invoke onError with a custom message
                    onError(RuntimeException("Failed to synthesize speech: ${response.code()} ${response.message()}"))
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                // If network call failed, invoke onError with the error thrown
                onError(t)
            }
        })
    }
}


