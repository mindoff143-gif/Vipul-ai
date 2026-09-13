package com.example.data.repository

import android.content.Context
import com.example.data.gemini.GeminiApiClient
import com.example.data.gemini.GeminiResult
import com.example.data.local.AssistantDao
import com.example.data.model.AssistantChatMessage
import com.example.data.model.CalendarEventItem
import com.example.data.model.CommunicationLog
import com.example.data.model.ContactRecord
import com.example.data.model.DailyBriefingData
import com.example.data.model.EmailSummaryItem
import com.example.data.model.FacebookPageConfig
import com.example.data.model.FacebookPublishResult
import com.example.data.model.ReminderItem
import com.example.data.model.SaharanpurNewsEntity
import com.example.service.CalendarManager
import com.example.service.CallScreenerManager
import com.example.service.CommunicationManager
import com.example.service.FacebookPublisherService
import com.example.service.SaharanpurNewsService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AssistantRepository(
    private val dao: AssistantDao,
    private val communicationManager: CommunicationManager,
    private val calendarManager: CalendarManager,
    private val callScreenerManager: CallScreenerManager,
    private val context: Context? = null
) {
    val chatMessages: Flow<List<AssistantChatMessage>> = dao.getAllChatMessages()
    val communicationLogs: Flow<List<CommunicationLog>> = dao.getAllCommunicationLogs()
    val reminders: Flow<List<ReminderItem>> = dao.getAllReminders()
    val contacts: Flow<List<ContactRecord>> = dao.getAllContacts()
    val emails: Flow<List<EmailSummaryItem>> = dao.getAllEmails()
    val saharanpurNews: Flow<List<SaharanpurNewsEntity>> = dao.getAllSaharanpurNews()

    private val prefs = context?.getSharedPreferences("ai_assistant_prefs", Context.MODE_PRIVATE)

    suspend fun processUserCommand(
        userInput: String,
        preferredLanguage: String = "hi"
    ): GeminiResult {
        // 1. Record user message
        dao.insertChatMessage(
            AssistantChatMessage(
                sender = "USER",
                text = userInput,
                language = preferredLanguage
            )
        )

        // 2. Fetch context memory for Gemini
        val recentLogs = dao.getAllCommunicationLogs().firstOrNull() ?: emptyList()
        val allReminders = dao.getAllReminders().firstOrNull() ?: emptyList()
        val allContacts = dao.getAllContacts().firstOrNull() ?: emptyList()

        val contextMemoryBuilder = StringBuilder().apply {
            append("RECENT CONTACTS: ")
            allContacts.take(5).forEach { append("${it.name} (${it.phone}), ") }
            append("\nRECENT COMMUNICATIONS (WHO SAID WHAT): ")
            recentLogs.take(6).forEach {
                append("[${it.platform} from ${it.contactName} (${it.type}): \"${it.content}\"], ")
            }
            append("\nACTIVE REMINDERS: ")
            allReminders.filter { !it.isCompleted }.take(4).forEach {
                append("[${it.title} at ${if (it.isLocationBased) it.locationName else "Time: " + it.timeInMillis}], ")
            }
        }

        // 3. Call Gemini API (or Fallback Engine)
        val geminiResult = GeminiApiClient.generateAssistantResponse(
            userPrompt = userInput,
            contextMemory = contextMemoryBuilder.toString(),
            preferredLanguage = preferredLanguage
        )

        // 4. Execute physical side-effects if needed
        executeActionSideEffect(geminiResult)

        // 5. Record assistant reply
        dao.insertChatMessage(
            AssistantChatMessage(
                sender = "ASSISTANT",
                text = geminiResult.responseText,
                language = geminiResult.language,
                actionType = geminiResult.actionType,
                actionDetails = geminiResult.actionParams
            )
        )

        return geminiResult
    }

    private suspend fun executeActionSideEffect(result: GeminiResult) {
        when (result.actionType) {
            "CREATE_REMINDER" -> {
                val parts = result.actionParams.split(":")
                val title = parts.getOrNull(0) ?: "महत्वपूर्ण कार्य (Reminder)"
                val locationOrTime = parts.getOrNull(1) ?: "Office"
                val isLocation = locationOrTime.contains("Office", ignoreCase = true) ||
                        locationOrTime.contains("Home", ignoreCase = true) ||
                        locationOrTime.contains("Market", ignoreCase = true)

                val reminder = ReminderItem(
                    title = title,
                    timeInMillis = if (!isLocation) System.currentTimeMillis() + 4 * 3600 * 1000L else 0L,
                    isLocationBased = isLocation,
                    locationName = if (isLocation) locationOrTime else null,
                    isCompleted = false,
                    isSyncedToCalendar = true
                )
                dao.insertReminder(reminder)
                if (!isLocation) {
                    calendarManager.syncReminderToCalendar(title, reminder.timeInMillis, locationOrTime)
                }
            }

            "SEND_SMS" -> {
                val parts = result.actionParams.split(":")
                val contact = parts.getOrNull(0) ?: "Rahul Sharma"
                val msg = parts.getOrNull(1) ?: "Hello from Personal AI Assistant"
                dao.insertCommunicationLog(
                    CommunicationLog(
                        contactName = contact,
                        contactNumber = "+91 98123 45678",
                        platform = "SMS",
                        type = "OUTGOING",
                        content = msg,
                        timestamp = System.currentTimeMillis(),
                        aiSummary = "SMS sent: $msg"
                    )
                )
                communicationManager.sendSms("+919812345678", msg)
            }

            "SEND_WHATSAPP" -> {
                val parts = result.actionParams.split(":")
                val contact = parts.getOrNull(0) ?: "Rahul Sharma"
                val msg = parts.getOrNull(1) ?: "Hello from Personal AI Assistant"
                dao.insertCommunicationLog(
                    CommunicationLog(
                        contactName = contact,
                        contactNumber = "+91 98123 45678",
                        platform = "WHATSAPP",
                        type = "OUTGOING",
                        content = msg,
                        timestamp = System.currentTimeMillis(),
                        aiSummary = "WhatsApp sent: $msg"
                    )
                )
                communicationManager.sendWhatsApp("+919812345678", msg)
            }

            "SEND_TELEGRAM" -> {
                val parts = result.actionParams.split(":")
                val contact = parts.getOrNull(0) ?: "Priya Verma"
                val msg = parts.getOrNull(1) ?: "Hello from Personal AI Assistant"
                dao.insertCommunicationLog(
                    CommunicationLog(
                        contactName = contact,
                        contactNumber = "+91 99887 76655",
                        platform = "TELEGRAM",
                        type = "OUTGOING",
                        content = msg,
                        timestamp = System.currentTimeMillis(),
                        aiSummary = "Telegram sent: $msg"
                    )
                )
                communicationManager.sendTelegram("priya_verma", msg)
            }

            "SCREEN_CALL" -> {
                val caller = result.actionParams.ifEmpty { "1409876543" }
                val decision = callScreenerManager.screenIncomingCall(caller)
                dao.insertCommunicationLog(
                    CommunicationLog(
                        contactName = decision.callerName,
                        contactNumber = decision.callerNumber,
                        platform = "CALL",
                        type = "SCREENED",
                        content = decision.screeningResultNote,
                        timestamp = System.currentTimeMillis(),
                        isSpam = decision.isSpam,
                        aiSummary = decision.screeningResultNote,
                        callDurationSeconds = 24
                    )
                )
            }

            "SEARCH_SAHARANPUR_NEWS" -> {
                searchAndFetchSaharanpurNews(result.actionParams)
            }

            "PUBLISH_SAHARANPUR_NEWS_FB" -> {
                val articleId = result.actionParams.toLongOrNull() ?: 0L
                publishNewsArticleToFacebook(articleId)
            }
        }
    }

    suspend fun simulateIncomingCall(phoneNumber: String, contactName: String?): CallScreenerManager.ScreeningDecision {
        val decision = callScreenerManager.screenIncomingCall(phoneNumber, contactName)
        dao.insertCommunicationLog(
            CommunicationLog(
                contactName = decision.callerName,
                contactNumber = decision.callerNumber,
                platform = "CALL",
                type = "SCREENED",
                content = decision.screeningResultNote,
                timestamp = System.currentTimeMillis(),
                isSpam = decision.isSpam,
                aiSummary = decision.screeningResultNote,
                callDurationSeconds = if (decision.isSpam) 15 else 45
            )
        )
        return decision
    }

    suspend fun addReminder(reminder: ReminderItem): Long {
        val id = dao.insertReminder(reminder)
        if (reminder.isSyncedToCalendar && !reminder.isLocationBased) {
            calendarManager.syncReminderToCalendar(reminder.title, reminder.timeInMillis, reminder.locationName)
        }
        return id
    }

    suspend fun toggleReminder(reminder: ReminderItem) {
        dao.updateReminder(reminder.copy(isCompleted = !reminder.isCompleted))
    }

    suspend fun deleteReminder(id: Long) {
        dao.deleteReminder(id)
    }

    suspend fun sendManualMessage(
        platform: String,
        contactName: String,
        contactNumber: String,
        message: String
    ) {
        dao.insertCommunicationLog(
            CommunicationLog(
                contactName = contactName,
                contactNumber = contactNumber,
                platform = platform.uppercase(),
                type = "OUTGOING",
                content = message,
                timestamp = System.currentTimeMillis(),
                aiSummary = "$platform sent to $contactName"
            )
        )

        when (platform.uppercase()) {
            "SMS" -> communicationManager.sendSms(contactNumber, message)
            "WHATSAPP" -> communicationManager.sendWhatsApp(contactNumber, message)
            "TELEGRAM" -> communicationManager.sendTelegram(contactNumber, message)
        }
    }

    fun getCalendarEvents(): List<CalendarEventItem> {
        return calendarManager.getDeviceCalendarEvents()
    }

    suspend fun generateDailyBriefing(): DailyBriefingData {
        val calendarEvents = calendarManager.getDeviceCalendarEvents()
        val allReminders = dao.getAllReminders().firstOrNull() ?: emptyList()
        val pendingReminders = allReminders.filter { !it.isCompleted }
        val logs = dao.getAllCommunicationLogs().firstOrNull() ?: emptyList()
        val missedCalls = logs.filter { it.type == "MISSED" || (it.platform == "CALL" && it.isSpam) }
        val unreadMessages = logs.filter { it.type == "INCOMING" }

        val todayDate = SimpleDateFormat("EEEE, d MMMM", Locale.forLanguageTag("hi-IN")).format(Date())

        val hindiSummary = buildString {
            append("शुभ प्रभात! आज $todayDate है। ")
            append("आज आपके पास ${calendarEvents.size} मीटिंग्स और ${pendingReminders.size} पेंडिंग रिमाइंडर हैं। ")
            if (missedCalls.isNotEmpty()) {
                append("AI ने ${missedCalls.size} स्पैम/अवांछित कॉल्स को स्क्रीन किया। ")
            }
            if (unreadMessages.isNotEmpty()) {
                append("${unreadMessages.size} नए संदेश आए हैं। आपका दिन शुभ और सफल रहे!")
            }
        }

        val englishSummary = buildString {
            append("Good morning! Today is $todayDate. ")
            append("You have ${calendarEvents.size} scheduled meetings and ${pendingReminders.size} active reminders. ")
            if (missedCalls.isNotEmpty()) {
                append("${missedCalls.size} spam or screened call(s) handled. ")
            }
            append("You are all set for a productive day!")
        }

        return DailyBriefingData(
            greetingHindi = "शुभ प्रभात! (Good Morning)",
            greetingEnglish = "Good Morning!",
            summaryTextHindi = hindiSummary,
            summaryTextEnglish = englishSummary,
            todayMeetingsCount = calendarEvents.size,
            pendingRemindersCount = pendingReminders.size,
            missedCallsCount = missedCalls.size,
            unreadMessagesCount = unreadMessages.size,
            events = calendarEvents,
            reminders = pendingReminders
        )
    }

    suspend fun clearHistory() {
        dao.clearChatMessages()
    }

    // Saharanpur News & Facebook Publishing
    fun getFacebookConfig(): FacebookPageConfig {
        return FacebookPageConfig(
            pageId = prefs?.getString("fb_page_id", "109876543210987") ?: "109876543210987",
            pageName = prefs?.getString("fb_page_name", "सहारनपुर न्यूज़ लाइव (Saharanpur News Live)") ?: "सहारनपुर न्यूज़ लाइव (Saharanpur News Live)",
            pageAccessToken = prefs?.getString("fb_page_token", "") ?: "",
            autoPublishEnabled = prefs?.getBoolean("fb_auto_publish", false) ?: false,
            includeHashtags = prefs?.getBoolean("fb_include_hashtags", true) ?: true,
            customFooterText = prefs?.getString("fb_custom_footer", "सहारनपुर की हर ताज़ा खबर के लिए हमारे पेज को फॉलो और शेयर करें!") ?: "सहारनपुर की हर ताज़ा खबर के लिए हमारे पेज को फॉलो और शेयर करें!"
        )
    }

    fun saveFacebookConfig(config: FacebookPageConfig) {
        prefs?.edit()
            ?.putString("fb_page_id", config.pageId)
            ?.putString("fb_page_name", config.pageName)
            ?.putString("fb_page_token", config.pageAccessToken)
            ?.putBoolean("fb_auto_publish", config.autoPublishEnabled)
            ?.putBoolean("fb_include_hashtags", config.includeHashtags)
            ?.putString("fb_custom_footer", config.customFooterText)
            ?.apply()
    }

    suspend fun searchAndFetchSaharanpurNews(query: String = ""): List<SaharanpurNewsEntity> {
        val fetched = SaharanpurNewsService.fetchLatestSaharanpurNews(query)
        if (fetched.isNotEmpty()) {
            dao.insertAllSaharanpurNews(fetched)
        }
        val config = getFacebookConfig()
        if (config.autoPublishEnabled && fetched.isNotEmpty()) {
            // Auto publish top news item if auto publish is turned on
            val top = fetched.first()
            publishNewsArticleToFacebook(top.id, top.formattedFbPost)
        }
        return fetched
    }

    suspend fun publishNewsArticleToFacebook(articleId: Long, customPostText: String? = null): FacebookPublishResult {
        val all = dao.getAllSaharanpurNews().first()
        val article = if (articleId != 0L) all.find { it.id == articleId } else all.firstOrNull()
        if (article == null) {
            return FacebookPublishResult(
                isSuccess = false,
                postId = null,
                message = "सहारनपुर का कोई समाचार नहीं मिला।"
            )
        }

        val config = getFacebookConfig()
        val postContent = customPostText ?: article.formattedFbPost
        val result = FacebookPublisherService.publishToFacebookPage(
            pageId = config.pageId,
            pageAccessToken = config.pageAccessToken,
            message = postContent,
            linkUrl = article.originalUrl
        )

        if (result.isSuccess) {
            dao.updateSaharanpurNews(
                article.copy(
                    isPublishedToFb = true,
                    publishedTimestamp = System.currentTimeMillis(),
                    fbPostId = result.postId
                )
            )
        }
        return result
    }

    suspend fun updateNewsArticle(article: SaharanpurNewsEntity) {
        dao.updateSaharanpurNews(article)
    }

    suspend fun deleteNewsArticle(id: Long) {
        dao.deleteSaharanpurNews(id)
    }
}
