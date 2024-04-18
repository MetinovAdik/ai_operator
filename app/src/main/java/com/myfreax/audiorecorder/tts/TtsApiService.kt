import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

data class TtsApiRequest(
    val speaker_id: Int,
    val text: String
)

interface TtsApiService {
    @Headers("Content-Type: application/json", "Authorization: Bearer f283ad02c11065ea379f70fe1c2edeb103d52a2de8638c4b9588e86bfad1041d8974f6a5977b92e1e20bb61fbd7201fbe12b22f1f235e5b89c07250723838fd7")
    @POST("tts")
    fun synthesizeSpeech(@Body request: TtsApiRequest): Call<ResponseBody>
}
