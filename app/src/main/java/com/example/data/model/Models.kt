package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "assistant_messages")
data class AssistantChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String, // "USER" or "ASSISTANT"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val language: String = "hi", // "hi" or "en"
    val actionType: String? = null,
    val actionDetails: String? = null
)

@Entity(tableName = "communication_logs")
data class CommunicationLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactName: String,
    val contactNumber: String,
    val platform: String, // "SMS", "WHATSAPP", "TELEGRAM", "CALL", "GMAIL"
    val type: String, // "INCOMING", "OUTGOING", "MISSED", "SCREENED"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSpam: Boolean = false,
    val aiSummary: String? = null,
    val callDurationSeconds: Int = 0
)

@Entity(tableName = "reminders")
data class ReminderItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val timeInMillis: Long = 0L,
    val isLocationBased: Boolean = false,
    val locationName: String? = null,
    val contactName: String? = null,
    val isCompleted: Boolean = false,
    val isSyncedToCalendar: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "contacts")
data class ContactRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val email: String? = null,
    val relation: String = "Contact",
    val isSpamOrBlocked: Boolean = false,
    val notes: String? = null,
    val lastInteracted: Long = System.currentTimeMillis()
)

@Entity(tableName = "email_summaries")
data class EmailSummaryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String,
    val subject: String,
    val snippet: String,
    val aiSummaryHindi: String,
    val aiSummaryEnglish: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

data class CalendarEventItem(
    val id: Long,
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val location: String? = null,
    val description: String? = null
)

data class DailyBriefingData(
    val greetingHindi: String,
    val greetingEnglish: String,
    val summaryTextHindi: String,
    val summaryTextEnglish: String,
    val todayMeetingsCount: Int,
    val pendingRemindersCount: Int,
    val missedCallsCount: Int,
    val unreadMessagesCount: Int,
    val events: List<CalendarEventItem>,
    val reminders: List<ReminderItem>
)

@Entity(tableName = "saharanpur_news")
data class SaharanpurNewsEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val summaryHindi: String,
    val category: String, // "ताज़ा ख़बर", "विकास व स्मार्ट सिटी", "शिक्षा व विश्वविद्यालय", "किसान व कृषि", "प्रशासन व पुलिस"
    val source: String,
    val originalUrl: String,
    val publishedDate: String,
    val timestamp: Long = System.currentTimeMillis(),
    val formattedFbPost: String,
    val isPublishedToFb: Boolean = false,
    val publishedTimestamp: Long = 0L,
    val fbPostId: String? = null
)

data class FacebookPageConfig(
    val pageId: String = "109876543210987",
    val pageName: String = "सहारनपुर न्यूज़ लाइव (Saharanpur News Live)",
    val pageAccessToken: String = "",
    val autoPublishEnabled: Boolean = false,
    val includeHashtags: Boolean = true,
    val customFooterText: String = "सहारनपुर की हर ताज़ा खबर के लिए हमारे पेज को फॉलो और शेयर करें!"
)

data class FacebookPublishResult(
    val isSuccess: Boolean,
    val postId: String? = null,
    val message: String,
    val isSimulatedOrIntent: Boolean = false
)

