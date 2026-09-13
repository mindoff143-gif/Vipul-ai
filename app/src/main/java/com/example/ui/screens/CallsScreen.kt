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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import com.example.ui.theme.ColorCall
import com.example.ui.theme.ColorSpam
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.IndigoPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CallsScreen(
    communicationLogs: List<CommunicationLog>,
    preferredLanguage: String,
    onSpeakText: (String) -> Unit,
    onOpenCallScreeningSimulator: () -> Unit,
    modifier: Modifier = Modifier
) {
    var autoScreenSpamEnabled by remember { mutableStateOf(true) }

    val callLogs = remember(communicationLogs) {
        communicationLogs.filter { it.platform == "CALL" }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // AI Smart Call Screening Dashboard Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(IndigoPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "स्मार्ट AI कॉल स्क्रीनर",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = if (autoScreenSpamEnabled) "सक्रिय (Auto-blocking active)" else "निष्क्रिय",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (autoScreenSpamEnabled) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }

                    Switch(
                        checked = autoScreenSpamEnabled,
                        onCheckedChange = { autoScreenSpamEnabled = it },
                        modifier = Modifier.testTag("auto_screen_toggle")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (preferredLanguage == "hi")
                        "अज्ञात या स्पैम कॉल्स आने पर AI स्वतः कॉल पिक करके कॉलर से उनका उद्देश्य पूछता है और स्पैम को ब्लॉक कर देता है।"
                    else
                        "When unknown or spam numbers ring, AI answers automatically, asks their intent, and blocks unwanted callers.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 17.sp
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Simulator Trigger Button
                Button(
                    onClick = onOpenCallScreeningSimulator,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("open_screening_simulator_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneInTalk,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "कॉल स्क्रीनिंग टेस्ट करें (Test Call Screening)",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (preferredLanguage == "hi") "कॉल हिस्ट्री व AI स्क्रीनिंग रिकॉर्ड (${callLogs.size})" else "Call History & AI Records (${callLogs.size})",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = ColorCall
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Calls list
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (callLogs.isEmpty()) {
                item {
                    EmptyStateNotice(
                        title = if (preferredLanguage == "hi") "कोई कॉल रिकॉर्ड नहीं मिला" else "No call records found",
                        subtitle = if (preferredLanguage == "hi") "ऊपर दिए टेस्ट बटन से AI स्क्रीनिंग का डेमो देखें।" else "Use the test button above to simulate call screening."
                    )
                }
            }

            items(callLogs, key = { it.id }) { log ->
                CallHistoryCard(
                    log = log,
                    preferredLanguage = preferredLanguage,
                    onListenScreening = {
                        val text = "कॉल स्क्रीनिंग सारांश: ${log.contactName} (${log.contactNumber}): ${log.aiSummary ?: log.content}"
                        onSpeakText(text)
                    }
                )
            }
        }
    }
}

@Composable
fun CallHistoryCard(
    log: CommunicationLog,
    preferredLanguage: String,
    onListenScreening: () -> Unit
) {
    val isSpam = log.isSpam
    val timeFormatted = SimpleDateFormat("hh:mm a, d MMM", Locale.getDefault()).format(Date(log.timestamp))

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSpam) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
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
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = if (isSpam) ColorSpam.copy(alpha = 0.15f) else ColorCall.copy(alpha = 0.15f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSpam) Icons.Default.Security else Icons.Default.Call,
                            contentDescription = null,
                            tint = if (isSpam) ColorSpam else ColorCall,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = log.contactName,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isSpam) ColorSpam else MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = log.contactNumber,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // AI Screening summary
            Surface(
                color = if (isSpam) ColorSpam.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = if (isSpam) "🛡️ AI स्पैम स्क्रीनिंग रिपोर्ट:" else "🤖 AI कॉल स्क्रीनिंग सारांश:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isSpam) ColorSpam else IndigoPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = log.aiSummary ?: log.content,
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
                    text = "अवधि: ${log.callDurationSeconds} सेकंड",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                )

                Button(
                    onClick = onListenScreening,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ColorCall.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = ColorCall,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ऑडियो सुनें", color = ColorCall, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
