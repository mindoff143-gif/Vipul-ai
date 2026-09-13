package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AssistantChatMessage
import com.example.service.AppInviteService
import com.example.ui.components.VoiceWaveformBar
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.VioletTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AssistantChatScreen(
    messages: List<AssistantChatMessage>,
    isListening: Boolean,
    isSpeaking: Boolean,
    isProcessing: Boolean,
    soundLevel: Float,
    preferredLanguage: String,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onSendQuery: (String) -> Unit,
    onSpeakText: (String) -> Unit,
    onStopSpeech: () -> Unit,
    onOpenInviteDialog: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val quickPrompts = if (preferredLanguage == "hi") {
        listOf(
            "दोस्तों को इनवाइट करो (Invite Link)",
            "सहारनपुर की ताज़ा खबरें खोजो",
            "सहारनपुर न्यूज़ फेसबुक पेज पर पब्लिश करो",
            "आज का शेड्यूल क्या है?",
            "माँ को SMS भेजो",
            "राहुल ने क्या कहा था?",
            "ऑफिस पहुँचते ही फाइल लेने का रिमाइंडर सेट करो",
            "कोई नया ईमेल आया क्या?",
            "कॉल स्क्रीन टेस्ट करो"
        )
    } else {
        listOf(
            "Invite friends (Direct APK Link)",
            "Search Saharanpur news & post to Facebook",
            "What's on today's schedule?",
            "Send SMS to Mom",
            "Who said what?",
            "Remind me when I reach office to submit bills",
            "Summarize my latest emails",
            "Screen spam calls"
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding()
    ) {
        // Voice Waveform & Speech indicator bar
        VoiceWaveformBar(
            isListening = isListening,
            isSpeaking = isSpeaking,
            soundLevel = soundLevel,
            statusText = if (isListening) "आप जो बोलेंगे, AI उसे समझेगा और पूरा करेगा..." else "AI उत्तर सुना रहा है..."
        )

        // Chat messages stream
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    EmptyAssistantPlaceholder(preferredLanguage = preferredLanguage)
                }
            }

            items(messages, key = { it.id }) { message ->
                ChatMessageBubble(
                    message = message,
                    isSpeaking = isSpeaking,
                    onSpeakText = { onSpeakText(message.text) },
                    onStopSpeech = onStopSpeech,
                    onOpenInviteDialog = onOpenInviteDialog
                )
            }

            if (isProcessing) {
                item {
                    AiThinkingBubble(preferredLanguage = preferredLanguage)
                }
            }
        }

        // Quick prompts row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickPrompts) { prompt ->
                FilterChip(
                    selected = false,
                    onClick = { onSendQuery(prompt) },
                    label = { Text(prompt, fontSize = 12.sp) },
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }

        // Input & Voice console bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mic Button (Tap to toggle voice recognition)
                IconButton(
                    onClick = {
                        if (isListening) {
                            onStopListening()
                        } else {
                            onStartListening()
                        }
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            brush = if (isListening) {
                                Brush.linearGradient(listOf(Color(0xFFEF4444), Color(0xFFDC2626)))
                            } else {
                                Brush.linearGradient(listOf(IndigoPrimary, CyanSecondary))
                            },
                            shape = CircleShape
                        )
                        .testTag("voice_mic_button")
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Voice Input",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Text Input field
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            text = if (preferredLanguage == "hi") "बोलें या यहाँ निर्देश/सवाल लिखें..." else "Speak or type anything...",
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IndigoPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    maxLines = 3,
                    trailingIcon = {
                        if (isSpeaking) {
                            IconButton(onClick = onStopSpeech) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Stop Speech",
                                    tint = VioletTertiary
                                )
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Send button
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            val textToSend = inputText
                            inputText = ""
                            onSendQuery(textToSend)
                        }
                    },
                    enabled = inputText.isNotBlank() && !isProcessing,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (inputText.isNotBlank()) IndigoPrimary else MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape
                        )
                        .testTag("send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (inputText.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatMessageBubble(
    message: AssistantChatMessage,
    isSpeaking: Boolean,
    onSpeakText: () -> Unit,
    onStopSpeech: () -> Unit,
    onOpenInviteDialog: () -> Unit = {}
) {
    val isUser = message.sender == "USER"
    val alignment = if (isUser) Alignment.End else Alignment.Start

    val timeFormatted = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(message.timestamp))

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isUser) 18.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 18.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) {
                    IndigoPrimary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isUser) 2.dp else 1.dp),
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .testTag(if (isUser) "user_message_bubble" else "assistant_message_bubble")
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Action tag if present
                if (!message.actionType.isNullOrBlank() && message.actionType != "WELCOME") {
                    Surface(
                        color = CyanSecondary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = "⚡ ACTION: ${message.actionType}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = CyanSecondary,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Message Text
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 21.sp,
                        color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                )

                // If Invite / APK download link is in message, show quick share/download buttons!
                if (!isUser && (message.actionType == "SHARE_INVITE_LINK" || message.text.contains("personal-ai-assistant.apk") || message.text.contains("डाउनलोड") || message.text.contains("इनवाइट"))) {
                    val context = LocalContext.current
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { AppInviteService.shareViaWhatsApp(context) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chat_whatsapp_invite_button")
                            ) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("WhatsApp इनवाइट", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { AppInviteService.downloadApkDirectly(context) },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chat_direct_download_button")
                            ) {
                                Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("APK डाउनलोड", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedButton(
                            onClick = onOpenInviteDialog,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("chat_open_invite_options_button")
                        ) {
                            Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("सभी इनवाइट व शेयरिंग विकल्प", fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom row: Time + TTS Audio button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeFormatted,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = if (isUser) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    if (!isUser) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onSpeakText,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Listen to response",
                                    tint = VioletTertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AiThinkingBubble(preferredLanguage: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 6.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = IndigoPrimary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (preferredLanguage == "hi") "AI सोच रहा है और प्रोसेस कर रहा है..." else "AI is processing...",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

@Composable
fun EmptyAssistantPlaceholder(preferredLanguage: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    brush = Brush.radialGradient(listOf(IndigoPrimary, CyanSecondary)),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (preferredLanguage == "hi") "पर्सनल AI असिस्टेंट सक्रिय है" else "Personal AI Assistant Active",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (preferredLanguage == "hi") "कॉल, SMS, WhatsApp, Telegram, ईमेल और शेड्यूल प्रबंधित करने के लिए बोलें या टाइप करें।" else "Speak or type to manage calls, SMS, WhatsApp, Telegram, emails, and schedule.",
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.padding(horizontal = 24.dp),
            lineHeight = 18.sp
        )
    }
}
