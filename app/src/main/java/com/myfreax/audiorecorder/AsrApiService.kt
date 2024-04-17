package com.myfreax.audiorecorder
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import okhttp3.MultipartBody
import retrofit2.http.Headers

interface AsrApiService {
    @Multipart
    @POST("receive_data")
    @Headers("Authorization: Bearer f283ad02c11065ea379f70fe1c2edeb103d52a2de8638c4b9588e86bfad1041d8974f6a5977b92e1e20bb61fbd7201fbe12b22f1f235e5b89c07250723838fd7")
    fun uploadAudioFile(@Part file: MultipartBody.Part): Call<ApiResponse>
}
