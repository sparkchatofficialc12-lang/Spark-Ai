package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.MediaUtils
import com.example.data.model.CommunityCatalog
import com.example.data.model.MovieItem
import com.example.data.model.MusicLibraryCatalog
import com.example.data.model.PostComment
import com.example.data.model.SocialPost
import com.example.data.model.SparkCatalog
import com.example.ui.components.Base64Thumbnail
import com.example.ui.components.CreatorAvatarCircle
import com.example.ui.components.SparkBadge
import com.example.ui.components.SparkBusyOverlay
import com.example.ui.components.SparkCard
import com.example.ui.components.SparkGoldButton
import com.example.ui.components.SparkOutlinedButton
import com.example.ui.components.SparkStatusBanner
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
import com.example.ui.viewmodel.SocialUiState
import com.example.ui.viewmodel.StudioUiState

@Composable
fun UniversalPostScreen(
    prefillTitle: String,
    prefillType: String,
    prefillProjectId: String,
    prefillVideoUrl: String,
    prefillThumbnail: String,
    prefillDurationSec: Int,
    prefillSoundId: String,
    prefillSoundTitle: String,
    studioState: StudioUiState,
    socialState: SocialUiState,
    onPublish: (
        creationType: String,
        mediaType: String,
        title: String,
        caption: String,
        hashtags: String,
        mentions: String,
        category: String,
        previewVideoUrl: String,
        thumbnailBitmap: Bitmap?,
        albumBitmaps: List<Bitmap>,
        existingThumbnailData: String,
        soundId: String,
        soundTitle: String,
        durationSec: Int,
        aspectRatio: String,
        resolution: String,
        visibility: String,
        allowComments: Boolean,
        allowReuse: Boolean,
        allowDownload: Boolean,
        linkedProjectId: String,
        reusableBasePrompt: String,
        alsoPublishReusableTemplate: Boolean
    ) -> Unit,
    onClearMessages: () -> Unit,
    onBack: (() -> Unit)? = null,
    onNavigate: (SparkRoute) -> Unit
) {
    if (onBack != null) BackHandler { onBack() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var creationType by remember { mutableStateOf(prefillType.ifBlank { "short_video" }) }
    var title by remember { mutableStateOf(prefillTitle) }
    var caption by remember { mutableStateOf("") }
    var hashtags by remember { mutableStateOf("#SparkAI #SparkSocial") }
    var mentions by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Movie scenes") }
    var videoUrl by remember { mutableStateOf(prefillVideoUrl) }
    var existingThumb by remember { mutableStateOf(prefillThumbnail) }
    var linkedProjectId by remember { mutableStateOf(prefillProjectId) }
    var durationSec by remember { mutableIntStateOf(prefillDurationSec) }
    var selectedSoundId by remember { mutableStateOf(prefillSoundId) }
    var selectedSoundTitle by remember { mutableStateOf(prefillSoundTitle) }
    var visibility by remember { mutableStateOf(studioState.profile?.defaultPrivacy ?: "public") }
    var allowComments by remember { mutableStateOf(true) }
    var allowReuse by remember { mutableStateOf(true) }
    var allowDownload by remember { mutableStateOf(true) }
    var alsoTemplate by remember { mutableStateOf(prefillType == "template") }
    var reusablePrompt by remember { mutableStateOf("") }

    var coverBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var albumBitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }

    val mediaType = when (creationType) {
        "text" -> "text"
        "photo" -> "photo"
        "photo_album" -> "photo_album"
        else -> "video"
    }

    val singlePhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val bmp = MediaUtils.uriToBitmapAsync(context, uri, 960)
                if (bmp != null) {
                    coverBitmap = bmp
                    existingThumb = ""
                }
            }
        }
    }

    val albumPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5)
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                val loaded = uris.mapNotNull { MediaUtils.uriToBitmapAsync(context, it, 720) }
                if (loaded.isNotEmpty()) {
                    albumBitmaps = loaded
                    if (creationType != "photo_album") creationType = "photo_album"
                }
            }
        }
    }

    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            videoUrl = uri.toString()
        }
    }

    Scaffold(
        topBar = {
            if (onBack != null) {
                SparkTopBar(title = "Create & Publish", subtitle = "Spark AI & Spark Social", onBack = onBack)
            }
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
                item {
                    SparkStatusBanner(
                        statusMessage = socialState.statusMessage,
                        errorMessage = socialState.errorMessage,
                        onDismiss = onClearMessages
                    )
                }

                // AI Quick Launchers inside Create Tab
                item {
                    Text("AI Creation Studio Shortcuts", style = MaterialTheme.typography.titleSmall, color = SoftGold)
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            SparkGoldButton(
                                text = "AI Movie",
                                icon = Icons.Default.AutoAwesome,
                                onClick = { onNavigate(SparkRoute.CreateMovie("text_to_movie")) }
                            )
                        }
                        item {
                            SparkOutlinedButton(
                                text = "Singing Photo",
                                onClick = { onNavigate(SparkRoute.SingingPhoto) }
                            )
                        }
                        item {
                            SparkOutlinedButton(
                                text = "Music Video",
                                onClick = { onNavigate(SparkRoute.MusicVideoStudio()) }
                            )
                        }
                        item {
                            SparkOutlinedButton(
                                text = "24h Story",
                                onClick = { onNavigate(SparkRoute.CreateStory) }
                            )
                        }
                    }
                }

                item {
                    SparkCard {
                        Text("Post Format", style = MaterialTheme.typography.titleSmall, color = SoftGold)
                        Spacer(Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(MusicLibraryCatalog.creationTypes) { (key, label) ->
                                FilterChip(
                                    selected = creationType == key,
                                    onClick = { creationType = key },
                                    label = { Text(label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ChampagneGold,
                                        selectedLabelColor = MidnightBlack
                                    )
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Title *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("post_title_input")
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = caption,
                            onValueChange = { caption = it },
                            label = { Text("Caption / Story Description") },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth().testTag("post_caption_input")
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = hashtags,
                                onValueChange = { hashtags = it },
                                label = { Text("Hashtags") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = mentions,
                                onValueChange = { mentions = it },
                                label = { Text("@Mentions") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Media Attachments (Zero-permission Photo Picker & Movie link)
                item {
                    SparkCard {
                        Text("Media & Studio Assets", style = MaterialTheme.typography.titleSmall, color = SoftGold)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            SparkOutlinedButton(
                                text = "Cover Photo",
                                icon = Icons.Default.AddPhotoAlternate,
                                onClick = {
                                    singlePhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                modifier = Modifier.weight(1f)
                            )
                            SparkOutlinedButton(
                                text = "Album (1-5)",
                                icon = Icons.Default.PhotoLibrary,
                                onClick = {
                                    albumPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                modifier = Modifier.weight(1f)
                            )
                            SparkOutlinedButton(
                                text = "Pick MP4",
                                icon = Icons.Default.VideoFile,
                                onClick = {
                                    videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (studioState.movies.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text("Or link a movie from My Creations:", style = MaterialTheme.typography.labelSmall, color = SecondaryIce)
                            Spacer(Modifier.height(4.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(studioState.movies.take(6), key = { it.id }) { m ->
                                    FilterChip(
                                        selected = linkedProjectId == m.projectId && title == m.title,
                                        onClick = {
                                            title = m.title
                                            videoUrl = m.videoUrl
                                            existingThumb = m.thumbnailData
                                            linkedProjectId = m.projectId
                                            durationSec = m.durationSec
                                        },
                                        label = { Text(m.title) }
                                    )
                                }
                            }
                        }

                        if (albumBitmaps.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text("${albumBitmaps.size} album photos selected", style = MaterialTheme.typography.bodySmall, color = ChampagneGold)
                        } else if (coverBitmap != null || existingThumb.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            val previewData by produceState(
                                initialValue = existingThumb,
                                key1 = coverBitmap,
                                key2 = existingThumb
                            ) {
                                value = coverBitmap?.let { MediaUtils.bitmapToCompactBase64Async(it, 120_000) } ?: existingThumb
                            }
                            Base64Thumbnail(
                                base64Data = previewData,
                                contentDescription = "Selected cover",
                                modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(12.dp))
                            )
                        }
                        if (videoUrl.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text("Video linked: $videoUrl", style = MaterialTheme.typography.labelSmall, color = ChampagneGold)
                        }
                    }
                }

                // Category, Sound, Visibility, Controls
                item {
                    SparkCard {
                        Text("Category", style = MaterialTheme.typography.titleSmall, color = SoftGold)
                        Spacer(Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(CommunityCatalog.categories.filter { it != "All" }) { cat ->
                                FilterChip(
                                    selected = category == cat,
                                    onClick = { category = cat },
                                    label = { Text(cat) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ChampagneGold,
                                        selectedLabelColor = MidnightBlack
                                    )
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        Text("Background Sound", style = MaterialTheme.typography.titleSmall, color = SoftGold)
                        Spacer(Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(MusicLibraryCatalog.curatedTracks, key = { it.id }) { trk ->
                                FilterChip(
                                    selected = selectedSoundId == trk.id,
                                    onClick = {
                                        if (selectedSoundId == trk.id) {
                                            selectedSoundId = ""
                                            selectedSoundTitle = ""
                                        } else {
                                            selectedSoundId = trk.id
                                            selectedSoundTitle = "${trk.title} — ${trk.artist}"
                                        }
                                    },
                                    label = { Text(trk.title) }
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        Text("Visibility", style = MaterialTheme.typography.titleSmall, color = SoftGold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CommunityCatalog.visibilities.forEach { vis ->
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

                        Spacer(Modifier.height(8.dp))
                        SwitchRow("Allow comments", allowComments) { allowComments = it }
                        SwitchRow("Allow Use Template / Remix", allowReuse) { allowReuse = it }
                        SwitchRow("Allow MP4 download", allowDownload) { allowDownload = it }
                        SwitchRow("Also publish as reusable Community Template", alsoTemplate) { alsoTemplate = it }

                        if (alsoTemplate || creationType == "template") {
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = reusablePrompt,
                                onValueChange = { reusablePrompt = it },
                                label = { Text("Reusable AI Template Prompt") },
                                minLines = 2,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                item {
                    SparkGoldButton(
                        text = "Publish to Spark Social",
                        onClick = {
                            val linkedMovie = studioState.movies.firstOrNull {
                                linkedProjectId.isNotBlank() && it.projectId == linkedProjectId
                            }
                            val resolvedAspectRatio = linkedMovie?.aspectRatio?.takeIf { it.isNotBlank() }
                                ?: studioState.profile?.defaultAspectRatio?.takeIf { it.isNotBlank() }
                                ?: "16:9"
                            val resolvedResolution = linkedMovie?.resolution?.takeIf { it.isNotBlank() }
                                ?: studioState.profile?.defaultResolution?.takeIf { it.isNotBlank() }
                                ?: "1080p"
                            onPublish(
                                creationType,
                                mediaType,
                                title,
                                caption,
                                hashtags,
                                mentions,
                                category,
                                videoUrl,
                                coverBitmap,
                                albumBitmaps,
                                existingThumb,
                                selectedSoundId,
                                selectedSoundTitle,
                                durationSec,
                                resolvedAspectRatio,
                                resolvedResolution,
                                visibility,
                                allowComments,
                                allowReuse,
                                allowDownload,
                                linkedProjectId,
                                reusablePrompt,
                                alsoTemplate
                            )
                        },
                        modifier = Modifier.fillMaxWidth().testTag("publish_post_submit_btn")
                    )
                }

                item { Spacer(Modifier.height(92.dp)) }
            }

            if (socialState.isBusy) {
                SparkBusyOverlay("Publishing to Spark Social...")
            }
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChanged: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onChanged(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f).padding(end = 8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChanged,
            colors = SwitchDefaults.colors(checkedThumbColor = MidnightBlack, checkedTrackColor = ChampagneGold)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PostDetailScreen(
    postId: String,
    currentUid: String,
    myUsername: String,
    socialState: SocialUiState,
    onOpenPost: (String) -> Unit,
    onToggleLike: (SocialPost) -> Unit,
    onToggleSave: (SocialPost) -> Unit,
    onRepost: (SocialPost) -> Unit,
    onAddComment: (SocialPost, String, String) -> Unit,
    onToggleCommentLike: (PostComment) -> Unit,
    onDeleteComment: (SocialPost, String) -> Unit,
    onRecordShare: (SocialPost, String) -> Unit,
    onToggleFollow: (String) -> Unit,
    onUseTemplate: (SocialPost) -> Unit,
    onReport: (SocialPost, String) -> Unit,
    onDeletePost: (String) -> Unit,
    myMovies: List<MovieItem> = emptyList(),
    onEditPost: (postId: String, title: String, caption: String, hashtags: String, visibility: String, allowComments: Boolean, allowReuse: Boolean, allowDownload: Boolean) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onReplaceVideo: (post: SocialPost, newVideoUrl: String, newThumb: String, durationSec: Int, res: String, keepLink: Boolean, title: String, caption: String) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onSoftDeletePost: (SocialPost) -> Unit = { p -> onDeletePost(p.id) },
    onChangePrivacy: (String, String) -> Unit = { _, _ -> },
    onToggleComments: (String, Boolean) -> Unit = { _, _ -> },
    onUpdateTemplatePermission: (String, Boolean) -> Unit = { _, _ -> },
    onClearMessages: () -> Unit = {},
    onBack: () -> Unit,
    onNavigate: (SparkRoute) -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current

    LaunchedEffect(postId, socialState.publicPosts.size, socialState.myPosts.size) {
        if (socialState.selectedPost?.id != postId) {
            onOpenPost(postId)
        }
    }

    val post = socialState.selectedPost?.takeIf { it.id == postId }
        ?: (socialState.publicPosts + socialState.myPosts).firstOrNull { it.id == postId }

    var commentText by remember { mutableStateOf("") }
    var replyToComment by remember { mutableStateOf<PostComment?>(null) }
    var showViewersSheet by remember { mutableStateOf(false) }
    var showLikesSheet by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var reportReason by remember { mutableStateOf(CommunityCatalog.reportReasons.first()) }

    Scaffold(
        topBar = {
            SparkTopBar(
                title = post?.title ?: "Spark Social Post",
                subtitle = post?.let { "@${it.creatorUsername}" },
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
            if (post == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading post...", style = MaterialTheme.typography.bodyMedium, color = SecondaryIce)
                }
            } else {
                val isLiked = socialState.myLikedPostIds.contains(post.id)
                val isSaved = socialState.savedPostIds.contains(post.id)
                val isFollowing = socialState.followingCreatorIds.contains(post.creatorId)
                val isOwner = post.creatorId == currentUid

                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        SparkStatusBanner(
                            statusMessage = socialState.statusMessage,
                            errorMessage = socialState.errorMessage,
                            onDismiss = onClearMessages
                        )
                    }
                    item {
                        SparkCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f).clickable {
                                        onNavigate(SparkRoute.CreatorProfileRoute(post.creatorId))
                                    }
                                ) {
                                    CreatorAvatarCircle(post.creatorUsername)
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text("@${post.creatorUsername}", style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "${post.category} • ${post.visibility}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SecondaryIce
                                        )
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (!isOwner) {
                                        SparkOutlinedButton(
                                            text = if (isFollowing) "Following" else "Follow",
                                            onClick = { onToggleFollow(post.creatorId) }
                                        )
                                    }
                                    PostMoreMenuAndDialogs(
                                        post = post,
                                        isOwner = isOwner,
                                        isSaved = isSaved,
                                        myMovies = myMovies,
                                        onToggleSave = onToggleSave,
                                        onShare = { showShareSheet = true },
                                        onEditPost = onEditPost,
                                        onReplaceVideo = onReplaceVideo,
                                        onSoftDeletePost = { p ->
                                            onSoftDeletePost(p)
                                            onBack()
                                        },
                                        onChangePrivacy = onChangePrivacy,
                                        onToggleComments = onToggleComments,
                                        onUpdateTemplatePermission = onUpdateTemplatePermission
                                    )
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            if (post.photoThumbnails.size > 1) {
                                val pagerState = rememberPagerState(pageCount = { post.photoThumbnails.size })
                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 10f).clip(RoundedCornerShape(14.dp))
                                ) { idx ->
                                    Base64Thumbnail(
                                        base64Data = post.photoThumbnails[idx],
                                        contentDescription = "${post.title} photo ${idx + 1}",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                            } else if (post.thumbnailData.isNotBlank() || post.mediaType == "video") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(16f / 9f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            if (post.mediaType == "video") {
                                                onNavigate(
                                                    SparkRoute.VideoPlayer(
                                                        title = post.title,
                                                        videoUrl = post.previewVideoUrl,
                                                        subtitlesText = post.caption,
                                                        resolution = post.resolution,
                                                        postId = post.id
                                                    )
                                                )
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Base64Thumbnail(
                                        base64Data = post.thumbnailData,
                                        contentDescription = post.title,
                                        modifier = Modifier.fillMaxSize(),
                                        placeholderIcon = Icons.Default.Movie
                                    )
                                    if (post.mediaType == "video") {
                                        SparkGoldButton(
                                            text = "Watch Video",
                                            icon = Icons.Default.PlayArrow,
                                            onClick = {
                                                onNavigate(
                                                    SparkRoute.VideoPlayer(
                                                        title = post.title,
                                                        videoUrl = post.previewVideoUrl,
                                                        subtitlesText = post.caption,
                                                        resolution = post.resolution,
                                                        postId = post.id
                                                    )
                                                )
                                            }
                                        )
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                            }

                            Text(post.title, style = MaterialTheme.typography.titleLarge)
                            if (post.caption.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(post.caption, style = MaterialTheme.typography.bodyMedium, color = SecondaryIce)
                            }
                            if (post.hashtags.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(post.hashtags, style = MaterialTheme.typography.bodySmall, color = ChampagneGold)
                            }
                            if (post.soundTitle.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(post.soundTitle, style = MaterialTheme.typography.labelSmall, color = SoftGold)
                                }
                            }

                            Spacer(Modifier.height(10.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = ChampagneGold.copy(alpha = 0.14f),
                                    border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .heightIn(min = 40.dp)
                                        .clip(RoundedCornerShape(50))
                                        .clickable { showViewersSheet = true }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = "${post.viewsCount} views (${post.uniqueViewersCount} unique)",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = ChampagneGold
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = ChampagneGold.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, SoftGold.copy(alpha = 0.45f)),
                                    modifier = Modifier
                                        .heightIn(min = 40.dp)
                                        .clip(RoundedCornerShape(50))
                                        .clickable { showLikesSheet = true }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = "${post.likesCount} likes",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = SoftGold
                                        )
                                    }
                                }
                                Text(
                                    text = "${post.sharesCount} shares • ${post.repostsCount} reposts",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SecondaryIce,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                                )
                            }

                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                SparkOutlinedButton(
                                    text = if (isLiked) "Liked" else "Like",
                                    icon = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    onClick = { onToggleLike(post) },
                                    modifier = Modifier.weight(1f)
                                )
                                SparkOutlinedButton(
                                    text = if (isSaved) "Saved" else "Save",
                                    icon = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    onClick = { onToggleSave(post) },
                                    modifier = Modifier.weight(1f)
                                )
                                SparkOutlinedButton(
                                    text = "Share",
                                    icon = Icons.Default.Share,
                                    onClick = { showShareSheet = true },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                SparkOutlinedButton(
                                    text = "Repost",
                                    icon = Icons.Default.Repeat,
                                    onClick = { onRepost(post) },
                                    modifier = Modifier.weight(1f)
                                )
                                if (post.allowReuse || post.linkedTemplateId.isNotBlank()) {
                                    SparkGoldButton(
                                        text = "USE TEMPLATE",
                                        onClick = { onUseTemplate(post) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                IconButton(onClick = { showReportDialog = true }) {
                                    Icon(Icons.Default.Flag, contentDescription = "Report post", tint = SoftGold)
                                }
                            }
                        }
                    }

                    // Comments section
                    item {
                        Text("Comments (${socialState.selectedPostComments.size})", style = MaterialTheme.typography.titleMedium, color = SoftGold)
                    }

                    if (post.allowComments) {
                        item {
                            SparkCard {
                                if (replyToComment != null) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Replying to @${replyToComment!!.username}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ChampagneGold
                                        )
                                        IconButton(onClick = { replyToComment = null }) {
                                            Icon(Icons.Default.Close, contentDescription = "Cancel reply")
                                        }
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = commentText,
                                        onValueChange = { commentText = it },
                                        label = { Text("Add a comment...") },
                                        modifier = Modifier.weight(1f).testTag("post_comment_input")
                                    )
                                    IconButton(
                                        onClick = {
                                            if (commentText.isNotBlank()) {
                                                onAddComment(post, replyToComment?.id.orEmpty(), commentText)
                                                commentText = ""
                                                replyToComment = null
                                            }
                                        },
                                        modifier = Modifier.testTag("post_comment_send_btn")
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Post comment", tint = ChampagneGold)
                                    }
                                }
                            }
                        }
                    }

                    items(socialState.selectedPostComments, key = { it.id }) { comment ->
                        val commentLiked = socialState.myLikedCommentIds.contains(comment.id)
                        val canDelete = comment.userId == currentUid || isOwner
                        val indent = if (comment.parentCommentId.isNotBlank()) 20.dp else 0.dp
                        SparkCard(modifier = Modifier.padding(start = indent)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("@${comment.username}", style = MaterialTheme.typography.labelLarge, color = ChampagneGold)
                                Row {
                                    IconButton(onClick = { onToggleCommentLike(comment) }) {
                                        Icon(
                                            imageVector = if (commentLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = "Like comment",
                                            tint = if (commentLiked) ChampagneGold else SecondaryIce,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Text("${comment.likesCount}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.CenterVertically))
                                    IconButton(onClick = { replyToComment = comment }) {
                                        Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = "Reply", tint = SoftGold, modifier = Modifier.size(18.dp))
                                    }
                                    if (canDelete) {
                                        IconButton(onClick = { onDeleteComment(post, comment.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete comment", tint = ErrorRed, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                            Text(comment.text, style = MaterialTheme.typography.bodySmall, color = IceWhite)
                        }
                    }
                    item { Spacer(Modifier.height(92.dp)) }
                }

                if (showViewersSheet) {
                    ModalBottomSheet(onDismissRequest = { showViewersSheet = false }, containerColor = SecondarySlate) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("View Activity (${post.viewsCount} total • ${post.uniqueViewersCount} unique)", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            if (socialState.selectedPostViews.isEmpty()) {
                                Text("No public viewer profiles visible under current privacy settings.", style = MaterialTheme.typography.bodySmall, color = SecondaryIce)
                            } else {
                                socialState.selectedPostViews.forEach { v ->
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                        CreatorAvatarCircle(v.viewerUsername)
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "${if (v.viewerPublic) "@${v.viewerUsername}" else "Private Viewer"} • ${v.viewCount} views",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(20.dp))
                        }
                    }
                }

                if (showLikesSheet) {
                    ModalBottomSheet(onDismissRequest = { showLikesSheet = false }, containerColor = SecondarySlate) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Likes (${post.likesCount})", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            if (socialState.selectedPostLikes.isEmpty()) {
                                Text("No public likers visible under current privacy settings.", style = MaterialTheme.typography.bodySmall, color = SecondaryIce)
                            } else {
                                socialState.selectedPostLikes.forEach { lk ->
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                        CreatorAvatarCircle(lk.username)
                                        Spacer(Modifier.width(8.dp))
                                        Text(if (lk.userPublic) "@${lk.username}" else "Spark Member", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                            Spacer(Modifier.height(20.dp))
                        }
                    }
                }

                if (showShareSheet) {
                    val shareLink = "https://sparkai.app/post/${post.id}"
                    ModalBottomSheet(onDismissRequest = { showShareSheet = false }, containerColor = SecondarySlate) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Share \"${post.title}\"", style = MaterialTheme.typography.titleLarge)
                            SparkGoldButton(
                                text = "Copy Post Link",
                                icon = Icons.Default.ContentCopy,
                                onClick = {
                                    val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cb.setPrimaryClip(ClipData.newPlainText("Spark AI Link", shareLink))
                                    onRecordShare(post, "copy_link")
                                    showShareSheet = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            SparkOutlinedButton(
                                text = "Share via WhatsApp",
                                icon = Icons.Default.Share,
                                onClick = {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        setPackage("com.whatsapp")
                                        putExtra(Intent.EXTRA_TEXT, "Watch \"${post.title}\" on Spark Social: $shareLink")
                                    }
                                    try {
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        MediaUtils.shareMovieTextOrLink(context, post.title, shareLink, post.caption)
                                    }
                                    onRecordShare(post, "whatsapp")
                                    showShareSheet = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            SparkOutlinedButton(
                                text = "Share to Phone Apps",
                                icon = Icons.Default.Share,
                                onClick = {
                                    MediaUtils.shareMovieTextOrLink(context, post.title, shareLink, post.caption)
                                    onRecordShare(post, "system_share")
                                    showShareSheet = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }

                if (showReportDialog) {
                    AlertDialog(
                        onDismissRequest = { showReportDialog = false },
                        containerColor = SecondarySlate,
                        title = { Text("Report Content") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                CommunityCatalog.reportReasons.forEach { reason ->
                                    FilterChip(
                                        selected = reportReason == reason,
                                        onClick = { reportReason = reason },
                                        label = { Text(reason) }
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                onReport(post, reportReason)
                                showReportDialog = false
                            }) { Text("Submit Report", color = ChampagneGold) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showReportDialog = false }) { Text("Cancel") }
                        }
                    )
                }
            }
        }
    }
}
