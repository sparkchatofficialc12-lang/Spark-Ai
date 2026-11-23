package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.MediaUtils
import com.example.core.VoiceAndAudioEngine
import com.example.data.model.CreatorProfile
import com.example.data.model.DirectMessage
import com.example.data.model.NotificationItem
import com.example.ui.components.Base64Thumbnail
import com.example.ui.components.CreatorAvatarCircle
import com.example.ui.components.SparkBadge
import com.example.ui.components.SparkCard
import com.example.ui.components.SparkGoldButton
import com.example.ui.components.SparkOutlinedButton
import com.example.ui.components.SparkTopBar
import com.example.ui.navigation.SparkRoute
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.IceWhite
import com.example.ui.theme.MidnightBlack
import com.example.ui.theme.SecondaryIce
import com.example.ui.theme.SecondarySlate
import com.example.ui.theme.SoftGold
import com.example.ui.theme.SparkBrushes
import com.example.ui.theme.SteelBorder
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.SocialUiState
import com.example.ui.viewmodel.StudioUiState

@Composable
fun InboxScreen(
    currentUid: String,
    myUsername: String,
    studioState: StudioUiState,
    socialState: SocialUiState,
    onStartChat: (CreatorProfile) -> Unit,
    onMarkNotificationRead: (String) -> Unit,
    onMarkAllNotificationsRead: () -> Unit,
    onNavigate: (SparkRoute) -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0 = Messages, 1 = Notifications
    var showNewChatPicker by remember { mutableStateOf(false) }

    val availablePeers = remember(socialState.allCreatorProfiles, socialState.blockedCreatorIds, currentUid) {
        socialState.allCreatorProfiles.filter {
            it.creatorId != currentUid && !socialState.blockedCreatorIds.contains(it.creatorId)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SparkBrushes.CosmicBackground)
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MidnightBlack,
            contentColor = ChampagneGold,
            indicator = { pos ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(pos[selectedTab]),
                    color = ChampagneGold
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("MESSAGES (${socialState.conversations.size})", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("inbox_tab_messages")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("NOTIFICATIONS (${studioState.notifications.count { !it.readFlag }})", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("inbox_tab_notifications")
            )
        }

        if (selectedTab == 0) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    SparkGoldButton(
                        text = "Start New Direct Message",
                        icon = Icons.Default.AddComment,
                        onClick = { showNewChatPicker = true },
                        modifier = Modifier.fillMaxWidth().testTag("inbox_new_chat_btn")
                    )
                }

                if (socialState.conversations.isEmpty()) {
                    item {
                        SparkCard {
                            Text("No private conversations yet", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Connect with fellow Spark Social creators to send text messages, photos, videos, audio clips, and voice notes.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SecondaryIce
                            )
                        }
                    }
                } else {
                    items(socialState.conversations, key = { it.id }) { conv ->
                        val peerId = conv.participantIds.firstOrNull { it != currentUid }.orEmpty()
                        val peerName = conv.participantNames[peerId] ?: "Creator"
                        SparkCard(
                            onClick = {
                                onNavigate(SparkRoute.ChatThread(conv.id, peerId, peerName))
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CreatorAvatarCircle(peerName)
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("@$peerName", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        conv.lastMessageText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SecondaryIce,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                SparkBadge(label = conv.lastMessageType.uppercase())
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(92.dp)) }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (studioState.notifications.any { !it.readFlag }) {
                    item {
                        SparkOutlinedButton(
                            text = "Mark All as Read",
                            onClick = onMarkAllNotificationsRead,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                if (studioState.notifications.isEmpty()) {
                    item {
                        SparkCard {
                            Text("No notifications yet", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Likes, comments, new followers, template remixes, and studio alerts appear here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SecondaryIce
                            )
                        }
                    }
                } else {
                    items(studioState.notifications, key = { it.id }) { notif ->
                        NotificationCardItem(
                            notif = notif,
                            onClick = {
                                onMarkNotificationRead(notif.id)
                                when {
                                    notif.category == "remix" && notif.targetPostId.isNotBlank() ->
                                        onNavigate(SparkRoute.TemplateDetail(notif.targetPostId))
                                    notif.targetPostId.isNotBlank() ->
                                        onNavigate(SparkRoute.PostDetail(notif.targetPostId))
                                    notif.targetCreatorId.isNotBlank() ->
                                        onNavigate(SparkRoute.CreatorProfileRoute(notif.targetCreatorId))
                                }
                            }
                        )
                    }
                }
                item { Spacer(Modifier.height(92.dp)) }
            }
        }
    }

    if (showNewChatPicker) {
        AlertDialog(
            onDismissRequest = { showNewChatPicker = false },
            containerColor = SecondarySlate,
            title = { Text("Select Creator to Message") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (availablePeers.isEmpty()) {
                        item {
                            Text(
                                "No other creators found yet. Discover or follow creators first!",
                                style = MaterialTheme.typography.bodySmall,
                                color = SecondaryIce
                            )
                        }
                    } else {
                        items(availablePeers, key = { it.creatorId }) { peer ->
                            SparkCard(onClick = {
                                showNewChatPicker = false
                                onStartChat(peer)
                            }) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CreatorAvatarCircle(peer.username)
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text("@${peer.username}", style = MaterialTheme.typography.titleSmall)
                                        Text(peer.displayName, style = MaterialTheme.typography.bodySmall, color = SecondaryIce)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNewChatPicker = false }) { Text("Close") }
            }
        )
    }
}

@Composable
fun NotificationCardItem(notif: NotificationItem, onClick: () -> Unit) {
    SparkCard(
        borderColor = if (!notif.readFlag) ChampagneGold else SteelBorder,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(notif.title, style = MaterialTheme.typography.titleSmall, color = if (!notif.readFlag) ChampagneGold else IceWhite)
                Spacer(Modifier.height(2.dp))
                Text(notif.message, style = MaterialTheme.typography.bodySmall, color = SecondaryIce)
            }
            SparkBadge(
                label = if (notif.readFlag) notif.category.uppercase() else "NEW",
                color = if (notif.readFlag) SecondaryIce else ChampagneGold
            )
        }
    }
}

@Composable
fun ChatThreadScreen(
    conversationId: String,
    peerUserId: String,
    peerUsername: String,
    currentUid: String,
    myUsername: String,
    socialState: SocialUiState,
    voiceAudio: VoiceAndAudioEngine,
    onObserveMessages: (String) -> Unit,
    onSendMessage: (
        conversationId: String,
        receiverId: String,
        senderUsername: String,
        messageType: String,
        text: String,
        mediaUrl: String,
        mediaBitmap: android.graphics.Bitmap?,
        audioDurationSec: Int,
        replyToMessageId: String,
        replyToPreview: String
    ) -> Unit,
    onDeleteMessage: (String, String) -> Unit,
    onBlockCreator: (String) -> Unit,
    onReportCreator: (String) -> Unit,
    onBack: () -> Unit,
    onNavigate: (SparkRoute) -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(conversationId) {
        onObserveMessages(conversationId)
    }

    var textInput by remember { mutableStateOf("") }
    var replyTarget by remember { mutableStateOf<DirectMessage?>(null) }
    val isRecording by voiceAudio.isRecordingVoice.collectAsStateWithLifecycle()
    val isAudioPlaying by voiceAudio.isAudioPlaying.collectAsStateWithLifecycle()

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val replyId = replyTarget?.id.orEmpty()
            val replyPreview = replyTarget?.text?.take(80).orEmpty()
            replyTarget = null
            scope.launch {
                val bmp = MediaUtils.uriToBitmapAsync(context, uri, 720)
                if (bmp != null) {
                    onSendMessage(
                        conversationId, peerUserId, myUsername, "image",
                        "Sent a photo", "", bmp, 0,
                        replyId, replyPreview
                    )
                }
            }
        }
    }

    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            onSendMessage(
                conversationId, peerUserId, myUsername, "video",
                "Shared a video", uri.toString(), null, 0,
                replyTarget?.id.orEmpty(), replyTarget?.text?.take(80).orEmpty()
            )
            replyTarget = null
        }
    }

    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            onSendMessage(
                conversationId, peerUserId, myUsername, "audio",
                "Audio attachment", uri.toString(), null, 0,
                replyTarget?.id.orEmpty(), replyTarget?.text?.take(80).orEmpty()
            )
            replyTarget = null
        }
    }

    val micPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            voiceAudio.startVoiceRecording()
        }
    }

    Scaffold(
        topBar = {
            SparkTopBar(
                title = "@$peerUsername",
                subtitle = "Direct Messages",
                onBack = onBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SparkBrushes.CosmicBackground)
                .padding(innerPadding)
        ) {
            // Peer safety/profile quick bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SparkOutlinedButton(
                    text = "View Profile",
                    onClick = { onNavigate(SparkRoute.CreatorProfileRoute(peerUserId)) }
                )
                Row {
                    IconButton(onClick = { onReportCreator(peerUserId) }) {
                        Icon(Icons.Default.Flag, contentDescription = "Report user", tint = SoftGold)
                    }
                    IconButton(onClick = {
                        onBlockCreator(peerUserId)
                        onBack()
                    }) {
                        Icon(Icons.Default.Block, contentDescription = "Block user", tint = ErrorRed)
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(socialState.activeMessages, key = { it.id }) { msg ->
                    val isMine = msg.senderId == currentUid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isMine) ChampagneGold.copy(alpha = 0.2f) else SecondarySlate,
                            border = BorderStroke(1.dp, if (isMine) ChampagneGold else SteelBorder),
                            modifier = Modifier.widthIn(max = 300.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                if (msg.replyToPreview.isNotBlank()) {
                                    Text(
                                        text = "Replying to: ${msg.replyToPreview}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ChampagneGold
                                    )
                                    Spacer(Modifier.height(4.dp))
                                }

                                if (msg.deletedForAll) {
                                    Text("Message deleted", style = MaterialTheme.typography.bodySmall, color = SecondaryIce)
                                } else {
                                    if (msg.mediaThumbnailData.isNotBlank()) {
                                        Base64Thumbnail(
                                            base64Data = msg.mediaThumbnailData,
                                            contentDescription = "Photo attachment",
                                            modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(10.dp))
                                        )
                                        Spacer(Modifier.height(6.dp))
                                    }

                                    when (msg.messageType) {
                                        "voice", "audio" -> {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(onClick = {
                                                    if (isAudioPlaying) voiceAudio.stopAudio()
                                                    else if (msg.mediaUrl.startsWith("data:audio")) voiceAudio.playBase64Audio(msg.mediaUrl)
                                                    else if (msg.mediaUrl.isNotBlank()) voiceAudio.playLocalAudio(Uri.parse(msg.mediaUrl), 1f)
                                                }) {
                                                    Icon(
                                                        imageVector = if (isAudioPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                                        contentDescription = "Play voice note",
                                                        tint = ChampagneGold
                                                    )
                                                }
                                                Text(
                                                    text = if (msg.messageType == "voice") "Voice Note (${msg.audioDurationSec}s)" else "Audio Clip",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = IceWhite
                                                )
                                            }
                                        }
                                        "video" -> {
                                            SparkOutlinedButton(
                                                text = "Watch Video Clip",
                                                icon = Icons.Default.PlayArrow,
                                                onClick = {
                                                    onNavigate(SparkRoute.VideoPlayer("Shared Clip", msg.mediaUrl))
                                                }
                                            )
                                        }
                                        else -> {}
                                    }

                                    if (msg.text.isNotBlank()) {
                                        Text(msg.text, style = MaterialTheme.typography.bodyMedium, color = IceWhite)
                                    }
                                }

                                Spacer(Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (isMine) {
                                        Icon(
                                            imageVector = Icons.Default.DoneAll,
                                            contentDescription = msg.deliveryStatus,
                                            tint = if (msg.deliveryStatus == "read") SuccessGreen else SecondaryIce,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(msg.deliveryStatus, style = MaterialTheme.typography.labelSmall, color = SecondaryIce)
                                    }
                                    if (!msg.deletedForAll) {
                                        Box(
                                            modifier = Modifier
                                                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                                .clip(CircleShape)
                                                .clickable { replyTarget = msg },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Reply,
                                                contentDescription = "Reply",
                                                tint = ChampagneGold,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    if (isMine && !msg.deletedForAll) {
                                        Box(
                                            modifier = Modifier
                                                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                                .clip(CircleShape)
                                                .clickable {
                                                    onDeleteMessage(conversationId, msg.id)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete message",
                                                tint = ErrorRed,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Reply banner
            if (replyTarget != null) {
                Surface(color = SecondarySlate, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Replying: ${replyTarget!!.text.take(60)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = ChampagneGold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { replyTarget = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel reply")
                        }
                    }
                }
            }

            // Composer bar
            Surface(color = MidnightBlack) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(onClick = {
                            imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }) {
                            Icon(Icons.Default.Image, contentDescription = "Send photo", tint = ChampagneGold)
                        }
                        IconButton(onClick = {
                            videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                        }) {
                            Icon(Icons.Default.VideoFile, contentDescription = "Send video", tint = ChampagneGold)
                        }
                        IconButton(onClick = { audioPicker.launch("audio/*") }) {
                            Icon(Icons.Default.AttachFile, contentDescription = "Send audio", tint = ChampagneGold)
                        }
                        IconButton(onClick = {
                            if (isRecording) {
                                val recorded = voiceAudio.stopVoiceRecording(cancel = false)
                                if (recorded != null) {
                                    onSendMessage(
                                        conversationId, peerUserId, myUsername, "voice",
                                        "", recorded.first, null, recorded.second,
                                        replyTarget?.id.orEmpty(), replyTarget?.text?.take(80).orEmpty()
                                    )
                                    replyTarget = null
                                }
                            } else {
                                val hasPerm = ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasPerm) voiceAudio.startVoiceRecording()
                                else micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }) {
                            Icon(
                                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = if (isRecording) "Stop and send voice note" else "Record voice note",
                                tint = if (isRecording) ErrorRed else ChampagneGold
                            )
                        }

                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            placeholder = { Text(if (isRecording) "Recording voice..." else "Message @$peerUsername") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("chat_text_input")
                        )
                        IconButton(
                            onClick = {
                                if (textInput.isNotBlank()) {
                                    onSendMessage(
                                        conversationId, peerUserId, myUsername, "text",
                                        textInput, "", null, 0,
                                        replyTarget?.id.orEmpty(), replyTarget?.text?.take(80).orEmpty()
                                    )
                                    textInput = ""
                                    replyTarget = null
                                }
                            },
                            modifier = Modifier.testTag("chat_send_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send message", tint = ChampagneGold)
                        }
                    }
                }
            }
        }
    }
}
