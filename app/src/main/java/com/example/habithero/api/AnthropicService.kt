package com.example.habithero.api

import android.util.Log
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import org.json.JSONObject
import java.util.concurrent.TimeUnit

interface AnthropicApi {
    @POST("v1/messages")
    suspend fun getQuote(
        @Header("x-api-key") apiKey: String,
        @Header("anthropic-version") version: String = "2023-06-01",
        @Body requestBody: RequestBody
    ): Response<ResponseBody>
}

data class QuoteResponse(
    val quote: String,
    val author: String
)

class AnthropicService {
    private val TAG = "AnthropicService"
    
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.anthropic.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val anthropicApi = retrofit.create(AnthropicApi::class.java)

    suspend fun getMotivationalQuote(apiKey: String): QuoteResponse {
        val jsonBody = """
            {
                "model": "claude-3-haiku-20240307",
                "max_tokens": 100,
                "messages": [
                    {
                        "role": "user",
                        "content": "Give me a short motivational quote about building good habits or consistency. Response must be in JSON format with 'quote' and 'author' fields only. Make sure the author is a real person known for their wisdom or expertise."
                    }
                ]
            }
        """.trimIndent()

        val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

        Log.d(TAG, "Making API request to Anthropic")
        val response = anthropicApi.getQuote(apiKey, "2023-06-01", requestBody)
        
        if (response.isSuccessful) {
            Log.d(TAG, "API request successful: ${response.code()}")
            val responseString = response.body()?.string() ?: ""
            
            try {
                // Parse the response which is in Claude's content format
                Log.d(TAG, "Parsing response: ${responseString.take(100)}...")
                val jsonResponse = JSONObject(responseString)
                val content = jsonResponse.getJSONArray("content")
                
                // Look for the text content that contains our JSON
                for (i in 0 until content.length()) {
                    val item = content.getJSONObject(i)
                    if (item.getString("type") == "text") {
                        val text = item.getString("text")
                        
                        // Parse the actual quote JSON from the text content
                        Log.d(TAG, "Found text content: ${text.take(50)}...")
                        val quoteJson = JSONObject(text)
                        return QuoteResponse(
                            quote = quoteJson.getString("quote"),
                            author = quoteJson.getString("author")
                        )
                    }
                }
                Log.e(TAG, "No text content found in response")
                throw Exception("No text content found in response")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse response: ${e.message}", e)
                Log.e(TAG, "Response content: $responseString")
                throw Exception("Failed to parse quote response: ${e.message}")
            }
        } else {
            val errorBody = response.errorBody()?.string()
            Log.e(TAG, "API request failed: ${response.code()}")
            Log.e(TAG, "Error body: $errorBody")
            throw Exception("Failed to get quote: HTTP ${response.code()} - $errorBody")
        }
    }
}
