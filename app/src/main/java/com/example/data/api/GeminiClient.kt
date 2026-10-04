package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiClient {

    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun getApiKey(): String {
        return try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun generateResponse(
        modelId: String,
        systemPrompt: String,
        history: List<ChatMessage>,
        latestUserMessage: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey().trim()

        if (apiKey.isEmpty()) {
            return@withContext Result.failure(
                IllegalStateException("GEMINI_API_KEY is not configured in the Secrets panel.")
            )
        }

        try {
            val requestJson = JSONObject()

            // System instructions
            if (systemPrompt.isNotBlank()) {
                val sysInstruction = JSONObject()
                val partsArray = JSONArray()
                partsArray.put(JSONObject().put("text", systemPrompt))
                sysInstruction.put("parts", partsArray)
                requestJson.put("systemInstruction", sysInstruction)
            }

            // Generation config
            val genConfig = JSONObject()
            genConfig.put("temperature", 0.7)
            genConfig.put("maxOutputTokens", 2048)
            requestJson.put("generationConfig", genConfig)

            // Multi-turn contents (limit to recent turns to fit context)
            val contentsArray = JSONArray()
            val recentHistory = history.takeLast(16)

            for (msg in recentHistory) {
                if (msg.isError) continue
                val turnObj = JSONObject()
                turnObj.put("role", if (msg.isUser) "user" else "model")
                val parts = JSONArray()
                parts.put(JSONObject().put("text", msg.content))
                turnObj.put("parts", parts)
                contentsArray.put(turnObj)
            }

            // Add latest user message
            val currentTurn = JSONObject()
            currentTurn.put("role", "user")
            val currentParts = JSONArray()
            currentParts.put(JSONObject().put("text", latestUserMessage))
            currentTurn.put("parts", currentParts)
            contentsArray.put(currentTurn)

            requestJson.put("contents", contentsArray)

            val endpoint = "$BASE_URL$modelId:generateContent?key=$apiKey"
            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(endpoint)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                Log.e(TAG, "API Error: code=${response.code}, body=$responseBody")
                val errorMsg = try {
                    val errorJson = JSONObject(responseBody)
                    errorJson.optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}"
                } catch (e: Exception) {
                    "HTTP ${response.code}: $responseBody"
                }
                return@withContext Result.failure(Exception("Gemini API Error: $errorMsg"))
            }

            val json = JSONObject(responseBody)
            val candidates = json.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val candidate = candidates.getJSONObject(0)
                val content = candidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val textBuilder = StringBuilder()
                    for (i in 0 until parts.length()) {
                        val part = parts.getJSONObject(i)
                        textBuilder.append(part.optString("text"))
                    }
                    val resultText = textBuilder.toString().trim()
                    if (resultText.isNotEmpty()) {
                        return@withContext Result.success(resultText)
                    }
                }
            }

            Result.failure(Exception("Empty response received from Gemini API"))
        } catch (e: Exception) {
            Log.e(TAG, "Exception during generateResponse", e)
            Result.failure(e)
        }
    }
}
