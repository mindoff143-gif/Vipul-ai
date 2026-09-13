package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.components.CallScreeningDialog
import com.example.ui.components.DailyBriefingDialog
import com.example.ui.components.InviteFriendsDialog
import com.example.ui.components.NewMessageDialog
import com.example.ui.components.NewReminderDialog
import com.example.ui.screens.AssistantChatScreen
import com.example.ui.screens.CallsScreen
import com.example.ui.screens.ContactsScreen
import com.example.ui.screens.DownloadAppDialog
import com.example.ui.screens.MessagesHubScreen
import com.example.ui.screens.SaharanpurNewsScreen
import com.example.ui.screens.ScheduleScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.VioletTertiary
import com.example.viewmodel.AssistantViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: AssistantViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: AssistantViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Request permissions launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Permissions handled
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR
        )
        val notGranted = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (notGranted.isNotEmpty()) {
            permissionLauncher.launch(notGranted.toTypedArray())
        }
    }

    // State collections from ViewModel
    val chatMessages by viewModel.chatMessages.collectAsState()
    val communicationLogs by viewModel.communicationLogs.collectAsState()
    val reminders by viewModel.reminders.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val emails by viewModel.emails.collectAsState()
    val calendarEvents by viewModel.calendarEvents.collectAsState()
    val dailyBriefing by viewModel.dailyBriefing.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val soundLevel by viewModel.soundLevel.collectAsState()
    val preferredLanguage by viewModel.preferredLanguage.collectAsState()
    val selectedContact by viewModel.selectedContact.collectAsState()
    val lastActionNotice by viewModel.lastActionNotice.collectAsState()

    val saharanpurNews by viewModel.saharanpurNews.collectAsState()
    val isSearchingNews by viewModel.isSearchingNews.collectAsState()
    val isPublishingToFb by viewModel.isPublishingToFb.collectAsState()
    val facebookConfig by viewModel.facebookConfig.collectAsState()

    // Navigation tab: 0 = Assistant, 1 = Saharanpur News, 2 = Schedule, 3 = Messages, 4 = Calls, 5 = Contacts, 6 = Settings
    var selectedNavTab by remember { mutableIntStateOf(0) }

    // Dialog visibility states
    var showBriefingDialog by remember { mutableStateOf(false) }
    var showCallScreeningDialog by remember { mutableStateOf(false) }
    var showAddReminderDialog by remember { mutableStateOf(false) }
    var showComposeMessageDialog by remember { mutableStateOf(false) }
    var showDownloadDialog by remember { mutableStateOf(false) }
    var showInviteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(lastActionNotice) {
        lastActionNotice?.let { notice ->
            if (notice.contains("SHARE_INVITE_LINK") || notice.contains("इनवाइट") || notice.contains("डाउनलोड")) {
                showInviteDialog = true
            }
            coroutineScope.launch {
                snackbarHostState.showSnackbar(notice)
                viewModel.clearActionNotice()
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.testTag("app_top_bar_title")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    brush = Brush.linearGradient(listOf(IndigoPrimary, CyanSecondary)),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Personal AI Assistant",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            )
                            Text(
                                text = if (preferredLanguage == "hi") "पर्सनल AI असिस्टेंट" else "AI Voice & Call Manager",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                },
                actions = {
                    // Language Switcher Chip (Tap to quickly toggle Hindi / English)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .clickable {
                                val nextLang = if (preferredLanguage == "hi") "en" else "hi"
                                viewModel.setPreferredLanguage(nextLang)
                            }
                            .testTag("language_toggle_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = IndigoPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (preferredLanguage == "hi") "🇮🇳 हिंदी" else "🇬🇧 EN",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Invite Friends & Share APK Button
                    IconButton(
                        onClick = { showInviteDialog = true },
                        modifier = Modifier.testTag("top_bar_invite_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Invite Friends & Share APK",
                            tint = Color(0xFF10B981)
                        )
                    }

                    // Download APK Button
                    IconButton(
                        onClick = { showDownloadDialog = true },
                        modifier = Modifier.testTag("download_apk_top_bar_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download App APK",
                            tint = IndigoPrimary
                        )
                    }

                    // Daily Briefing Button
                    IconButton(
                        onClick = {
                            viewModel.loadDailyBriefing()
                            showBriefingDialog = true
                        },
                        modifier = Modifier.testTag("daily_briefing_top_button")
                    ) {
                        BadgedBox(
                            badge = {
                                Badge(containerColor = CyanSecondary) {
                                    Text(calendarEvents.size.toString())
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.WbSunny,
                                contentDescription = "Daily Briefing",
                                tint = Color(0xFFF59E0B)
                            )
                        }
                    }

                    // Stop Speaking Button if speaking
                    if (isSpeaking) {
                        IconButton(
                            onClick = { viewModel.stopSpeaking() },
                            modifier = Modifier.testTag("stop_speaking_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop Speech",
                                tint = VioletTertiary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationItem(
                    selected = selectedNavTab == 0,
                    onClick = { selectedNavTab = 0 },
                    icon = Icons.AutoMirrored.Filled.Chat,
                    label = if (preferredLanguage == "hi") "AI चैट" else "Chat",
                    testTag = "nav_chat_tab"
                )
                NavigationItem(
                    selected = selectedNavTab == 1,
                    onClick = { selectedNavTab = 1 },
                    icon = Icons.Default.Newspaper,
                    label = if (preferredLanguage == "hi") "सहारनपुर" else "News & FB",
                    testTag = "nav_saharanpur_news_tab"
                )
                NavigationItem(
                    selected = selectedNavTab == 2,
                    onClick = { selectedNavTab = 2 },
                    icon = Icons.Default.CalendarMonth,
                    label = if (preferredLanguage == "hi") "शेड्यूल" else "Schedule",
                    testTag = "nav_schedule_tab"
                )
                NavigationItem(
                    selected = selectedNavTab == 3,
                    onClick = { selectedNavTab = 3 },
                    icon = Icons.AutoMirrored.Filled.Message,
                    label = if (preferredLanguage == "hi") "मेमोरी" else "Messages",
                    testTag = "nav_messages_tab"
                )
                NavigationItem(
                    selected = selectedNavTab == 4,
                    onClick = { selectedNavTab = 4 },
                    icon = Icons.Default.PhoneInTalk,
                    label = if (preferredLanguage == "hi") "कॉल स्क्रीन" else "Calls",
                    testTag = "nav_calls_tab"
                )
                NavigationItem(
                    selected = selectedNavTab == 5,
                    onClick = { selectedNavTab = 5 },
                    icon = Icons.Default.Settings,
                    label = if (preferredLanguage == "hi") "सेटिंग्स" else "Settings",
                    testTag = "nav_settings_tab"
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedNavTab) {
                0 -> AssistantChatScreen(
                    messages = chatMessages,
                    isListening = isListening,
                    isSpeaking = isSpeaking,
                    isProcessing = isProcessing,
                    soundLevel = soundLevel,
                    preferredLanguage = preferredLanguage,
                    onStartListening = { viewModel.startListening() },
                    onStopListening = { viewModel.stopListening() },
                    onSendQuery = { query -> viewModel.sendUserQuery(query) },
                    onSpeakText = { text -> viewModel.speakText(text) },
                    onStopSpeech = { viewModel.stopSpeaking() }
                )

                1 -> SaharanpurNewsScreen(
                    newsArticles = saharanpurNews,
                    facebookConfig = facebookConfig,
                    isSearchingNews = isSearchingNews,
                    isPublishingToFb = isPublishingToFb,
                    preferredLanguage = preferredLanguage,
                    onSearchNews = { query -> viewModel.searchSaharanpurNews(query) },
                    onPublishToFacebook = { article, customText ->
                        viewModel.publishNewsToFacebook(article, customText)
                    },
                    onShareViaIntent = { ctx, article ->
                        viewModel.shareNewsViaIntent(ctx, article)
                    },
                    onSaveFacebookConfig = { config ->
                        viewModel.saveFacebookConfig(config)
                    },
                    onSpeakNews = { text -> viewModel.speakText(text) },
                    onShowDownloadDialog = { showDownloadDialog = true }
                )

                2 -> ScheduleScreen(
                    calendarEvents = calendarEvents,
                    reminders = reminders,
                    preferredLanguage = preferredLanguage,
                    onToggleReminder = { reminder -> viewModel.toggleReminder(reminder) },
                    onDeleteReminder = { id -> viewModel.deleteReminder(id) },
                    onOpenAddReminder = { showAddReminderDialog = true },
                    onRefreshCalendar = { viewModel.refreshCalendarEvents() }
                )

                3 -> MessagesHubScreen(
                    communicationLogs = communicationLogs,
                    emails = emails,
                    preferredLanguage = preferredLanguage,
                    onSpeakText = { text -> viewModel.speakText(text) },
                    onReadEmail = { email -> viewModel.readEmailSummary(email) },
                    onOpenComposeMessage = { showComposeMessageDialog = true }
                )

                4 -> CallsScreen(
                    communicationLogs = communicationLogs,
                    preferredLanguage = preferredLanguage,
                    onSpeakText = { text -> viewModel.speakText(text) },
                    onOpenCallScreeningSimulator = { showCallScreeningDialog = true }
                )

                5 -> SettingsScreen(
                    preferredLanguage = preferredLanguage,
                    onLanguageChange = { lang -> viewModel.setPreferredLanguage(lang) },
                    onClearChat = { viewModel.clearChat() },
                    onDownloadAppClick = { showDownloadDialog = true }
                )
            }
        }
    }

    // Modal Dialogs
    if (showBriefingDialog && dailyBriefing != null) {
        DailyBriefingDialog(
            briefing = dailyBriefing!!,
            preferredLanguage = preferredLanguage,
            onDismiss = { showBriefingDialog = false },
            onPlayAloud = { viewModel.playDailyBriefingAloud() }
        )
    }

    if (showCallScreeningDialog) {
        CallScreeningDialog(
            preferredLanguage = preferredLanguage,
            onDismiss = { showCallScreeningDialog = false },
            onSimulateScreening = { number, name ->
                viewModel.simulateCallScreening(number, name)
            }
        )
    }

    if (showAddReminderDialog) {
        NewReminderDialog(
            preferredLanguage = preferredLanguage,
            onDismiss = { showAddReminderDialog = false },
            onSaveReminder = { title, time, isLoc, locName ->
                viewModel.addCustomReminder(title, time, isLoc, locName)
            }
        )
    }

    if (showComposeMessageDialog) {
        NewMessageDialog(
            preferredLanguage = preferredLanguage,
            onDismiss = { showComposeMessageDialog = false },
            onSendMessage = { platform, name, number, msg ->
                viewModel.sendManualMessage(platform, name, number, msg)
            }
        )
    }

    if (showDownloadDialog) {
        DownloadAppDialog(
            onDismiss = { showDownloadDialog = false }
        )
    }
}

@Composable
fun androidx.compose.foundation.layout.RowScope.NavigationItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    testTag: String
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(imageVector = icon, contentDescription = label) },
        label = { Text(text = label, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = IndigoPrimary,
            selectedTextColor = IndigoPrimary,
            indicatorColor = IndigoPrimary.copy(alpha = 0.15f)
        ),
        modifier = Modifier.testTag(testTag)
    )
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
