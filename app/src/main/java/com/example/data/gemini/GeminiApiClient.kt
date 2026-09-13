package com.example.data.gemini

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiApiClient {
    private const val TAG = "GeminiApiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun generateAssistantResponse(
        userPrompt: String,
        contextMemory: String,
        preferredLanguage: String = "hi" // "hi" for Hindi default, "en" for English
    ): GeminiResult = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }

        val effectiveApiKey = if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            apiKey
        } else {
            ""
        }

        val systemInstructionText = """
            You are a highly capable, intelligent Personal AI Assistant for Android named "Personal AI Assistant".
            
            CORE BEHAVIOR & RULES:
            1. LANGUAGE:
               - By default, ALWAYS REPLY IN POLITE, NATURAL HINDI (or Hinglish if appropriate).
               - If the user specifically addresses you in English or prefers English, reply in clear, professional English.
            2. FUNCTIONAL ROLE:
               - You manage the user's calls, SMS, WhatsApp, Telegram, Gmail, daily schedule, and reminders.
               - You remember all past communications ("who said what", contact histories, spam detections).
               - When the user asks you to create a reminder, send an SMS/WhatsApp/Telegram, check schedule, or screen a call, you must output an ACTION TAG at the very top of your response in this exact format:
                 [ACTION:CREATE_REMINDER:title:time_or_location:contact]
                 [ACTION:SEND_SMS:contact_name_or_number:message_content]
                 [ACTION:SEND_WHATSAPP:contact_name_or_number:message_content]
                 [ACTION:SEND_TELEGRAM:contact_name_or_number:message_content]
                 [ACTION:SCHEDULE_BRIEFING]
                 [ACTION:SHOW_CONTACT_HISTORY:contact_name]
                 [ACTION:SCREEN_CALL:caller_name_or_number]
                 [ACTION:QUERY_MEMORY:contact_or_topic]
                 [ACTION:EMAIL_SUMMARY]
                 [ACTION:SEARCH_SAHARANPUR_NEWS:query]
                 [ACTION:PUBLISH_SAHARANPUR_NEWS_FB:news_id]
                 [ACTION:GENERAL_AI]
               Followed by your natural, warm voice reply in Hindi (or English).
            3. SAHARANPUR NEWS & FACEBOOK PUBLISHING:
               - You can search local news inside Saharanpur, Uttar Pradesh (UP), India (infrastructure, Smart City, Maa Shakumbhari University, sugarcane mills/farmers, law & order).
               - You can format viral, engaging posts in Hindi with bold headings, bullet points, and hashtags (#Saharanpur #SaharanpurNews #UttarPradesh #UPNews) and publish them to the user's Facebook Page.
            4. GENERAL KNOWLEDGE:
               - You possess comprehensive general knowledge, comparable to ChatGPT, Gemini, and Claude.
               - If the user asks general questions (science, history, calculations, explanations, code, general advice, news concepts), provide a thorough, accurate, and structured answer, by default in Hindi.
            
            CURRENT CONTEXT & DATA IN MEMORY:
            $contextMemory
        """.trimIndent()

        if (effectiveApiKey.isEmpty()) {
            Log.d(TAG, "No Gemini API key provided, running on-device smart fallback logic")
            return@withContext GeminiFallbackEngine.processQuery(userPrompt, contextMemory, preferredLanguage)
        }

        try {
            val payload = JSONObject().apply {
                // system instruction
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", systemInstructionText))
                    })
                })
                // contents
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", userPrompt))
                        })
                    })
                })
                // generation config
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                    put("topP", 0.95)
                    put("maxOutputTokens", 1200)
                })
            }

            val requestBody = payload.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$BASE_URL?key=$effectiveApiKey")
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                Log.w(TAG, "Gemini API HTTP ${response.code}: $errorBody")
                return@withContext GeminiFallbackEngine.processQuery(userPrompt, contextMemory, preferredLanguage)
            }

            val responseBody = response.body?.string() ?: ""
            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                val text = parts?.optJSONObject(0)?.optString("text") ?: ""
                parseActionAndText(text, preferredLanguage)
            } else {
                GeminiFallbackEngine.processQuery(userPrompt, contextMemory, preferredLanguage)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calling Gemini API: ${e.message}", e)
            GeminiFallbackEngine.processQuery(userPrompt, contextMemory, preferredLanguage)
        }
    }

    private fun parseActionAndText(rawText: String, language: String): GeminiResult {
        val actionRegex = Regex("""\[ACTION:([A-Z_]+)(?::([^\]]+))?\]""")
        val match = actionRegex.find(rawText)
        val cleanText = actionRegex.replace(rawText, "").trim()

        return if (match != null) {
            val actionType = match.groupValues[1]
            val actionParams = match.groupValues.getOrNull(2) ?: ""
            GeminiResult(
                responseText = cleanText,
                actionType = actionType,
                actionParams = actionParams,
                language = language
            )
        } else {
            GeminiResult(
                responseText = cleanText,
                actionType = "GENERAL_AI",
                actionParams = "",
                language = language
            )
        }
    }
}

data class GeminiResult(
    val responseText: String,
    val actionType: String,
    val actionParams: String = "",
    val language: String = "hi"
)
