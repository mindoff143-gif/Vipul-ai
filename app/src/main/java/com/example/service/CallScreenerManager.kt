package com.example.service

import android.content.Context
import com.example.data.model.CommunicationLog

class CallScreenerManager(private val context: Context) {

    data class ScreeningDecision(
        val isSpam: Boolean,
        val callerName: String,
        val callerNumber: String,
        val aiGreetingSpeech: String,
        val screeningResultNote: String,
        val actionTaken: String // "AUTO_REJECTED", "ACCEPTED_WITH_NOTE", "VIP_PASSED"
    )

    fun screenIncomingCall(phoneNumber: String, contactName: String? = null): ScreeningDecision {
        val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
        val isKnownSpam = cleanNumber.contains("14098") ||
                cleanNumber.startsWith("+91140") ||
                cleanNumber.startsWith("140") ||
                contactName?.contains("Spam", ignoreCase = true) == true

        val displayName = contactName ?: if (isKnownSpam) "संभावित स्पैम (Suspected Spam)" else "अज्ञात कॉलर (Unknown Caller)"

        return if (isKnownSpam) {
            ScreeningDecision(
                isSpam = true,
                callerName = displayName,
                callerNumber = phoneNumber,
                aiGreetingSpeech = "नमस्ते। मैं इनका AI पर्सनल असिस्टेंट बोल रहा हूँ। यह नंबर टेलीमार्केटिंग स्पैम के रूप में चिह्नित है। यूजर इस समय उपलब्ध नहीं हैं।",
                screeningResultNote = "स्पैम कॉल डिटेक्ट: AI ने कॉल को स्वतः स्क्रीन करके डिस्कनेक्ट कर दिया। यूजर को डिस्टर्ब नहीं किया गया।",
                actionTaken = "AUTO_REJECTED"
            )
        } else {
            ScreeningDecision(
                isSpam = false,
                callerName = displayName,
                callerNumber = phoneNumber,
                aiGreetingSpeech = "नमस्ते! मैं इनका AI पर्सनल असिस्टेंट हूँ। यूजर अभी एक महत्वपूर्ण कार्य में व्यस्त हैं। कृपया अपना संदेश या कारण बताएं, मैं तुरंत उन तक पहुँचा दूँगा।",
                screeningResultNote = "AI कॉल स्क्रीनिंग: कॉलर ने मीटिंग के संदर्भ में संपर्क किया है। संदेश सुरक्षित कर लिया गया है।",
                actionTaken = "ACCEPTED_WITH_NOTE"
            )
        }
    }
}
