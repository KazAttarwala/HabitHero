package com.example.habithero.api

import android.util.Log
import com.example.habithero.config.ApiConfig
import com.example.habithero.model.Habit
import com.example.habithero.model.HabitAnalysis
import com.example.habithero.model.QuoteResponse
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
import org.json.JSONArray
import java.util.concurrent.TimeUnit

interface AnthropicApi {
    @POST("v1/messages")
    suspend fun makeRequest(
        @Header("x-api-key") apiKey: String,
        @Header("anthropic-version") version: String = "2023-06-01",
        @Body requestBody: RequestBody
    ): Response<ResponseBody>
}

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
        val response = anthropicApi.makeRequest(apiKey, "2023-06-01", requestBody)
        
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
                        Log.d(TAG, "Found text content: $text")
                        
                        try {
                            // First, try to parse directly as JSON
                            val quoteJson = try {
                                JSONObject(text)
                            } catch (e: Exception) {
                                // If direct parsing fails, try to extract JSON from markdown code blocks
                                Log.d(TAG, "Direct JSON parsing failed, trying to extract from markdown")
                                val jsonPattern = "```json\\s*(.+?)\\s*```"
                                val alternatePattern = "```\\s*(.+?)\\s*```"
                                
                                val regex = Regex(jsonPattern, RegexOption.DOT_MATCHES_ALL)
                                val match = regex.find(text)
                                
                                if (match != null) {
                                    val jsonContent = match.groupValues[1].trim()
                                    Log.d(TAG, "Extracted JSON from markdown: $jsonContent")
                                    JSONObject(jsonContent)
                                } else {
                                    // Try alternate pattern without json specification
                                    val altRegex = Regex(alternatePattern, RegexOption.DOT_MATCHES_ALL)
                                    val altMatch = altRegex.find(text)
                                    
                                    if (altMatch != null) {
                                        val jsonContent = altMatch.groupValues[1].trim()
                                        Log.d(TAG, "Extracted JSON from alternate markdown: $jsonContent")
                                        JSONObject(jsonContent)
                                    } else {
                                        throw Exception("Could not extract JSON from the response")
                                    }
                                }
                            }
                            
                            return QuoteResponse(
                                quote = quoteJson.getString("quote"),
                                author = quoteJson.getString("author")
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing quote JSON: ${e.message}", e)
                            Log.e(TAG, "Text content: $text")
                            throw Exception("Error parsing quote JSON: ${e.message}")
                        }
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

    suspend fun analyzeHabitData(
        habit: Habit,
        weeklyData: Map<String, Int>,
        completionRate: Int
    ): HabitAnalysis {
        Log.d(TAG, "Analyzing habit data for: ${habit.title}")
        
        // Properly escape user input to avoid JSON formatting issues
        val escapedTitle = habit.title.replace("\"", "\\\"").replace("\n", " ")
        val escapedDescription = habit.description.replace("\"", "\\\"").replace("\n", " ")
        
        // Create a cleaner weekly data string
        val weeklyDataStr = weeklyData.entries.joinToString(", ") { 
            "${it.key}: ${it.value}/${habit.frequency}" 
        }
        
        val jsonBody = """
            {
                "model": "claude-3-haiku-20240307",
                "max_tokens": 1000,
                "system": "You are a supportive habit-building coach in the HabitHero app. Communicate directly with users about their habits in a personalized, encouraging way. Always use 'you' when addressing the user, making them feel like they're having a conversation with a coach who is invested in their success. Format responses in JSON with keys: summary, recommendations, suggestedImprovements.",
                "messages": [
                    {
                        "role": "user",
                        "content": "Here's my habit data:\\n\\nHabit: $escapedTitle\\nDescription: $escapedDescription\\nWeekly progress data: $weeklyDataStr\\nOverall completion rate: ${completionRate}%\\n\\nCan you give me:\\n1. A brief summary of my progress\\n2. Three specific recommendations for improvement\\n3. Two adjustments if I'm struggling"
                    }
                ]
            }
        """.trimIndent()

        Log.d(TAG, "API request body: $jsonBody")
        val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

        Log.d(TAG, "Making API request to Anthropic for habit analysis")
        val response = anthropicApi.makeRequest(ApiConfig.ANTHROPIC_API_KEY, "2023-06-01", requestBody)
        
        if (response.isSuccessful) {
            Log.d(TAG, "API request successful: ${response.code()}")
            val responseString = response.body()?.string() ?: ""
            
            Log.d(TAG, "Full API response: $responseString")
            
            try {
                // Parse the response which is in Claude's content format
                Log.d(TAG, "Parsing response: ${responseString.take(100)}...")
                val jsonResponse = JSONObject(responseString)
                
                // Check for content field
                if (!jsonResponse.has("content")) {
                    Log.e(TAG, "Response missing 'content' field: $responseString")
                    throw Exception("Response missing 'content' field")
                }
                
                val content = jsonResponse.getJSONArray("content")
                
                // Look for the text content that contains our JSON
                for (i in 0 until content.length()) {
                    val item = content.getJSONObject(i)
                    if (item.getString("type") == "text") {
                        val text = item.getString("text")
                        
                        // Parse the actual quote JSON from the text content
                        Log.d(TAG, "Found text content: $text")
                        
                        try {
                            // First, try to parse directly as JSON
                            val analysisJson = try {
                                JSONObject(text)
                            } catch (e: Exception) {
                                // If direct parsing fails, try to extract JSON from markdown code blocks
                                Log.d(TAG, "Direct JSON parsing failed, trying to extract from markdown")
                                val jsonPattern = "```json\\s*(.+?)\\s*```"
                                val alternatePattern = "```\\s*(.+?)\\s*```"
                                
                                val regex = Regex(jsonPattern, RegexOption.DOT_MATCHES_ALL)
                                val match = regex.find(text)
                                
                                if (match != null) {
                                    val jsonContent = match.groupValues[1].trim()
                                    Log.d(TAG, "Extracted JSON from markdown: $jsonContent")
                                    JSONObject(jsonContent)
                                } else {
                                    // Try alternate pattern without json specification
                                    val altRegex = Regex(alternatePattern, RegexOption.DOT_MATCHES_ALL)
                                    val altMatch = altRegex.find(text)
                                    
                                    if (altMatch != null) {
                                        val jsonContent = altMatch.groupValues[1].trim()
                                        Log.d(TAG, "Extracted JSON from alternate markdown: $jsonContent")
                                        JSONObject(jsonContent)
                                    } else {
                                        throw Exception("Could not extract JSON from the response")
                                    }
                                }
                            }
                            
                            // Validate required fields
                            if (!analysisJson.has("summary")) {
                                Log.e(TAG, "Analysis JSON missing 'summary' field: $text")
                                throw Exception("Analysis JSON missing 'summary' field")
                            }
                            
                            if (!analysisJson.has("recommendations")) {
                                Log.e(TAG, "Analysis JSON missing 'recommendations' field: $text")
                                throw Exception("Analysis JSON missing 'recommendations' field")
                            }
                            
                            if (!analysisJson.has("suggestedImprovements")) {
                                Log.e(TAG, "Analysis JSON missing 'suggestedImprovements' field: $text")
                                throw Exception("Analysis JSON missing 'suggestedImprovements' field")
                            }
                            
                            // Parse recommendations array
                            val recommendationsArray = analysisJson.getJSONArray("recommendations")
                            val recommendations = mutableListOf<String>()
                            for (j in 0 until recommendationsArray.length()) {
                                recommendations.add(recommendationsArray.getString(j))
                            }
                            
                            // Parse suggestedImprovements array
                            val improvementsArray = analysisJson.getJSONArray("suggestedImprovements")
                            val improvements = mutableListOf<String>()
                            for (j in 0 until improvementsArray.length()) {
                                improvements.add(improvementsArray.getString(j))
                            }
                            
                            return HabitAnalysis(
                                summary = analysisJson.getString("summary"),
                                recommendations = recommendations,
                                suggestedImprovements = improvements
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing analysis JSON: ${e.message}", e)
                            Log.e(TAG, "Text content: $text")
                            throw Exception("Error parsing analysis JSON: ${e.message}")
                        }
                    }
                }
                Log.e(TAG, "No text content found in response")
                throw Exception("No text content found in response")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse response: ${e.message}", e)
                Log.e(TAG, "Response content: $responseString")
                throw Exception("Failed to parse analysis response: ${e.message}")
            }
        } else {
            val errorBody = response.errorBody()?.string()
            Log.e(TAG, "API request failed: ${response.code()}")
            Log.e(TAG, "Error body: $errorBody")
            throw Exception("Failed to get habit analysis: HTTP ${response.code()} - $errorBody")
        }
    }
}
