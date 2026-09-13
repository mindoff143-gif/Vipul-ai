package com.example.service

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.example.data.model.FacebookPublishResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object FacebookPublisherService {
    private const val TAG = "FacebookPublisher"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun publishToFacebookPage(
        pageId: String,
        pageAccessToken: String,
        message: String,
        linkUrl: String? = null
    ): FacebookPublishResult = withContext(Dispatchers.IO) {
        val effectivePageId = pageId.trim().ifBlank { "109876543210987" }
        val effectiveToken = pageAccessToken.trim()

        // If user has provided a real token, make the official Meta Graph API call
        if (effectiveToken.isNotBlank() && effectiveToken != "DEMO_TOKEN") {
            try {
                val graphUrl = "https://graph.facebook.com/v19.0/$effectivePageId/feed"

                val formBodyBuilder = FormBody.Builder()
                    .add("message", message)
                    .add("access_token", effectiveToken)

                if (!linkUrl.isNullOrBlank() && linkUrl.startsWith("http")) {
                    formBodyBuilder.add("link", linkUrl)
                }

                val request = Request.Builder()
                    .url(graphUrl)
                    .post(formBodyBuilder.build())
                    .build()

                val response = httpClient.newCall(request).execute()
                val responseString = response.body?.string().orEmpty()

                if (response.isSuccessful) {
                    val json = JSONObject(responseString)
                    val postId = json.optString("id", "${effectivePageId}_${System.currentTimeMillis()}")
                    return@withContext FacebookPublishResult(
                        isSuccess = true,
                        postId = postId,
                        message = "फेसबुक पेज पर समाचार सफलतापूर्वक पब्लिश हो गया! (Post ID: $postId)",
                        isSimulatedOrIntent = false
                    )
                } else {
                    Log.e(TAG, "Facebook Graph API error response: $responseString")
                    val errorMsg = try {
                        val errObj = JSONObject(responseString).optJSONObject("error")
                        errObj?.optString("message") ?: "API Error HTTP ${response.code}"
                    } catch (e: Exception) {
                        "HTTP ${response.code}: $responseString"
                    }

                    // Provide helpful guidance for token permission
                    return@withContext FacebookPublishResult(
                        isSuccess = false,
                        postId = null,
                        message = "फेसबुक एरर: $errorMsg। कृपया सुनिश्चित करें कि टोकन में 'pages_manage_posts' और 'pages_read_engagement' की अनुमति (Permission) है।",
                        isSimulatedOrIntent = false
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network error publishing to Facebook: ${e.message}", e)
                return@withContext FacebookPublishResult(
                    isSuccess = false,
                    postId = null,
                    message = "नेटवर्क त्रुटि: ${e.message ?: "फेसबुक सर्वर से संपर्क नहीं हो पाया"}",
                    isSimulatedOrIntent = false
                )
            }
        }

        // Test/Demo mode: Simulate instant publication with generated ID
        val mockPostId = "${effectivePageId}_${System.currentTimeMillis().toString().takeLast(7)}"
        return@withContext FacebookPublishResult(
            isSuccess = true,
            postId = mockPostId,
            message = "सहारनपुर न्यूज़ आपके फेसबुक पेज पर टेस्ट मोड में सफलतापूर्वक पब्लिश हुई! (Post ID: $mockPostId)",
            isSimulatedOrIntent = true
        )
    }

    /**
     * One-tap sharing via the native Facebook Android App or universal share sheet
     * Copies the news content to clipboard and opens the Facebook composer.
     */
    fun shareViaFacebookApp(context: Context, postText: String, linkUrl: String? = null) {
        try {
            // Copy formatted text to clipboard
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText("Saharanpur Facebook Post", postText)
            clipboard?.setPrimaryClip(clip)

            Toast.makeText(
                context,
                "पोस्ट का मैटर कॉपी हो गया है! अब फेसबुक पेज पर पेस्ट करें।",
                Toast.LENGTH_LONG
            ).show()

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "सहारनपुर ताज़ा समाचार")
                putExtra(Intent.EXTRA_TEXT, postText)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Check if Facebook app is installed
            val fbPackage = "com.facebook.katana"
            val pm = context.packageManager
            val fbInstalled = try {
                pm.getPackageInfo(fbPackage, 0)
                true
            } catch (e: Exception) {
                false
            }

            if (fbInstalled) {
                shareIntent.setPackage(fbPackage)
                context.startActivity(shareIntent)
            } else {
                val chooser = Intent.createChooser(shareIntent, "फेसबुक या सोशल मीडिया पर शेयर करें")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing via intent: ${e.message}", e)
            try {
                // Open browser facebook
                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://m.facebook.com")
                ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                context.startActivity(browserIntent)
            } catch (e2: Exception) {
                Toast.makeText(context, "फेसबुक ऐप खोलने में असमर्थ", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
