import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.myfreax.audiorecorder.roberta.QuestionAnsweringApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.myfreax.audiorecorder.roberta.QuestionAnsweringRequestBody
import com.myfreax.audiorecorder.roberta.QuestionAnsweringResponse
import com.myfreax.audiorecorder.roberta.RobertaRetrofitClient

class RobertaService(private val context: Context) {
    companion object {
        private const val TAG = "RobertaService"
    }

    private val apiService: QuestionAnsweringApiService by lazy {
        RobertaRetrofitClient.apiService
    }

    fun askQuestion(question: String, context: String) {
        val requestBody = QuestionAnsweringRequestBody(question, context)
        apiService.postQuestion(requestBody).enqueue(object : Callback<QuestionAnsweringResponse> {
            override fun onResponse(call: Call<QuestionAnsweringResponse>, response: Response<QuestionAnsweringResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val qaResponse = response.body()!!
                    Log.d(TAG, "RoBERTa answered: ${qaResponse.answer} with score: ${qaResponse.score}")
                } else {
                    Log.e(TAG, "Failed to get response from RoBERTa: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<QuestionAnsweringResponse>, t: Throwable) {
                Log.e(TAG, "Error in RoBERTa communication: ${t.message}")
            }
        })
    }
}
