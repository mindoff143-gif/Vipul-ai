package com.example.service

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TtsManager(context: Context) : TextToSpeech.OnInitListener {
    private val TAG = "TtsManager"
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _currentLanguage = MutableStateFlow("hi") // default Hindi
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    private var pendingText: String? = null

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            setLanguage(_currentLanguage.value)

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    Log.e(TAG, "TTS Utterance Error on ID: $utteranceId")
                }
            })

            pendingText?.let {
                speak(it, _currentLanguage.value)
                pendingText = null
            }
        } else {
            Log.e(TAG, "TTS Initialization failed")
        }
    }

    fun setLanguage(langCode: String) {
        _currentLanguage.value = langCode
        if (!isInitialized || tts == null) return

        val locale = when (langCode) {
            "hi" -> Locale.forLanguageTag("hi-IN")
            "en" -> Locale.ENGLISH
            else -> Locale.forLanguageTag("hi-IN")
        }

        val result = tts?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(TAG, "Locale $locale not directly supported, falling back to default")
            tts?.setLanguage(Locale.ENGLISH)
        }
    }

    fun speak(text: String, languageCode: String = _currentLanguage.value) {
        if (!isInitialized || tts == null) {
            pendingText = text
            return
        }

        stop()
        setLanguage(languageCode)

        // Remove markdown action tags and bold markers for clean audio speech
        val cleanedText = text
            .replace(Regex("""\[ACTION:[^\]]+\]"""), "")
            .replace("*", "")
            .replace("#", "")
            .trim()

        val utteranceId = "ai_assistant_${System.currentTimeMillis()}"
        tts?.speak(cleanedText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        try {
            tts?.stop()
            _isSpeaking.value = false
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS: ${e.message}")
        }
    }

    fun release() {
        stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
