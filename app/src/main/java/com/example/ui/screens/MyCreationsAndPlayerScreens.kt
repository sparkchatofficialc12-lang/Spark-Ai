package com.example.ui.screens

import android.content.pm.ActivityInfo
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.R
import com.example.core.MediaUtils
import com.example.core.findActivity
import com.example.data.model.MovieItem
import com.example.data.model.SparkCatalog
import com.example.ui.components.Base64Thumbnail
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
import com.example.ui.theme.SoftGold
import com.example.ui.theme.SparkBrushes
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.SocialUiState
import com.example.ui.viewmodel.StudioUiState

@Composable
fun MyCreationsScreen(
    studioState: StudioUiState,
    socialState: SocialUiState,
    onAttachLocalVideo: (MovieItem, Uri) -> Unit,
    onToggleFavorite: (MovieItem) -> Unit,
    onDownloadMovie: (MovieItem) -> Unit,
    onDeleteMovie: (String) -> Unit,
    onBatchDeleteMovies: (List<MovieItem>) -> Unit = {},
    onDeleteProject: (String) -> Unit = {},
    onClearBanners: () -> Unit,
    onBack: () -> Unit,
    onNavigate: (SparkRoute) -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current

    val filters = listOf(
        "All", "Movies", "Music Videos", "Singing", "Templates", "Projects", "Downloads", "Saved", "Drafts"
    )
    var selectedFilter by remember { mutableStateOf("All") }
    var targetAttachMovie by remember { mutableStateOf<MovieItem?>(null) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedMovieIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var confirmSingleDeleteMovie by remember { mutableStateOf<MovieItem?>(null) }
    var confirmBatchDelete by remember { mutableStateOf(false) }

    val attachLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val m = targetAttachMovie
        if (uri != null && m != null) {
            onAttachLocalVideo(m, uri)
        }
        targetAttachMovie = null
    }

    val filteredMovies = remember(studioState.movies, selectedFilter) {
        when (selectedFilter) {
            "Movies" -> studioState.movies.filter {
                !it.genre.contains("Music", true) && !it.genre.contains("Singing", true) && it.status != "draft"
            }
            "Music Videos" -> studioState.movies.filter { it.genre.contains("Music", true) }
            "Singing" -> studioState.movies.filter { it.genre.contains("Singing", true) }
            "Drafts" -> studioState.movies.filter { it.status == "draft" }
            else -> studioState.movies
        }
    }

    val savedPosts = remember(socialState.publicPosts, socialState.myPosts, socialState.savedPostIds) {
        (socialState.publicPosts + socialState.myPosts)
            .distinctBy { it.id }
            .filter { socialState.savedPostIds.contains(it.id) }
    }
    val projectLanguageById = remember(studioState.projects) {
        studioState.projects.associate { it.id to it.language }
    }

    Scaffold(
        topBar = {
            SparkTopBar(
                title = "My Creations & Movies",
                subtitle = "${studioState.movies.size} movies • ${studioState.projects.size} projects",
                onBack = onBack,
                credits = studioState.credits?.balance,
                onCreditsClick = { onNavigate(SparkRoute.SubscriptionCredits) },
                actions = {
                    IconButton(
                        onClick = {
                            selectionMode = !selectionMode
                            if (!selectionMode) selectedMovieIds = emptySet()
                        },
                        modifier = Modifier.testTag("creations_multi_select_btn")
                    ) {
                        Icon(
                            Icons.Default.Checklist,
                            contentDescription = "Multi-select",
                            tint = if (selectionMode) ChampagneGold else IceWhite
                        )
                    }
                    IconButton(
                        onClick = { onNavigate(SparkRoute.MovieLibrary) },
                        modifier = Modifier.testTag("creations_grid_library_btn")
                    ) {
                        Icon(Icons.Default.GridView, contentDescription = "Movie Library Grid", tint = ChampagneGold)
                    }
                    IconButton(
                        onClick = { onNavigate(SparkRoute.RecentlyDeleted) },
                        modifier = Modifier.testTag("creations_recycle_bin_btn")
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Recently Deleted", tint = ChampagneGold)
                    }
                }
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
                item {
                    SparkStatusBanner(
                        statusMessage = studioState.statusBanner,
                        errorMessage = studioState.errorBanner,
                        onDismiss = onClearBanners
                    )
                }

                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filters) { f ->
                            FilterChip(
                                selected = selectedFilter == f,
                                onClick = { selectedFilter = f },
                                label = { Text(f) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ChampagneGold,
                                    selectedLabelColor = MidnightBlack
                                )
                            )
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        SparkGoldButton(
                            text = "Create Another Video",
                            icon = Icons.Default.MovieCreation,
                            onClick = { onNavigate(SparkRoute.CreateMovie("text_to_movie")) },
                            modifier = Modifier.weight(1f)
                        )
                        SparkOutlinedButton(
                            text = "Recycle Bin (${studioState.recentlyDeleted.size})",
                            icon = Icons.Default.DeleteSweep,
                            onClick = { onNavigate(SparkRoute.RecentlyDeleted) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (selectionMode) {
                    item {
                        SparkCard(borderColor = ChampagneGold) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${selectedMovieIds.size} selected",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = ChampagneGold
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = {
                                        selectedMovieIds = filteredMovies.map { it.id }.toSet()
                                    }) { Text("Select All", color = SoftGold) }
                                    SparkOutlinedButton(
                                        text = "Delete (${selectedMovieIds.size})",
                                        accent = ErrorRed,
                                        onClick = { if (selectedMovieIds.isNotEmpty()) confirmBatchDelete = true }
                                    )
                                }
                            }
                        }
                    }
                }

                when (selectedFilter) {
                    "Templates" -> {
                        if (socialState.myTemplates.isEmpty()) {
                            item {
                                SparkCard {
                                    Text("No templates published yet", style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.height(6.dp))
                                    SparkGoldButton(
                                        text = "Publish a Template",
                                        onClick = { onNavigate(SparkRoute.UniversalPost(prefillType = "template")) }
                                    )
                                }
                            }
                        } else {
                            items(socialState.myTemplates, key = { it.id }) { tpl ->
                                SparkCard(onClick = { onNavigate(SparkRoute.TemplateDetail(tpl.id)) }) {
                                    Text(tpl.title, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "${tpl.category} • ${tpl.remixesCount} remixes • ${tpl.visibility}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SoftGold
                                    )
                                }
                            }
                        }
                    }
                    "Projects" -> {
                        items(studioState.projects, key = { it.id }) { proj ->
                            SparkCard(onClick = { onNavigate(SparkRoute.MovieEditor(proj.id)) }) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(proj.title, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "${proj.genre} • ${proj.language.ifBlank { "English" }} • ${SparkCatalog.formatDuration(proj.targetDurationSec)} • ${proj.status}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SecondaryIce
                                        )
                                    }
                                    SparkOutlinedButton(
                                        text = "Editor",
                                        onClick = { onNavigate(SparkRoute.MovieEditor(proj.id)) }
                                    )
                                    IconButton(onClick = { onDeleteProject(proj.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Move project to Recycle Bin", tint = ErrorRed)
                                    }
                                }
                            }
                        }
                    }
                    "Downloads" -> {
                        if (studioState.downloads.isEmpty()) {
                            item {
                                SparkCard {
                                    Text("No MP4 downloads yet", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "Completed movies can be saved directly to your Android Movies/SparkAI folder.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SecondaryIce
                                    )
                                }
                            }
                        } else {
                            items(studioState.downloads, key = { it.id }) { dl ->
                                SparkCard {
                                    Text(dl.movieTitle, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "${dl.resolution} • ${dl.format} • ${dl.status}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SuccessGreen
                                    )
                                }
                            }
                        }
                    }
                    "Saved" -> {
                        if (savedPosts.isEmpty()) {
                            item {
                                SparkCard {
                                    Text("No saved Spark Social posts yet", style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        } else {
                            items(savedPosts, key = { it.id }) { post ->
                                SparkCard(onClick = { onNavigate(SparkRoute.PostDetail(post.id)) }) {
                                    Text(post.title, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "@${post.creatorUsername} • ${post.creationType}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SoftGold
                                    )
                                }
                            }
                        }
                    }
                    else -> {
                        if (filteredMovies.isEmpty()) {
                            item {
                                SparkCard {
                                    Text("No creations in \"$selectedFilter\"", style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.height(8.dp))
                                    SparkGoldButton(
                                        text = "Create AI Movie",
                                        onClick = { onNavigate(SparkRoute.CreateMovie("text_to_movie")) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        } else {
                            items(filteredMovies, key = { it.id }) { movie ->
                                val isChecked = selectedMovieIds.contains(movie.id)
                                SparkCard {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (selectionMode) {
                                            IconButton(onClick = {
                                                selectedMovieIds = if (isChecked) {
                                                    selectedMovieIds - movie.id
                                                } else {
                                                    selectedMovieIds + movie.id
                                                }
                                            }) {
                                                Icon(
                                                    imageVector = if (isChecked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                                    contentDescription = "Select movie",
                                                    tint = if (isChecked) ChampagneGold else SecondaryIce
                                                )
                                            }
                                        }
                                        Base64Thumbnail(
                                            base64Data = movie.thumbnailData,
                                            contentDescription = movie.title,
                                            modifier = Modifier
                                                .width(92.dp)
                                                .height(68.dp)
                                                .clip(RoundedCornerShape(12.dp)),
                                            placeholderIcon = Icons.Default.Movie
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            val movieLang = projectLanguageById[movie.projectId].orEmpty()
                                            Text(movie.title, style = MaterialTheme.typography.titleMedium)
                                            Text(
                                                buildString {
                                                    append(movie.genre)
                                                    if (movieLang.isNotBlank()) append(" • $movieLang")
                                                    append(" • ${SparkCatalog.formatDuration(movie.durationSec)} • ${movie.resolution}")
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = SecondaryIce
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            SparkBadge(
                                                label = if (movie.videoUrl.isNotBlank()) "MP4 READY" else movie.status.uppercase(),
                                                color = if (movie.videoUrl.isNotBlank()) SuccessGreen else ChampagneGold
                                            )
                                        }
                                        IconButton(onClick = { onToggleFavorite(movie) }) {
                                            Icon(
                                                imageVector = if (movie.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                contentDescription = "Favorite",
                                                tint = if (movie.favorite) ChampagneGold else SecondaryIce
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        SparkGoldButton(
                                            text = "Watch",
                                            icon = Icons.Default.PlayArrow,
                                            onClick = {
                                                onNavigate(
                                                    SparkRoute.VideoPlayer(
                                                        title = movie.title,
                                                        videoUrl = movie.videoUrl,
                                                        movieId = movie.id,
                                                        subtitlesText = movie.subtitlesText,
                                                        resolution = movie.resolution
                                                    )
                                                )
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                        SparkOutlinedButton(
                                            text = "Attach MP4",
                                            icon = Icons.Default.AttachFile,
                                            onClick = {
                                                targetAttachMovie = movie
                                                attachLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                                )
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        SparkOutlinedButton(
                                            text = "Post",
                                            icon = Icons.Default.Public,
                                            onClick = {
                                                onNavigate(
                                                    SparkRoute.UniversalPost(
                                                        prefillTitle = movie.title,
                                                        prefillType = if (movie.durationSec > 60) "movie" else "short_video",
                                                        prefillProjectId = movie.projectId,
                                                        prefillVideoUrl = movie.videoUrl,
                                                        prefillThumbnail = movie.thumbnailData,
                                                        prefillDurationSec = movie.durationSec
                                                    )
                                                )
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                        SparkOutlinedButton(
                                            text = "Save MP4",
                                            icon = Icons.Default.Download,
                                            onClick = { onDownloadMovie(movie) },
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(onClick = {
                                            MediaUtils.shareMovieTextOrLink(context, movie.title, movie.videoUrl, movie.subtitlesText)
                                        }) {
                                            Icon(Icons.Default.Share, contentDescription = "Share", tint = ChampagneGold)
                                        }
                                        IconButton(onClick = { confirmSingleDeleteMovie = movie }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(92.dp)) }
            }

            if (studioState.isBusy) {
                SparkBusyOverlay(studioState.busyLabel)
            }
        }
    }

    confirmSingleDeleteMovie?.let { movie ->
        AlertDialog(
            onDismissRequest = { confirmSingleDeleteMovie = null },
            containerColor = com.example.ui.theme.SecondarySlate,
            title = { Text("Delete \"${movie.title}\"?", color = ErrorRed) },
            text = { Text("This movie will move to Recently Deleted (Recycle Bin) for 30 days before permanent removal.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteMovie(movie.id)
                    confirmSingleDeleteMovie = null
                }) { Text("Move to Recycle Bin", color = ErrorRed) }
            },
            dismissButton = {
                TextButton(onClick = { confirmSingleDeleteMovie = null }) { Text("Cancel") }
            }
        )
    }

    if (confirmBatchDelete) {
        val victims = studioState.movies.filter { selectedMovieIds.contains(it.id) }
        AlertDialog(
            onDismissRequest = { confirmBatchDelete = false },
            containerColor = com.example.ui.theme.SecondarySlate,
            title = { Text("Delete ${victims.size} Items?", color = ErrorRed) },
            text = { Text("Selected creations will be moved to Recently Deleted (Recycle Bin) with 30-day recovery.") },
            confirmButton = {
                TextButton(onClick = {
                    onBatchDeleteMovies(victims)
                    selectedMovieIds = emptySet()
                    selectionMode = false
                    confirmBatchDelete = false
                }) { Text("Move All to Bin", color = ErrorRed) }
            },
            dismissButton = {
                TextButton(onClick = { confirmBatchDelete = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    title: String,
    videoUrl: String,
    movieId: String,
    subtitlesText: String,
    resolution: String,
    postId: String,
    studioState: StudioUiState,
    socialState: SocialUiState,
    onMarkMovieViewed: (String) -> Unit,
    onDownloadMovie: (MovieItem) -> Unit,
    onTogglePostLike: (com.example.data.model.SocialPost) -> Unit,
    onToggleSavePost: (com.example.data.model.SocialPost) -> Unit,
    onRecordShare: (com.example.data.model.SocialPost, String) -> Unit,
    onOpenPostDetail: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var isFullscreen by remember { mutableStateOf(false) }
    var showSubtitles by remember { mutableStateOf(subtitlesText.isNotBlank()) }
    var speed by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(movieId) {
        if (movieId.isNotBlank()) onMarkMovieViewed(movieId)
    }

    DisposableEffect(isFullscreen) {
        if (isFullscreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    val exoPlayer = remember(videoUrl) {
        if (videoUrl.isBlank()) null
        else ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            prepare()
            playWhenReady = studioState.profile?.videoAutoplay ?: true
        }
    }

    LaunchedEffect(speed) {
        exoPlayer?.playbackParameters = PlaybackParameters(speed)
    }

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer?.release() }
    }

    val linkedPost = remember(postId, socialState.publicPosts, socialState.myPosts) {
        if (postId.isBlank()) null
        else (socialState.publicPosts + socialState.myPosts).firstOrNull { it.id == postId }
    }

    Scaffold(
        topBar = {
            if (!isFullscreen) {
                SparkTopBar(
                    title = title.ifBlank { "Spark AI Cinema Player" },
                    subtitle = "$resolution • Speed ${speed}x",
                    onBack = onBack
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MidnightBlack)
                .padding(innerPadding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (isFullscreen) Modifier.weight(1f) else Modifier.aspectRatio(16f / 9f))
                    .background(MidnightBlack),
                contentAlignment = Alignment.Center
            ) {
                if (exoPlayer != null) {
                    AndroidView(
                        factory = { ctx ->
                            (LayoutInflater.from(ctx).inflate(R.layout.view_spark_player, null, false) as PlayerView).apply {
                                player = exoPlayer
                                useController = true
                                layoutParams = FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }
                        },
                        update = { it.player = exoPlayer },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(Icons.Default.Movie, contentDescription = null, tint = ChampagneGold)
                        Spacer(Modifier.height(8.dp))
                        Text("No rendered MP4 stream attached yet", style = MaterialTheme.typography.titleMedium, color = ChampagneGold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Connect an external Video Provider API in Admin API Config or attach a local MP4 video in My Creations / Editor.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryIce
                        )
                    }
                }

                if (showSubtitles && subtitlesText.isNotBlank()) {
                    Surface(
                        color = MidnightBlack.copy(alpha = 0.78f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(start = 14.dp, end = 14.dp, bottom = 54.dp)
                    ) {
                        Text(
                            text = subtitlesText.lineSequence().take(3).joinToString("\n"),
                            style = MaterialTheme.typography.bodySmall,
                            color = IceWhite,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                IconButton(
                    onClick = {
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                        isFullscreen = !isFullscreen
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .testTag("player_fullscreen_btn")
                ) {
                    Icon(
                        imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = "Toggle fullscreen",
                        tint = ChampagneGold
                    )
                }
            }

            if (!isFullscreen) {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text("Playback Speed", style = MaterialTheme.typography.labelLarge, color = SoftGold)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(0.5f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { spd ->
                                FilterChip(
                                    selected = speed == spd,
                                    onClick = { speed = spd },
                                    label = { Text("${spd}x") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ChampagneGold,
                                        selectedLabelColor = MidnightBlack
                                    )
                                )
                            }
                        }
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            SparkOutlinedButton(
                                text = if (showSubtitles) "Subtitles On" else "Subtitles Off",
                                icon = Icons.Default.ClosedCaption,
                                onClick = { showSubtitles = !showSubtitles },
                                modifier = Modifier.weight(1f)
                            )
                            SparkOutlinedButton(
                                text = "Download MP4",
                                icon = Icons.Default.Download,
                                onClick = {
                                    onDownloadMovie(
                                        MovieItem(
                                            id = movieId.ifBlank { "player_dl" },
                                            title = title,
                                            videoUrl = videoUrl,
                                            resolution = resolution
                                        )
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (linkedPost != null) {
                        val isLiked = socialState.myLikedPostIds.contains(linkedPost.id)
                        val isSaved = socialState.savedPostIds.contains(linkedPost.id)
                        item {
                            SparkCard(borderColor = ChampagneGold.copy(alpha = 0.5f)) {
                                Text(
                                    "${linkedPost.viewsCount} views • ${linkedPost.likesCount} likes • ${linkedPost.commentsCount} comments",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SoftGold
                                )
                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    SparkOutlinedButton(
                                        text = if (isLiked) "Liked" else "Like",
                                        icon = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        onClick = { onTogglePostLike(linkedPost) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    SparkOutlinedButton(
                                        text = if (isSaved) "Saved" else "Save",
                                        icon = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        onClick = { onToggleSavePost(linkedPost) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    SparkGoldButton(
                                        text = "Comments",
                                        onClick = { onOpenPostDetail(linkedPost.id) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        SparkOutlinedButton(
                            text = "Share Video Link",
                            icon = Icons.Default.Share,
                            onClick = {
                                MediaUtils.shareMovieTextOrLink(context, title, videoUrl, subtitlesText)
                                linkedPost?.let { onRecordShare(it, "system_share") }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(72.dp))
                    }
                }
            }
        }
    }
}
