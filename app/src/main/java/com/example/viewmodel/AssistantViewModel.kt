package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.AssistantChatMessage
import com.example.data.model.CalendarEventItem
import com.example.data.model.CommunicationLog
import com.example.data.model.ContactRecord
import com.example.data.model.DailyBriefingData
import com.example.data.model.EmailSummaryItem
import com.example.data.model.FacebookPageConfig
import com.example.data.model.ReminderItem
import com.example.data.model.SaharanpurNewsEntity
import com.example.data.repository.AssistantRepository
import com.example.service.CalendarManager
import com.example.service.CallScreenerManager
import com.example.service.CommunicationManager
import com.example.service.FacebookPublisherService
import com.example.service.SpeechManager
import com.example.service.TtsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val communicationManager = CommunicationManager(application)
    private val calendarManager = CalendarManager(application)
    private val callScreenerManager = CallScreenerManager(application)

    private val repository = AssistantRepository(
        dao = database.assistantDao(),
        communicationManager = communicationManager,
        calendarManager = calendarManager,
        callScreenerManager = callScreenerManager,
        context = application
    )

    val ttsManager = TtsManager(application)
    private var speechManager: SpeechManager? = null

    // UI States
    val chatMessages: StateFlow<List<AssistantChatMessage>> = repository.chatMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val communicationLogs: StateFlow<List<CommunicationLog>> = repository.communicationLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reminders: StateFlow<List<ReminderItem>> = repository.reminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val contacts: StateFlow<List<ContactRecord>> = repository.contacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val emails: StateFlow<List<EmailSummaryItem>> = repository.emails
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val saharanpurNews: StateFlow<List<SaharanpurNewsEntity>> = repository.saharanpurNews
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isSearchingNews = MutableStateFlow(false)
    val isSearchingNews: StateFlow<Boolean> = _isSearchingNews.asStateFlow()

    private val _isPublishingToFb = MutableStateFlow(false)
    val isPublishingToFb: StateFlow<Boolean> = _isPublishingToFb.asStateFlow()

    private val _facebookConfig = MutableStateFlow(repository.getFacebookConfig())
    val facebookConfig: StateFlow<FacebookPageConfig> = _facebookConfig.asStateFlow()

    private val _calendarEvents = MutableStateFlow<List<CalendarEventItem>>(emptyList())
    val calendarEvents: StateFlow<List<CalendarEventItem>> = _calendarEvents.asStateFlow()

    private val _dailyBriefing = MutableStateFlow<DailyBriefingData?>(null)
    val dailyBriefing: StateFlow<DailyBriefingData?> = _dailyBriefing.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _soundLevel = MutableStateFlow(0f)
    val soundLevel: StateFlow<Float> = _soundLevel.asStateFlow()

    val isSpeaking: StateFlow<Boolean> = ttsManager.isSpeaking

    private val _preferredLanguage = MutableStateFlow("hi") // "hi" = Hindi default, "en" = English
    val preferredLanguage: StateFlow<String> = _preferredLanguage.asStateFlow()

    private val _selectedContact = MutableStateFlow<ContactRecord?>(null)
    val selectedContact: StateFlow<ContactRecord?> = _selectedContact.asStateFlow()

    private val _lastActionNotice = MutableStateFlow<String?>(null)
    val lastActionNotice: StateFlow<String?> = _lastActionNotice.asStateFlow()

    init {
        speechManager = SpeechManager(application) { recognizedSpeech ->
            onVoiceInputReceived(recognizedSpeech)
        }

        viewModelScope.launch {
            speechManager?.isListening?.collect { _isListening.value = it }
        }
        viewModelScope.launch {
            speechManager?.soundLevel?.collect { _soundLevel.value = it }
        }

        refreshCalendarEvents()
        loadDailyBriefing()
    }

    fun setPreferredLanguage(lang: String) {
        _preferredLanguage.value = lang
        ttsManager.setLanguage(lang)
    }

    fun startListening() {
        ttsManager.stop()
        val langCode = if (_preferredLanguage.value == "hi") "hi-IN" else "en-IN"
        speechManager?.startListening(langCode)
    }

    fun stopListening() {
        speechManager?.stopListening()
    }

    private fun onVoiceInputReceived(spokenText: String) {
        if (spokenText.isNotBlank()) {
            sendUserQuery(spokenText)
        }
    }

    fun sendUserQuery(prompt: String) {
        if (prompt.isBlank() || _isProcessing.value) return
        _isProcessing.value = true

        viewModelScope.launch {
            try {
                val result = repository.processUserCommand(prompt, _preferredLanguage.value)
                _lastActionNotice.value = if (result.actionType != "GENERAL_AI") "Action: ${result.actionType}" else null
                // By default reply by voice!
                ttsManager.speak(result.responseText, _preferredLanguage.value)
            } catch (e: Exception) {
                _lastActionNotice.value = "Error: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun speakText(text: String) {
        ttsManager.speak(text, _preferredLanguage.value)
    }

    fun stopSpeaking() {
        ttsManager.stop()
    }

    fun refreshCalendarEvents() {
        _calendarEvents.value = repository.getCalendarEvents()
    }

    fun loadDailyBriefing() {
        viewModelScope.launch {
            _dailyBriefing.value = repository.generateDailyBriefing()
        }
    }

    fun playDailyBriefingAloud() {
        val briefing = _dailyBriefing.value ?: return
        val speech = if (_preferredLanguage.value == "hi") briefing.summaryTextHindi else briefing.summaryTextEnglish
        ttsManager.speak(speech, _preferredLanguage.value)
    }

    fun toggleReminder(reminder: ReminderItem) {
        viewModelScope.launch {
            repository.toggleReminder(reminder)
        }
    }

    fun addCustomReminder(title: String, timeInMillis: Long, isLocation: Boolean, location: String?) {
        viewModelScope.launch {
            val reminder = ReminderItem(
                title = title,
                timeInMillis = timeInMillis,
                isLocationBased = isLocation,
                locationName = location,
                isCompleted = false,
                isSyncedToCalendar = true
            )
            repository.addReminder(reminder)
            val msg = if (_preferredLanguage.value == "hi") "रिमाइंडर सुरक्षित कर लिया गया है" else "Reminder saved"
            _lastActionNotice.value = msg
            ttsManager.speak(msg, _preferredLanguage.value)
        }
    }

    fun deleteReminder(id: Long) {
        viewModelScope.launch {
            repository.deleteReminder(id)
        }
    }

    fun simulateCallScreening(phoneNumber: String, name: String? = null) {
        viewModelScope.launch {
            val decision = repository.simulateIncomingCall(phoneNumber, name)
            _lastActionNotice.value = "कॉल स्क्रीन: ${decision.callerName} (${decision.actionTaken})"
            ttsManager.speak(decision.aiGreetingSpeech, _preferredLanguage.value)
        }
    }

    fun sendManualMessage(platform: String, contactName: String, number: String, text: String) {
        viewModelScope.launch {
            repository.sendManualMessage(platform, contactName, number, text)
            val notice = if (_preferredLanguage.value == "hi") "$contactName को $platform भेजा गया" else "$platform sent to $contactName"
            _lastActionNotice.value = notice
            ttsManager.speak(notice, _preferredLanguage.value)
        }
    }

    fun selectContact(contact: ContactRecord?) {
        _selectedContact.value = contact
    }

    fun readEmailSummary(email: EmailSummaryItem) {
        viewModelScope.launch {
            database.assistantDao().markEmailAsRead(email.id)
            val summary = if (_preferredLanguage.value == "hi") email.aiSummaryHindi else email.aiSummaryEnglish
            ttsManager.speak("${email.sender} से ईमेल समरी: $summary", _preferredLanguage.value)
        }
    }

    fun clearActionNotice() {
        _lastActionNotice.value = null
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    // Saharanpur News & Facebook Publishing
    fun searchSaharanpurNews(query: String = "") {
        if (_isSearchingNews.value) return
        _isSearchingNews.value = true
        viewModelScope.launch {
            try {
                val results = repository.searchAndFetchSaharanpurNews(query)
                val msg = if (_preferredLanguage.value == "hi") {
                    "सहारनपुर की ${results.size} ताज़ा ख़बरें प्राप्त हुईं।"
                } else {
                    "Fetched ${results.size} latest news stories for Saharanpur."
                }
                _lastActionNotice.value = msg
            } catch (e: Exception) {
                _lastActionNotice.value = "न्यूज़ खोजने में त्रुटि: ${e.message}"
            } finally {
                _isSearchingNews.value = false
            }
        }
    }

    fun publishNewsToFacebook(article: SaharanpurNewsEntity, customText: String? = null) {
        if (_isPublishingToFb.value) return
        _isPublishingToFb.value = true
        viewModelScope.launch {
            try {
                val result = repository.publishNewsArticleToFacebook(article.id, customText)
                _lastActionNotice.value = result.message
                if (result.isSuccess) {
                    ttsManager.speak(
                        if (_preferredLanguage.value == "hi") "सहारनपुर समाचार आपके फेसबुक पेज पर पब्लिश कर दिया गया है!" else "Saharanpur news published to your Facebook page!",
                        _preferredLanguage.value
                    )
                }
            } catch (e: Exception) {
                _lastActionNotice.value = "फेसबुक पब्लिशिंग त्रुटि: ${e.message}"
            } finally {
                _isPublishingToFb.value = false
            }
        }
    }

    fun shareNewsViaIntent(context: android.content.Context, article: SaharanpurNewsEntity) {
        FacebookPublisherService.shareViaFacebookApp(context, article.formattedFbPost, article.originalUrl)
        _lastActionNotice.value = "फेसबुक शेयर विंडो खोली गई"
    }

    fun saveFacebookConfig(config: FacebookPageConfig) {
        repository.saveFacebookConfig(config)
        _facebookConfig.value = config
        _lastActionNotice.value = "फेसबुक पेज सेटिंग्स सुरक्षित की गईं"
    }

    fun updateNewsArticle(article: SaharanpurNewsEntity) {
        viewModelScope.launch {
            repository.updateNewsArticle(article)
        }
    }

    fun deleteNewsArticle(id: Long) {
        viewModelScope.launch {
            repository.deleteNewsArticle(id)
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechManager?.destroy()
        ttsManager.release()
    }
}
