package com.bimarihaunter.network

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.io.IOException

object GeminiClient {
    private const val API_KEY = "AIzaSyCcsseKeLBcSPpECdW7H_w2b4CrlaoZXQM"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta2/models/gemini-1.5-pro:generate"
    private val gson = Gson()
    private val client = OkHttpClient.Builder().build()

    suspend fun generateReply(prompt: String): String {
        val requestBody = GeminiRequest(
            prompt = Prompt(prompt),
            temperature = 0.2,
            maxOutputTokens = 512
        )
        val json = gson.toJson(requestBody)
        val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
        val urlWithKey = "$BASE_URL?key=$API_KEY"

        val request = Request.Builder()
            .url(urlWithKey)
            .post(body)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Timber.e("Gemini request failed: ${response.code} ${response.message}")
                    return@use "I couldn't reach the smart assistant right now. Please try again later."
                }
                val responseBody = response.body?.string().orEmpty()
                val geminiResponse = try {
                    gson.fromJson(responseBody, GeminiResponse::class.java)
                } catch (e: Throwable) {
                    Timber.e(e, "Gemini response parsing failed")
                    null
                }
                geminiResponse?.candidates?.firstOrNull()?.output?.trim().orEmpty().takeIf { it.isNotBlank() }
                    ?: "I couldn't generate a response at this time."
            }
        } catch (e: Throwable) {
            Timber.e(e, "Gemini request failed")
            "I couldn't reach the smart assistant right now. Please try again later."
        }
    }
}

data class GeminiRequest(
    val prompt: Prompt,
    val temperature: Double,
    @SerializedName("max_output_tokens")
    val maxOutputTokens: Int
)

data class Prompt(
    val text: String
)

data class GeminiResponse(
    val candidates: List<Candidate>? = null
)

data class Candidate(
    val output: String? = null
)
