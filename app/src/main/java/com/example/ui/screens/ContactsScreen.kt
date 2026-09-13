package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.CommunicationLog
import com.example.data.model.ContactRecord
import com.example.data.model.ReminderItem
import com.example.ui.theme.ColorCall
import com.example.ui.theme.ColorSms
import com.example.ui.theme.ColorTelegram
import com.example.ui.theme.ColorWhatsApp
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.IndigoPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ContactsScreen(
    contacts: List<ContactRecord>,
    communicationLogs: List<CommunicationLog>,
    reminders: List<ReminderItem>,
    selectedContact: ContactRecord?,
    preferredLanguage: String,
    onSelectContact: (ContactRecord?) -> Unit,
    onQuickCall: (String) -> Unit,
    onQuickSms: (name: String, number: String) -> Unit,
    onQuickWhatsApp: (name: String, number: String) -> Unit,
    onSpeakHistory: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (preferredLanguage == "hi") "कॉन्टैक्ट्स व संपूर्ण मेमोरी रिकॉर्ड (${contacts.size})" else "Contacts & Unified History Dossier (${contacts.size})",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            text = if (preferredLanguage == "hi") "किसी भी व्यक्ति का नाम चुनें या बोलें: उनकी सभी कॉल्स, SMS, WhatsApp और नोट्स एक जगह देखें।" else "Tap any contact to see their complete cross-platform history and memory.",
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(contacts, key = { it.id }) { contact ->
                ContactItemCard(
                    contact = contact,
                    preferredLanguage = preferredLanguage,
                    onClick = { onSelectContact(contact) },
                    onCall = { onQuickCall(contact.phone) },
                    onSms = { onQuickSms(contact.name, contact.phone) },
                    onWhatsApp = { onQuickWhatsApp(contact.name, contact.phone) }
                )
            }
        }
    }

    // Contact Unified History Modal
    if (selectedContact != null) {
        ContactHistoryDossierDialog(
            contact = selectedContact,
            allLogs = communicationLogs,
            reminders = reminders,
            preferredLanguage = preferredLanguage,
            onDismiss = { onSelectContact(null) },
            onSpeakHistory = onSpeakHistory
        )
    }
}

@Composable
fun ContactItemCard(
    contact: ContactRecord,
    preferredLanguage: String,
    onClick: () -> Unit,
    onCall: () -> Unit,
    onSms: () -> Unit,
    onWhatsApp: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("contact_card_${contact.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val isFamilyOrVip = contact.relation.equals("Family", ignoreCase = true) || contact.relation.equals("VIP", ignoreCase = true)
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(
                            color = if (isFamilyOrVip) Color(0xFFF59E0B).copy(alpha = 0.2f) else IndigoPrimary.copy(alpha = 0.15f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isFamilyOrVip) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "VIP",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        Text(
                            text = contact.name.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = IndigoPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = contact.name,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        if (contact.relation.isNotBlank()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = contact.relation,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = contact.phone,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            // Quick action icon buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onCall, modifier = Modifier.size(34.dp)) {
                    Icon(imageVector = Icons.Default.Call, contentDescription = "Call", tint = ColorCall, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onSms, modifier = Modifier.size(34.dp)) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Message, contentDescription = "SMS", tint = ColorSms, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onWhatsApp, modifier = Modifier.size(34.dp)) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(ColorWhatsApp, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("W", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ContactHistoryDossierDialog(
    contact: ContactRecord,
    allLogs: List<CommunicationLog>,
    reminders: List<ReminderItem>,
    preferredLanguage: String,
    onDismiss: () -> Unit,
    onSpeakHistory: (String) -> Unit
) {
    val contactLogs = remember(allLogs, contact) {
        allLogs.filter {
            it.contactName.contains(contact.name, ignoreCase = true) ||
                    it.contactNumber.replace(Regex("[^0-9]"), "").contains(contact.phone.replace(Regex("[^0-9]"), ""))
        }
    }

    val contactReminders = remember(reminders, contact) {
        reminders.filter { it.title.contains(contact.name, ignoreCase = true) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("contact_dossier_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = contact.name,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "${contact.phone} • ${contact.relation}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = CyanSecondary
                            )
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "कुल संवाद",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Text(
                                text = "${contactLogs.size} रिकॉर्ड",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = IndigoPrimary
                                )
                            )
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "पेंडिंग कार्य",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Text(
                                text = "${contactReminders.size} रिमाइंडर",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = CyanSecondary
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Unified History Timeline
                Text(
                    text = "संयुक्त संचार इतिहास (Calls + SMS + WhatsApp):",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (contactLogs.isEmpty()) {
                    Text(
                        text = "इस संपर्क के साथ अभी तक कोई रिकॉर्ड दर्ज नहीं है।",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        contactLogs.forEach { log ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${log.platform} (${log.type})",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = when (log.platform) {
                                                    "WHATSAPP" -> ColorWhatsApp
                                                    "SMS" -> ColorSms
                                                    "TELEGRAM" -> ColorTelegram
                                                    else -> ColorCall
                                                }
                                            )
                                        )
                                        Text(
                                            text = SimpleDateFormat("d MMM, hh:mm a", Locale.getDefault()).format(Date(log.timestamp)),
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = log.content,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            lineHeight = 16.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action button: Speak aloud history
                Button(
                    onClick = {
                        val speech = "${contact.name} का संपूर्ण इतिहास: कुल ${contactLogs.size} संवाद हैं। हालिया संदेश: " + (contactLogs.firstOrNull()?.content ?: "कोई हालिया संदेश नहीं")
                        onSpeakHistory(speech)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("पूरा इतिहास बोलकर सुनाएं (Listen to Dossier)", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
