package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.example.R
import com.example.ui.components.SparkStatusBanner
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.core.MediaUtils
import com.example.data.model.MusicLibraryCatalog
import com.example.data.model.StoryItem
import com.example.ui.components.Base64Thumbnail
import com.example.ui.components.CreatorAvatarCircle
import com.example.ui.components.SparkBadge
import com.example.ui.components.SparkBusyOverlay
import com.example.ui.components.SparkCard
import com.example.ui.components.SparkGoldButton
import com.example.ui.components.SparkOutlinedButton
import com.example.ui.components.SparkTopBar
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.IceWhite
import com.example.ui.theme.MidnightBlack
import com.example.ui.theme.SecondaryIce
import com.example.ui.theme.SecondarySlate
import com.example.ui.theme.SoftGold
import com.example.ui.theme.SparkBrushes
import com.example.ui.theme.SteelBorder
import com.example.ui.viewmodel.SocialUiState

@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoriesViewerScreen(
    startCreatorId: String,
    currentUid: String,
    socialState: SocialUiState,
    onOpenStory: (StoryItem) -> Unit,
    onDeleteStory: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current

    val stories = remember(socialState.activeStories, startCreatorId) {
        if (startCreatorId.isBlank()) socialState.activeStories
        else {
            val prioritized = socialState.activeStories.filter { it.creatorId == startCreatorId }
            val others = socialState.activeStories.filter { it.creatorId != startCreatorId }
            prioritized + others
        }
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    val story = stories.getOrNull(currentIndex)
    var showViewersSheet by remember { mutableStateOf(false) }

    LaunchedEffect(story?.id) {
        if (story != null) onOpenStory(story)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightBlack)
    ) {
        if (story == null) {
            Column(
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No active 24-hour stories", style = MaterialTheme.typography.titleLarge, color = IceWhite)
                Spacer(Modifier.height(10.dp))
                SparkGoldButton(text = "Back to Feed", onClick = onBack)
            }
        } else {
            val isOwner = story.creatorId == currentUid

            if (story.mediaType == "video" && story.mediaUrl.isNotBlank()) {
                var isPaused by remember(story.id) { mutableStateOf(false) }
                val player = remember(story.mediaUrl) {
                    ExoPlayer.Builder(context).build().apply {
                        setMediaItem(MediaItem.fromUri(story.mediaUrl))
                        prepare()
                        playWhenReady = !isPaused
                    }
                }
                LaunchedEffect(player, isPaused) {
                    player.playWhenReady = !isPaused
                }
                DisposableEffect(player) {
                    onDispose { player.release() }
                }
                AndroidView(
                    factory = { ctx ->
                        (LayoutInflater.from(ctx).inflate(R.layout.view_spark_player, null, false) as PlayerView).apply {
                            this.player = player
                            useController = false
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    update = {
                        it.player = player
                        it.useController = false
                    },
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { isPaused = !isPaused },
                    contentAlignment = Alignment.Center
                ) {
                    if (isPaused) {
                        Surface(
                            color = MidnightBlack.copy(alpha = 0.72f),
                            shape = CircleShape,
                            border = BorderStroke(1.5.dp, ChampagneGold)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Resume story",
                                tint = ChampagneGold,
                                modifier = Modifier.padding(16.dp).size(36.dp)
                            )
                        }
                    }
                }
            } else if (story.thumbnailData.isNotBlank()) {
                Base64Thumbnail(
                    base64Data = story.thumbnailData,
                    contentDescription = story.caption,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SparkBrushes.HeroCardGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = story.overlayText.ifBlank { story.caption.ifBlank { "Spark AI 24h Story" } },
                        style = MaterialTheme.typography.headlineMedium,
                        color = ChampagneGold,
                        modifier = Modifier.padding(28.dp)
                    )
                }
            }

            // Progress & Header
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(top = 34.dp, start = 14.dp, end = 14.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    stories.forEachIndexed { idx, _ ->
                        LinearProgressIndicator(
                            progress = { if (idx <= currentIndex) 1f else 0f },
                            color = ChampagneGold,
                            trackColor = SteelBorder,
                            modifier = Modifier.weight(1f).height(3.dp)
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ChampagneGold)
                        }
                        CreatorAvatarCircle(story.creatorUsername)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                "@${story.creatorUsername}",
                                style = MaterialTheme.typography.titleSmall,
                                color = IceWhite,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "24h Story • ${story.visibility}",
                                style = MaterialTheme.typography.labelSmall,
                                color = SoftGold
                            )
                        }
                    }
                    if (isOwner) {
                        IconButton(onClick = {
                            onDeleteStory(story.id)
                            if (stories.size <= 1) onBack() else currentIndex = (currentIndex - 1).coerceAtLeast(0)
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete story", tint = ErrorRed)
                        }
                    }
                }
            }

            // Bottom overlay: sticker, music, caption, navigation, viewers
            Surface(
                color = MidnightBlack.copy(alpha = 0.78f),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (story.stickerTag.isNotBlank()) {
                            SparkBadge(label = story.stickerTag, color = ChampagneGold)
                        }
                        if (story.musicTrackTitle.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = SecondarySlate,
                                border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(story.musicTrackTitle, style = MaterialTheme.typography.labelSmall, color = SoftGold)
                                }
                            }
                        }
                    }
                    if (story.overlayText.isNotBlank() && story.thumbnailData.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(story.overlayText, style = MaterialTheme.typography.titleMedium, color = ChampagneGold)
                    }
                    if (story.caption.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(story.caption, style = MaterialTheme.typography.bodySmall, color = IceWhite)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SparkOutlinedButton(
                                text = "Prev",
                                enabled = currentIndex > 0,
                                onClick = { currentIndex = (currentIndex - 1).coerceAtLeast(0) }
                            )
                            SparkGoldButton(
                                text = if (currentIndex + 1 < stories.size) "Next" else "Done",
                                onClick = {
                                    if (currentIndex + 1 < stories.size) currentIndex += 1 else onBack()
                                }
                            )
                        }
                        if (isOwner) {
                            SparkOutlinedButton(
                                text = "${story.viewsCount} Viewers",
                                icon = Icons.Default.Visibility,
                                onClick = { showViewersSheet = true }
                            )
                        }
                    }
                }
            }

            if (showViewersSheet && isOwner) {
                ModalBottomSheet(
                    onDismissRequest = { showViewersSheet = false },
                    containerColor = SecondarySlate
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Story Viewers (${socialState.selectedStoryViews.size})", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(10.dp))
                        if (socialState.selectedStoryViews.isEmpty()) {
                            Text("No views recorded yet.", style = MaterialTheme.typography.bodySmall, color = SecondaryIce)
                        } else {
                            socialState.selectedStoryViews.forEach { v ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                ) {
                                    CreatorAvatarCircle(v.viewerUsername)
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = if (v.viewerPublic) "@${v.viewerUsername}" else "Private Viewer",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun CreateStoryScreen(
    username: String,
    avatarUrl: String,
    isBusy: Boolean,
    onCreateStory: (
        creatorUsername: String,
        creatorAvatarUrl: String,
        mediaType: String,
        mediaUrl: String,
        bitmap: Bitmap?,
        overlayText: String,
        stickerTag: String,
        caption: String,
        musicTrackTitle: String,
        visibility: String
    ) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var mediaType by remember { mutableStateOf("photo") }
    var pickedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pickedVideoUri by remember { mutableStateOf("") }
    var overlayText by remember { mutableStateOf("") }
    var stickerTag by remember { mutableStateOf(MusicLibraryCatalog.stickerTags.first()) }
    var caption by remember { mutableStateOf("") }
    var selectedTrack by remember { mutableStateOf(MusicLibraryCatalog.curatedTracks.first().title) }
    var visibility by remember { mutableStateOf("public") }
    var localError by remember { mutableStateOf<String?>(null) }

    val previewBase64 by produceState<String?>(initialValue = null, key1 = pickedBitmap) {
        value = pickedBitmap?.let { MediaUtils.bitmapToCompactBase64Async(it, 140_000) }
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val bmp = MediaUtils.uriToBitmapAsync(context, uri, 960)
                if (bmp != null) {
                    pickedBitmap = bmp
                    mediaType = "photo"
                    pickedVideoUri = ""
                    localError = null
                } else {
                    localError = "Could not load selected photo."
                }
            }
        }
    }

    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            pickedVideoUri = uri.toString()
            mediaType = "video"
            pickedBitmap = null
            localError = null
        }
    }

    Scaffold(
        topBar = {
            SparkTopBar(
                title = "Create 24-Hour Story",
                subtitle = "Expires automatically after 24h",
                onBack = onBack
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SparkBrushes.CosmicBackground)
                .padding(innerPadding)
        ) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (localError != null) {
                    item {
                        SparkStatusBanner(
                            statusMessage = null,
                            errorMessage = localError,
                            onDismiss = { localError = null }
                        )
                    }
                }

                item {
                    SparkCard {
                        Text("Story Format", style = MaterialTheme.typography.titleSmall, color = SoftGold)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            SparkOutlinedButton(
                                text = "Photo",
                                icon = Icons.Default.AddPhotoAlternate,
                                onClick = {
                                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                modifier = Modifier.weight(1f)
                            )
                            SparkOutlinedButton(
                                text = "Video",
                                onClick = {
                                    videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                                },
                                modifier = Modifier.weight(1f)
                            )
                            SparkOutlinedButton(
                                text = "Text Card",
                                onClick = {
                                    mediaType = "text"
                                    pickedBitmap = null
                                    pickedVideoUri = ""
                                    localError = null
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (!previewBase64.isNullOrBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Base64Thumbnail(
                                base64Data = previewBase64.orEmpty(),
                                contentDescription = "Story preview",
                                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp))
                            )
                        } else if (pickedVideoUri.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text("Video attached: $pickedVideoUri", style = MaterialTheme.typography.bodySmall, color = ChampagneGold)
                        }
                    }
                }

                item {
                    SparkCard {
                        OutlinedTextField(
                            value = overlayText,
                            onValueChange = { overlayText = it },
                            label = { Text("Overlay Text on Story") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = caption,
                            onValueChange = { caption = it },
                            label = { Text("Caption") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                item {
                    SparkCard {
                        Text("Sticker Tag", style = MaterialTheme.typography.titleSmall, color = SoftGold)
                        Spacer(Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(MusicLibraryCatalog.stickerTags) { st ->
                                FilterChip(
                                    selected = stickerTag == st,
                                    onClick = { stickerTag = st },
                                    label = { Text(st) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ChampagneGold,
                                        selectedLabelColor = MidnightBlack
                                    )
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        Text("Permitted Studio Music", style = MaterialTheme.typography.titleSmall, color = SoftGold)
                        Spacer(Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(MusicLibraryCatalog.curatedTracks, key = { it.id }) { trk ->
                                FilterChip(
                                    selected = selectedTrack == trk.title,
                                    onClick = { selectedTrack = trk.title },
                                    label = { Text(trk.title) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ChampagneGold,
                                        selectedLabelColor = MidnightBlack
                                    )
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        Text("Story Privacy", style = MaterialTheme.typography.titleSmall, color = SoftGold)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("public", "followers", "private").forEach { vis ->
                                FilterChip(
                                    selected = visibility == vis,
                                    onClick = { visibility = vis },
                                    label = { Text(vis.replaceFirstChar { it.uppercase() }) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ChampagneGold,
                                        selectedLabelColor = MidnightBlack
                                    )
                                )
                            }
                        }
                    }
                }

                item {
                    SparkGoldButton(
                        text = "Publish 24-Hour Story",
                        onClick = {
                            if (mediaType == "photo" && pickedBitmap == null && overlayText.isBlank() && caption.isBlank()) {
                                localError = "Please select a photo, attach a video, or add story text first."
                                return@SparkGoldButton
                            }
                            if (mediaType == "video" && pickedVideoUri.isBlank()) {
                                localError = "Please pick a video file or switch to Photo / Text Card."
                                return@SparkGoldButton
                            }
                            if (mediaType == "text" && overlayText.isBlank() && caption.isBlank()) {
                                localError = "Please enter overlay text or a caption for your text story."
                                return@SparkGoldButton
                            }
                            localError = null
                            onCreateStory(
                                username,
                                avatarUrl,
                                mediaType,
                                pickedVideoUri,
                                pickedBitmap,
                                overlayText,
                                stickerTag,
                                caption,
                                selectedTrack,
                                visibility
                            )
                        },
                        modifier = Modifier.fillMaxWidth().testTag("publish_story_btn")
                    )
                    Spacer(Modifier.height(72.dp))
                }
            }

            if (isBusy) {
                SparkBusyOverlay("Publishing 24-hour Story...")
            }
        }
    }
}
