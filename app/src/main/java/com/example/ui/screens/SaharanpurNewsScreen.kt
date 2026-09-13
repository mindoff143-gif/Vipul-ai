package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FiberNew
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FacebookPageConfig
import com.example.data.model.SaharanpurNewsEntity
import com.example.service.AppInviteService

private val FacebookBlue = Color(0xFF1877F2)
private val GreenPublished = Color(0xFF10B981)
private val DeepIndigo = Color(0xFF1E1B4B)
private val IndigoAccent = Color(0xFF4F46E5)
private val OrangeAccent = Color(0xFFEA580C)

@Composable
fun SaharanpurNewsScreen(
    newsArticles: List<SaharanpurNewsEntity>,
    facebookConfig: FacebookPageConfig,
    isSearchingNews: Boolean,
    isPublishingToFb: Boolean,
    preferredLanguage: String,
    onSearchNews: (query: String) -> Unit,
    onPublishToFacebook: (article: SaharanpurNewsEntity, customText: String?) -> Unit,
    onShareViaIntent: (context: Context, article: SaharanpurNewsEntity) -> Unit,
    onSaveFacebookConfig: (config: FacebookPageConfig) -> Unit,
    onSpeakNews: (text: String) -> Unit,
    onShowDownloadDialog: () -> Unit,
    onShowInviteDialog: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedCategoryFilter by remember { mutableStateOf("सभी") }
    var searchQuery by remember { mutableStateOf("") }
    var showConfigDialog by remember { mutableStateOf(false) }
    var editingArticle by remember { mutableStateOf<SaharanpurNewsEntity?>(null) }

    val categories = listOf(
        "सभी",
        "ताज़ा ख़बर",
        "विकास व स्मार्ट सिटी",
        "शिक्षा व विश्वविद्यालय",
        "किसान व कृषि",
        "प्रशासन व पुलिस",
        "उद्योग व व्यापार"
    )

    val filteredArticles = newsArticles.filter { article ->
        val matchesCategory = if (selectedCategoryFilter == "सभी") true else article.category == selectedCategoryFilter
        val matchesQuery = if (searchQuery.isBlank()) true else {
            article.title.contains(searchQuery, ignoreCase = true) ||
                    article.summaryHindi.contains(searchQuery, ignoreCase = true) ||
                    article.category.contains(searchQuery, ignoreCase = true)
        }
        matchesCategory && matchesQuery
    }

    val publishedCount = newsArticles.count { it.isPublishedToFb }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Header Hero Card
            item {
                SaharanpurHeroBanner(
                    newsCount = newsArticles.size,
                    publishedCount = publishedCount,
                    isAutoPublish = facebookConfig.autoPublishEnabled,
                    onSearchClick = { onSearchNews(searchQuery) },
                    onDownloadClick = onShowDownloadDialog,
                    onInviteClick = onShowInviteDialog,
                    isSearching = isSearchingNews
                )
            }

            // 2. Facebook Connected Page Card
            item {
                FacebookPageStatusCard(
                    config = facebookConfig,
                    onConfigureClick = { showConfigDialog = true },
                    onToggleAutoPublish = { enabled ->
                        onSaveFacebookConfig(facebookConfig.copy(autoPublishEnabled = enabled))
                    }
                )
            }

            // 3. Search and Category Filter Row
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("सहारनपुर समाचार खोजें...", fontSize = 14.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = IndigoAccent
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("saharanpur_news_search_input")
                        )

                        Button(
                            onClick = { onSearchNews(searchQuery) },
                            enabled = !isSearchingNews,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoAccent),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                            modifier = Modifier.testTag("fetch_saharanpur_news_button")
                        ) {
                            if (isSearchingNews) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("खोजें", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    // Category Filter Scrollable Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { category ->
                            FilterChip(
                                selected = selectedCategoryFilter == category,
                                onClick = { selectedCategoryFilter = category },
                                label = { Text(category, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = IndigoAccent.copy(alpha = 0.15f),
                                    selectedLabelColor = IndigoAccent
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                        }
                    }
                }
            }

            // 4. Articles Feed
            if (filteredArticles.isEmpty()) {
                item {
                    EmptyNewsPlaceholder(
                        searchQuery = searchQuery,
                        onFetchNews = { onSearchNews("") }
                    )
                }
            } else {
                items(filteredArticles, key = { it.id }) { article ->
                    NewsArticleCard(
                        article = article,
                        isPublishing = isPublishingToFb,
                        onPublish = { onPublishToFacebook(article, null) },
                        onShare = { onShareViaIntent(context, article) },
                        onEdit = { editingArticle = article },
                        onSpeak = {
                            onSpeakNews("${article.title}। ${article.summaryHindi}")
                        }
                    )
                }
            }
        }
    }

    // Facebook Config Dialog
    if (showConfigDialog) {
        FacebookConfigDialog(
            currentConfig = facebookConfig,
            onDismiss = { showConfigDialog = false },
            onSave = { updated ->
                onSaveFacebookConfig(updated)
                showConfigDialog = false
            }
        )
    }

    // Edit Post Dialog
    editingArticle?.let { article ->
        EditPostBeforePublishDialog(
            article = article,
            onDismiss = { editingArticle = null },
            onPublish = { customText ->
                onPublishToFacebook(article, customText)
                editingArticle = null
            }
        )
    }
}

@Composable
private fun SaharanpurHeroBanner(
    newsCount: Int,
    publishedCount: Int,
    isAutoPublish: Boolean,
    onSearchClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onInviteClick: () -> Unit = {},
    isSearching: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("saharanpur_hero_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF1E1B4B),
                            Color(0xFF312E81),
                            Color(0xFF1E3A8A)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "सहारनपुर न्यूज़ (उ.प्र.)",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "लाइव खोजें व FB पर पोस्ट करें",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Action buttons: Invite + Download APK
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = onInviteClick,
                            colors = ButtonDefaults.buttonColors(containerColor = GreenPublished),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("invite_friends_hero_button")
                        ) {
                            Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "Invite", modifier = Modifier.size(14.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("इनवाइट", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        OutlinedButton(
                            onClick = onDownloadClick,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("download_apk_hero_button")
                        ) {
                            Icon(imageVector = Icons.Default.Download, contentDescription = "Download APK", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("APK", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.2f))

                // Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "$newsCount ताज़ा ख़बरें",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$publishedCount फेसबुक पर पोस्टेड",
                            color = GreenPublished,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isAutoPublish) GreenPublished.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isAutoPublish) "⚡ ऑटो-पोस्ट ऑन" else "⚙️ मैन्युअल मोड",
                                color = if (isAutoPublish) Color(0xFF6EE7B7) else Color.White.copy(alpha = 0.9f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FacebookPageStatusCard(
    config: FacebookPageConfig,
    onConfigureClick: () -> Unit,
    onToggleAutoPublish: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("facebook_status_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(FacebookBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("f", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    }
                    Column {
                        Text(
                            text = config.pageName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Page ID: ${config.pageId} • ${if (config.pageAccessToken.isNotBlank()) "Graph API कनेक्टेड" else "टेस्ट व शेयर मोड"}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onConfigureClick,
                    modifier = Modifier.testTag("configure_fb_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Configure Facebook",
                        tint = IndigoAccent
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ऑटो-पब्लिश (Auto-publish)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "नई खबर मिलते ही अपने-आप फेसबुक पेज पर पब्लिश करें",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = config.autoPublishEnabled,
                    onCheckedChange = onToggleAutoPublish,
                    colors = SwitchDefaults.colors(checkedThumbColor = FacebookBlue, checkedTrackColor = FacebookBlue.copy(alpha = 0.3f)),
                    modifier = Modifier.testTag("auto_publish_switch")
                )
            }
        }
    }
}

@Composable
private fun NewsArticleCard(
    article: SaharanpurNewsEntity,
    isPublishing: Boolean,
    onPublish: () -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onSpeak: () -> Unit
) {
    val context = LocalContext.current
    var showPostPreview by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("saharanpur_news_card_${article.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Category & Source header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(IndigoAccent.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = article.category,
                            color = IndigoAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = article.source,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = article.publishedDate,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )

                    IconButton(
                        onClick = onSpeak,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Read News Aloud",
                            tint = IndigoAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Headline
            Text(
                text = article.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Summary
            Text(
                text = article.summaryHindi,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )

            // Facebook Published Status Badge
            if (article.isPublishedToFb) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(GreenPublished.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = GreenPublished,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "फेसबुक पेज पर पब्लिश हो गया • Post ID: ${article.fbPostId ?: "Published"}",
                        color = GreenPublished,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Expandable Facebook Post Preview
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPostPreview = !showPostPreview }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (showPostPreview) "▼ फेसबुक पोस्ट का प्रारूप छिपाएं" else "▶ फेसबुक पोस्ट का पूरा प्रारूप देखें",
                    fontSize = 12.sp,
                    color = FacebookBlue,
                    fontWeight = FontWeight.Medium
                )

                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        clipboard?.setPrimaryClip(ClipData.newPlainText("FB Post", article.formattedFbPost))
                        Toast.makeText(context, "पोस्ट कॉपी हो गई!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy text",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = showPostPreview,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = article.formattedFbPost,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Publish to Facebook Page Button
                Button(
                    onClick = onPublish,
                    enabled = !isPublishing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FacebookBlue,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("publish_news_to_fb_${article.id}")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (article.isPublishedToFb) "पुनः पब्लिश करें" else "फेसबुक पेज पर पब्लिश करें",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Share via Intent
                OutlinedButton(
                    onClick = onShare,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("share_news_intent_${article.id}")
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ऐप में शेयर", fontSize = 11.sp)
                }

                // Edit with AI
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Post with AI",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyNewsPlaceholder(
    searchQuery: String,
    onFetchNews: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Article,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Text(
                text = if (searchQuery.isNotBlank()) "'$searchQuery' से संबंधित कोई खबर नहीं मिली" else "सहारनपुर की कोई खबर अभी लोड नहीं है",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onFetchNews,
                colors = ButtonDefaults.buttonColors(containerColor = IndigoAccent),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("सहारनपुर ताज़ा समाचार लोड करें", fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun FacebookConfigDialog(
    currentConfig: FacebookPageConfig,
    onDismiss: () -> Unit,
    onSave: (FacebookPageConfig) -> Unit
) {
    var pageId by remember { mutableStateOf(currentConfig.pageId) }
    var pageName by remember { mutableStateOf(currentConfig.pageName) }
    var pageToken by remember { mutableStateOf(currentConfig.pageAccessToken) }
    var autoPublish by remember { mutableStateOf(currentConfig.autoPublishEnabled) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(FacebookBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Text("f", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Text("फेसबुक पेज इंटीग्रेशन सेटिंग्स", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "अपने फेसबुक पेज पर समाचार स्वतः पब्लिश करने के लिए पेज आईडी व एक्सेस टोकन दर्ज करें:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = pageName,
                    onValueChange = { pageName = it },
                    label = { Text("पेज का नाम (Page Name)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = pageId,
                    onValueChange = { pageId = it },
                    label = { Text("फेसबुक पेज आईडी (Page ID)") },
                    placeholder = { Text("उदा. 109876543210987") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = pageToken,
                    onValueChange = { pageToken = it },
                    label = { Text("Page Access Token (Meta Graph API)") },
                    placeholder = { Text("टोकन यहाँ पेस्ट करें या खाली छोड़ें (Test Mode)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "💡 नोट: टोकन न होने पर भी आप 'ऐप में शेयर' बटन द्वारा एक क्लिक में फेसबुक ऐप या ब्राउज़र पर पोस्ट शेयर कर सकते हैं।",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        currentConfig.copy(
                            pageId = pageId.trim(),
                            pageName = pageName.trim(),
                            pageAccessToken = pageToken.trim(),
                            autoPublishEnabled = autoPublish
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = FacebookBlue)
            ) {
                Text("सेटिंग्स सुरक्षित करें")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("रद्द करें")
            }
        }
    )
}

@Composable
fun EditPostBeforePublishDialog(
    article: SaharanpurNewsEntity,
    onDismiss: () -> Unit,
    onPublish: (customText: String) -> Unit
) {
    var postText by remember { mutableStateOf(article.formattedFbPost) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = IndigoAccent)
                Text("पोस्ट एडिट व अनुकूलित करें", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "फेसबुक पेज पर पब्लिश करने से पहले पोस्ट के टेक्स्ट, हैशटैग या रिपोर्टर नाम में संशोधन करें:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = postText,
                    onValueChange = { postText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    maxLines = 15
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onPublish(postText) },
                colors = ButtonDefaults.buttonColors(containerColor = FacebookBlue)
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("अभी पब्लिश करें")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("रद्द करें")
            }
        }
    )
}

@Composable
fun DownloadAppDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val apkUrl = AppInviteService.DIRECT_APK_URL

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = Icons.Default.Download, contentDescription = null, tint = IndigoAccent)
                Text("डायरेक्ट APK व इनवाइट लिंक", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "इस लिंक को खोलते ही सीधे Personal AI Assistant की APK आपके फोन में डाउनलोड हो जाएगी:",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Direct APK Download Box
                Card(
                    colors = CardDefaults.cardColors(containerColor = IndigoAccent.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "⚡ डायरेक्ट APK डाउनलोड लिंक:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = IndigoAccent
                            )
                        }
                        Text(
                            text = apkUrl,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    AppInviteService.downloadApkDirectly(context)
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = IndigoAccent),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("सीधा डाउनलोड", fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    AppInviteService.copyDirectApkLink(context)
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DeepIndigo),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("कॉपी लिंक", fontSize = 11.sp)
                            }
                        }
                    }
                }

                // WhatsApp Invite Share
                Button(
                    onClick = { AppInviteService.shareViaWhatsApp(context) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("WhatsApp पर इनवाइट व लिंक भेजें", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Text(
                    text = "📱 निर्देश: लिंक खोलने पर APK तुरंत डाउनलोड होगी। डाउनलोड पूरी होने पर फाइल खोलें और Install करें।",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = IndigoAccent)) {
                Text("ठीक है (Done)")
            }
        }
    )
}
