package com.example.service

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Service to handle Invite Links, social sharing (WhatsApp, SMS, All Apps),
 * and direct APK download triggering.
 */
object AppInviteService {

    const val DIRECT_APK_URL = "https://ais-pre-fhl65wwees3no3x535okry-424357755952.asia-southeast1.run.app/personal-ai-assistant.apk"
    const val DOWNLOAD_PAGE_URL = "https://ais-pre-fhl65wwees3no3x535okry-424357755952.asia-southeast1.run.app/download.html"
    const val PREVIEW_URL = "https://ais-pre-fhl65wwees3no3x535okry-424357755952.asia-southeast1.run.app"

    fun getInviteMessage(language: String = "hi"): String {
        return if (language == "hi") {
            """
            🤖 *पर्सनल AI असिस्टेंट (Personal AI Assistant)*
            *सहारनपुर न्यूज़ व फेसबुक पब्लिशर*
            
            नमस्ते! मैंने आपके लिए एक बहुत ही काम का पर्सनल AI असिस्टेंट ऐप भेजा है।
            
            ✨ मुख्य खासियतें:
            • 🎙️ हिंदी व इंग्लिश में बोलकर कॉल, SMS व रिमाइंडर सेट करें
            • 📰 सहारनपुर (UP) की ताज़ा खबरें 1-क्लिक में खोजें
            • 📢 फेसबुक पेज पर खबरें सीधे ऑटो-पोस्ट करें
            • 🛡️ अनजान व स्पैम कॉल्स को AI अपने आप स्क्रीन करेगा
            
            📲 *सीधे APK डाउनलोड करने के लिए इस लिंक पर क्लिक करें:*
            $DIRECT_APK_URL
            
            (लिंक खोलते ही APK तुरंत आपके फोन में डाउनलोड हो जाएगी!)
            
            🌐 वेब पेज लिंक: $DOWNLOAD_PAGE_URL
            """.trimIndent()
        } else {
            """
            🤖 *Personal AI Assistant*
            *Voice Assistant, Saharanpur News & Facebook Publisher*
            
            Hello! Try out this Personal AI Assistant Android App:
            • 🎙️ Voice & call manager in Hindi & English
            • 📰 Live Saharanpur & UP news search
            • 📢 1-click Facebook Page publishing
            • 🛡️ Smart call screening & spam protection
            
            📲 *Direct APK Download Link (Instant Download):*
            $DIRECT_APK_URL
            
            🌐 Web Download Page: $DOWNLOAD_PAGE_URL
            """.trimIndent()
        }
    }

    /**
     * Share invite link directly to WhatsApp
     */
    fun shareViaWhatsApp(context: Context, language: String = "hi"): Boolean {
        val text = getInviteMessage(language)
        return try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                `package` = "com.whatsapp"
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            // If WhatsApp not installed, fallback to system chooser
            shareViaSystem(context, language)
            false
        }
    }

    /**
     * Share invite via SMS
     */
    fun shareViaSms(context: Context, language: String = "hi") {
        val text = getInviteMessage(language)
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("sms:")
                putExtra("sms_body", text)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            shareViaSystem(context, language)
        }
    }

    /**
     * Open system share sheet for any app (Facebook, Telegram, Gmail, etc.)
     */
    fun shareViaSystem(context: Context, language: String = "hi") {
        val text = getInviteMessage(language)
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "पर्सनल AI असिस्टेंट ऐप इनवाइट")
                putExtra(Intent.EXTRA_TEXT, text)
            }
            val chooser = Intent.createChooser(intent, "इनवाइट लिंक शेयर करें (Share Invite Link)").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "शेयर नहीं हो सका: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Copy direct APK download link to clipboard
     */
    fun copyDirectApkLink(context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("Direct APK Download Link", DIRECT_APK_URL)
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(context, "✅ डायरेक्ट APK डाउनलोड लिंक कॉपी हो गया!", Toast.LENGTH_SHORT).show()
    }

    /**
     * Copy full invitation text to clipboard
     */
    fun copyFullInviteMessage(context: Context, language: String = "hi") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("App Invite Message", getInviteMessage(language))
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(context, "✅ इनवाइट मैसेज लिंक सहित कॉपी हो गया!", Toast.LENGTH_SHORT).show()
    }

    /**
     * Trigger direct APK download on current device via browser
     */
    fun downloadApkDirectly(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(DIRECT_APK_URL)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Toast.makeText(context, "🚀 APK डाउनलोड शुरू हो रहा है...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "डाउनलोड लिंक नहीं खुल सका: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
