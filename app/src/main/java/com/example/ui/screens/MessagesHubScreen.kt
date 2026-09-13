package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CommunicationLog
import com.example.data.model.EmailSummaryItem
import com.example.ui.theme.ColorGmail
import com.example.ui.theme.ColorSms
import com.example.ui.theme.ColorTelegram
import com.example.ui.theme.ColorWhatsApp
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.IndigoPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MessagesHubScreen(
    communicationLogs: List<CommunicationLog>,
    emails: List<EmailSummaryItem>,
    preferredLanguage: String,
    onSpeakText: (String) -> Unit,
    onReadEmail: (EmailSummaryItem) -> Unit,
    onOpenComposeMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, SMS, WHATSAPP, TELEGRAM, GMAIL
    var memorySearchQuery by remember { mutableStateOf("") }

    val filteredLogs = remember(communicationLogs, selectedFilter, memorySearchQuery) {
        val nonCalls = communicationLogs.filter { it.platform != "CALL" }
        val platformFiltered = when (selectedFilter) {
            "SMS" -> nonCalls.filter { it.platform == "SMS" }
            "WHATSAPP" -> nonCalls.filter { it.platform == "WHATSAPP" }
            "TELEGRAM" -> nonCalls.filter { it.platform == "TELEGRAM" }
            else -> nonCalls
        }
        if (memorySearchQuery.isBlank()) {
            platformFiltered
        } else {
            platformFiltered.filter {
                it.contactName.contains(memorySearchQuery, ignoreCase = true) ||
                        it.content.contains(memorySearchQuery, ignoreCase = true) ||
                        it.platform.contains(memorySearchQuery, ignoreCase = true)
            }
        }
    }

    val filteredEmails = remember(emails, memorySearchQuery) {
        if (memorySearchQuery.isBlank()) {
            emails
        } else {
            emails.filter {
                it.sender.contains(memorySearchQuery, ignoreCase = true) ||
                        it.subject.contains(memorySearchQuery, ignoreCase = true) ||
                        it.aiSummaryHindi.contains(memorySearchQuery, ignoreCase = true) ||
                        it.aiSummaryEnglish.contains(memorySearchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Top Row: Memory Query Box + Compose Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = memorySearchQuery,
                onValueChange = { memorySearchQuery = it },
                placeholder = {
                    Text(
                        text = if (preferredLanguage == "hi") "किसने क्या कहा था? (खोजें)..." else "Who said what? (Search memory)...",
                        fontSize = 13.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = IndigoPrimary
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("memory_search_input"),
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = IndigoPrimary
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onOpenComposeMessage,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                modifier = Modifier.testTag("compose_message_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("भेजें", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Platform Filter chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedFilter == "ALL",
                onClick = { selectedFilter = "ALL" },
                label = { Text("सभी (All)", fontSize = 11.sp) }
            )
            FilterChip(
                selected = selectedFilter == "SMS",
                onClick = { selectedFilter = "SMS" },
                label = { Text("SMS", fontSize = 11.sp) }
            )
            FilterChip(
                selected = selectedFilter == "WHATSAPP",
                onClick = { selectedFilter = "WHATSAPP" },
                label = { Text("WhatsApp", fontSize = 11.sp) }
            )
            FilterChip(
                selected = selectedFilter == "TELEGRAM",
                onClick = { selectedFilter = "TELEGRAM" },
                label = { Text("Telegram", fontSize = 11.sp) }
            )
            FilterChip(
                selected = selectedFilter == "GMAIL",
                onClick = { selectedFilter = "GMAIL" },
                label = { Text("Gmail (${emails.size})", fontSize = 11.sp) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Items List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // If Gmail or ALL selected, show Gmail summaries
            if (selectedFilter == "GMAIL" || (selectedFilter == "ALL" && filteredEmails.isNotEmpty())) {
                item {
                    Text(
                        text = if (preferredLanguage == "hi") "Gmail ईमेल सारांश (AI Email Summaries)" else "Gmail AI Email Summaries",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = ColorGmail
                        ),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                items(filteredEmails, key = { "email_${it.id}" }) { email ->
                    EmailSummaryCard(
                        email = email,
                        preferredLanguage = preferredLanguage,
                        onReadAloud = { onReadEmail(email) }
                    )
                }

                if (selectedFilter != "GMAIL") {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (preferredLanguage == "hi") "मैसेज संचार मेमोरी (SMS, WhatsApp, Telegram)" else "Communication Memory (SMS, WhatsApp, Telegram)",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = IndigoPrimary
                            ),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }

            if (selectedFilter != "GMAIL") {
                if (filteredLogs.isEmpty() && filteredEmails.isEmpty()) {
                    item {
                        EmptyStateNotice(
                            title = if (preferredLanguage == "hi") "कोई संदेश नहीं मिला" else "No messages found",
                            subtitle = if (preferredLanguage == "hi") "ऊपर 'भेजें' बटन से नया SMS/WhatsApp भेजें।" else "Tap 'Send' to dispatch a new message."
                        )
                    }
                }

                items(filteredLogs, key = { "log_${it.id}" }) { log ->
                    MessageLogCard(
                        log = log,
                        preferredLanguage = preferredLanguage,
                        onSpeak = {
                            val text = "${log.platform} from ${log.contactName}: ${log.content}"
                            onSpeakText(text)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MessageLogCard(
    log: CommunicationLog,
    preferredLanguage: String,
    onSpeak: () -> Unit
) {
    val platformColor = when (log.platform) {
        "WHATSAPP" -> ColorWhatsApp
        "TELEGRAM" -> ColorTelegram
        "SMS" -> ColorSms
        else -> IndigoPrimary
    }

    val timeFormatted = SimpleDateFormat("hh:mm a, d MMM", Locale.getDefault()).format(Date(log.timestamp))

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = platformColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = log.platform,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = platformColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = log.contactName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = log.content,
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 20.sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (log.type == "INCOMING") "प्राप्त (Incoming)" else "भेजा गया (Outgoing)",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (log.type == "INCOMING") CyanSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                )

                IconButton(
                    onClick = onSpeak,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Read aloud",
                        tint = IndigoPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmailSummaryCard(
    email: EmailSummaryItem,
    preferredLanguage: String,
    onReadAloud: () -> Unit
) {
    val timeFormatted = SimpleDateFormat("hh:mm a, d MMM", Locale.getDefault()).format(Date(email.timestamp))

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = ColorGmail.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = ColorGmail,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "GMAIL",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = ColorGmail,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = email.sender,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = email.subject,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            // AI summary box
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "AI समरी (Summary):",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = IndigoPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (preferredLanguage == "hi") email.aiSummaryHindi else email.aiSummaryEnglish,
                        style = MaterialTheme.typography.bodySmall.copy(
                            lineHeight = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (email.isRead) "पढ़ा हुआ (Read)" else "नया ईमेल (New)",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (email.isRead) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF10B981),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )

                IconButton(
                    onClick = onReadAloud,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Listen to email summary",
                        tint = ColorGmail,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
