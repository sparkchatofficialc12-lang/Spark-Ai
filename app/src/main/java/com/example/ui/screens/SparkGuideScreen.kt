package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.SparkGuideCatalog
import com.example.data.model.SparkGuideConversation
import com.example.ui.components.SparkBadge
import com.example.ui.components.SparkCard
import com.example.ui.components.SparkStatusBanner
import com.example.ui.components.SparkTopBar
import com.example.ui.theme.AmberGold
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.IceWhite
import com.example.ui.theme.MidnightBlack
import com.example.ui.theme.SecondaryIce
import com.example.ui.theme.SecondarySlate
import com.example.ui.theme.SoftGold
import com.example.ui.theme.SparkBrushes
import com.example.ui.theme.SteelBorder
import com.example.ui.viewmodel.LocalGuideBubble
import com.example.ui.viewmodel.SparkGuideUiState

@Composable
fun FloatingSparkGuideButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SmallFloatingActionButton(
        onClick = onClick,
        containerColor = SecondarySlate,
        contentColor = ChampagneGold,
        shape = CircleShape,
        modifier = modifier
            .size(50.dp)
            .testTag("floating_spark_guide_btn")
    ) {
        Surface(
            color = ChampagneGold.copy(alpha = 0.14f),
            shape = CircleShape,
            border = BorderStroke(1.2.dp, ChampagneGold),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Ask Spark Guide",
                    tint = ChampagneGold,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SparkGuideScreen(
    contextSection: String,
    initialQuestion: String,
    guideState: SparkGuideUiState,
    onAskQuestion: (String) -> Unit,
    onSetContextSection: (String) -> Unit,
    onSetLanguage: (String) -> Unit,
    onStartNewChat: () -> Unit,
    onOpenConversation: (SparkGuideConversation) -> Unit,
    onDeleteConversation: (String) -> Unit,
    onClearBanners: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var showHistorySheet by remember { mutableStateOf(false) }
    var copiedNotice by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    var didAutoAsk by rememberSaveable(initialQuestion) { mutableStateOf(false) }

    LaunchedEffect(contextSection) {
        onSetContextSection(contextSection)
    }

    LaunchedEffect(initialQuestion) {
        if (initialQuestion.isNotBlank() && !didAutoAsk) {
            didAutoAsk = true
            onAskQuestion(initialQuestion)
        }
    }

    LaunchedEffect(guideState.messages.size, guideState.streamingText.length) {
        val lastIdx = guideState.messages.size + if (guideState.streamingText.isNotBlank()) 1 else 0
        if (lastIdx > 0) {
            listState.animateScrollToItem(lastIdx)
        }
    }

    val contextQuestions = SparkGuideCatalog.settingsHelpQuestions[contextSection].orEmpty()

    Scaffold(
        topBar = {
            SparkTopBar(
                title = "SPARK GUIDE",
                subtitle = "Your Personal Spark AI Assistant",
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = onStartNewChat,
                        modifier = Modifier.testTag("spark_guide_new_chat_btn")
                    ) {
                        Icon(Icons.Default.AddComment, contentDescription = "New Chat", tint = ChampagneGold)
                    }
                    IconButton(
                        onClick = { showHistorySheet = true },
                        modifier = Modifier.testTag("spark_guide_history_btn")
                    ) {
                        Icon(Icons.Default.History, contentDescription = "Chat History", tint = ChampagneGold)
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = SecondarySlate,
                border = BorderStroke(1.dp, SteelBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Ask Spark Guide anything...", color = SecondaryIce) },
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ChampagneGold,
                            unfocusedBorderColor = SteelBorder,
                            focusedTextColor = IceWhite,
                            unfocusedTextColor = IceWhite
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("spark_guide_input")
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = if (guideState.isGenerating || inputText.isBlank()) SteelBorder else ChampagneGold,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(48.dp)
                            .clickable(enabled = !guideState.isGenerating && inputText.isNotBlank()) {
                                val q = inputText.trim()
                                inputText = ""
                                onAskQuestion(q)
                            }
                            .testTag("spark_guide_send_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send question",
                                tint = MidnightBlack
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SparkBrushes.CosmicBackground)
                .padding(innerPadding)
        ) {
            SparkStatusBanner(
                statusMessage = copiedNotice ?: guideState.statusBanner,
                errorMessage = guideState.errorBanner,
                onDismiss = {
                    copiedNotice = null
                    onClearBanners()
                }
            )

            // Language selector row
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Lang:", style = MaterialTheme.typography.labelSmall, color = SoftGold)
                    }
                }
                items(SparkGuideCatalog.supportedLanguages) { lang ->
                    FilterChip(
                        selected = guideState.selectedLanguage == lang,
                        onClick = { onSetLanguage(lang) },
                        label = { Text(lang) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ChampagneGold,
                            selectedLabelColor = MidnightBlack
                        )
                    )
                }
            }

            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Suggested Starter Questions / Settings Context Help
                item {
                    SparkCard(borderColor = ChampagneGold.copy(alpha = 0.55f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (contextQuestions.isNotEmpty()) {
                                    "Need help in ${contextSection.replaceFirstChar { it.uppercase() }}?"
                                } else {
                                    "Suggested Topics"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                color = ChampagneGold
                            )
                            TextButton(onClick = onStartNewChat) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = SoftGold, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Clear", color = SoftGold)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        val chips = if (contextQuestions.isNotEmpty()) {
                            contextQuestions + SparkGuideCatalog.suggestedQuestions.take(3)
                        } else {
                            SparkGuideCatalog.suggestedQuestions
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            chips.forEachIndexed { idx, q ->
                                Surface(
                                    color = MidnightBlack,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.45f)),
                                    modifier = Modifier
                                        .clickable(enabled = !guideState.isGenerating) { onAskQuestion(q) }
                                        .testTag("guide_starter_$idx")
                                ) {
                                    Text(
                                        text = q,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = IceWhite,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                items(guideState.messages, key = { it.id }) { msg ->
                    SparkGuideBubbleCard(
                        bubble = msg,
                        onCopy = {
                            val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cb.setPrimaryClip(ClipData.newPlainText("Spark Guide Answer", msg.text))
                            copiedNotice = "Copied Spark Guide answer to clipboard."
                        }
                    )
                }

                if (guideState.isGenerating) {
                    item {
                        SparkGuideBubbleCard(
                            bubble = LocalGuideBubble(
                                id = "streaming",
                                role = "assistant",
                                text = guideState.streamingText.ifBlank { "Spark Guide is thinking..." }
                            ),
                            isStreaming = true,
                            onCopy = {}
                        )
                    }
                }
            }
        }
    }

    if (showHistorySheet) {
        ModalBottomSheet(
            onDismissRequest = { showHistorySheet = false },
            containerColor = SecondarySlate
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Spark Guide Chat History", style = MaterialTheme.typography.titleLarge, color = ChampagneGold)
                if (guideState.conversations.isEmpty()) {
                    Text(
                        "No saved guide conversations yet. Ask any question to start.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryIce
                    )
                } else {
                    guideState.conversations.take(20).forEach { conv ->
                        SparkCard(
                            onClick = {
                                onOpenConversation(conv)
                                showHistorySheet = false
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(conv.title, style = MaterialTheme.typography.titleSmall, color = IceWhite)
                                    Text(
                                        "${conv.language} • ${conv.lastMessagePreview}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SecondaryIce,
                                        maxLines = 2
                                    )
                                }
                                IconButton(onClick = { onDeleteConversation(conv.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete chat", tint = SoftGold)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun SparkGuideBubbleCard(
    bubble: LocalGuideBubble,
    isStreaming: Boolean = false,
    onCopy: () -> Unit
) {
    val isUser = bubble.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (isUser) ChampagneGold.copy(alpha = 0.18f) else SecondarySlate,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(
                width = 1.dp,
                color = if (isUser) ChampagneGold else SteelBorder
            ),
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (isUser) "You" else "Spark Guide",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isUser) SoftGold else ChampagneGold
                        )
                        if (bubble.offlineFallback) {
                            SparkBadge(label = "OFFLINE GUIDE", color = AmberGold)
                        } else if (isStreaming) {
                            SparkBadge(label = "STREAMING", color = ChampagneGold)
                        }
                    }
                    if (!isUser && !isStreaming) {
                        IconButton(onClick = onCopy) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy answer",
                                tint = SoftGold,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = bubble.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = IceWhite
                )
            }
        }
    }
}
