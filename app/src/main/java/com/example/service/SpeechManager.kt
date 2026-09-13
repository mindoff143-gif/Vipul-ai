package com.example.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class SpeechManager(
    private val context: Context,
    private val onSpeechRecognized: (String) -> Unit
) : RecognitionListener {
    private val TAG = "SpeechManager"
    private var speechRecognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _soundLevel = MutableStateFlow(0f)
    val soundLevel: StateFlow<Float> = _soundLevel.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        initRecognizer()
    }

    private fun initRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(this@SpeechManager)
            }
        } else {
            Log.w(TAG, "Speech recognition is not available on this device")
        }
    }

    fun startListening(languageCode: String = "hi-IN") {
        if (speechRecognizer == null) {
            initRecognizer()
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageCode)
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, languageCode)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        try {
            speechRecognizer?.startListening(intent)
            _isListening.value = true
            _errorMessage.value = null
        } catch (e: Exception) {
            Log.e(TAG, "Error starting speech recognition: ${e.message}")
            _isListening.value = false
            _errorMessage.value = e.message
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            _isListening.value = false
            _soundLevel.value = 0f
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping speech recognizer: ${e.message}")
        }
    }

    override fun onReadyForSpeech(params: Bundle?) {
        _isListening.value = true
    }

    override fun onBeginningOfSpeech() {
        _isListening.value = true
    }

    override fun onRmsChanged(rmsdB: Float) {
        // Normalize sound level from -2dB..10dB to 0..1 for visualizer
        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
        _soundLevel.value = normalized
    }

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        _isListening.value = false
        _soundLevel.value = 0f
    }

    override fun onError(error: Int) {
        _isListening.value = false
        _soundLevel.value = 0f
        val msg = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "ऑडियो रिकॉर्डिंग त्रुटि"
            SpeechRecognizer.ERROR_CLIENT -> "क्लाइंट त्रुटि"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "माइक्रोफ़ोन अनुमति आवश्यक है"
            SpeechRecognizer.ERROR_NETWORK -> "नेटवर्क कनेक्शन चेक करें"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "नेटवर्क टाइमआउट"
            SpeechRecognizer.ERROR_NO_MATCH -> "आवाज़ स्पष्ट सुनाई नहीं दी, कृपया पुनः बोलें"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "स्पीच रिकॉग्नाइज़र व्यस्त है"
            SpeechRecognizer.ERROR_SERVER -> "सर्वर त्रुटि"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "कोई आवाज़ नहीं मिली"
            else -> "त्रुटि कोड: $error"
        }
        _errorMessage.value = msg
        Log.w(TAG, "Speech error: $msg ($error)")
    }

    override fun onResults(results: Bundle?) {
        _isListening.value = false
        _soundLevel.value = 0f
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val spokenText = matches[0]
            onSpeechRecognized(spokenText)
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        // Can be used for live transcription if desired
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    fun destroy() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying speech recognizer: ${e.message}")
        }
    }
}
