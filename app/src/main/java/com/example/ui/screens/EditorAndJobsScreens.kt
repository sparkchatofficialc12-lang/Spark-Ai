package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.ClipItem
import com.example.data.model.GenerationJob
import com.example.data.model.SparkCatalog
import com.example.ui.components.SparkBadge
import com.example.ui.components.SparkBusyOverlay
import com.example.ui.components.SparkCard
import com.example.ui.components.SparkGoldButton
import com.example.ui.components.SparkOutlinedButton
import com.example.ui.components.SparkStatusBanner
import com.example.ui.components.SparkTopBar
import com.example.ui.navigation.SparkRoute
import com.example.ui.theme.AmberGold
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.MidnightBlack
import com.example.ui.theme.SecondaryIce
import com.example.ui.theme.SoftGold
import com.example.ui.theme.SparkBrushes
import com.example.ui.theme.SteelBorder
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.ProjectDetailUiState
import com.example.ui.viewmodel.StudioUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieEditorScreen(
    initialProjectId: String,
    studioState: StudioUiState,
    detailState: ProjectDetailUiState,
    onSelectProject: (String) -> Unit,
    onSaveClip: (String, ClipItem) -> Unit,
    onSplitClip: (String, ClipItem) -> Unit,
    onMoveClip: (String, ClipItem, Boolean) -> Unit,
    onDeleteClip: (String, String) -> Unit,
    onStitchExport: (com.example.data.model.MovieProject) -> Unit,
    onClearMessages: () -> Unit,
    onBack: () -> Unit,
    onNavigate: (SparkRoute) -> Unit
) {
    BackHandler { onBack() }

    val selectedProject = studioState.projects.firstOrNull { it.id == detailState.projectId }
        ?: studioState.projects.firstOrNull { it.id == initialProjectId }
        ?: studioState.projects.firstOrNull()

    LaunchedEffect(selectedProject?.id) {
        selectedProject?.id?.takeIf { it.isNotBlank() }?.let { pid ->
            if (detailState.projectId != pid) onSelectProject(pid)
        }
    }

    var targetClipForLocalAttach by remember { mutableStateOf<ClipItem?>(null) }
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val target = targetClipForLocalAttach
        val pid = selectedProject?.id
        if (uri != null && target != null && !pid.isNullOrBlank()) {
            onSaveClip(pid, target.copy(videoUrl = uri.toString(), status = "ready"))
        }
        targetClipForLocalAttach = null
    }

    Scaffold(
        topBar = {
            SparkTopBar(
                title = "Movie Timeline Editor",
                subtitle = selectedProject?.title ?: "Select a project",
                onBack = onBack,
                credits = studioState.credits?.balance,
                onCreditsClick = { onNavigate(SparkRoute.SubscriptionCredits) }
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
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    SparkStatusBanner(
                        statusMessage = detailState.statusMessage,
                        errorMessage = detailState.errorMessage,
                        onDismiss = onClearMessages
                    )
                }

                if (studioState.projects.isEmpty()) {
                    item {
                        SparkCard {
                            Text("No movie projects yet", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Create a Text, Image, or Script movie first to edit timeline clips, transitions, subtitles, and music.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SecondaryIce
                            )
                            Spacer(Modifier.height(12.dp))
                            SparkGoldButton(
                                text = "Create Movie",
                                onClick = { onNavigate(SparkRoute.CreateMovie("text_to_movie")) },
                                modifier = Modifier.fillMaxWidth().testTag("editor_create_movie_btn")
                            )
                        }
                    }
                } else {
                    item {
                        Text("Active Project", style = MaterialTheme.typography.titleSmall, color = SoftGold)
                        Spacer(Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(studioState.projects, key = { it.id }) { proj ->
                                FilterChip(
                                    selected = proj.id == selectedProject?.id,
                                    onClick = { onSelectProject(proj.id) },
                                    label = { Text(proj.title) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ChampagneGold,
                                        selectedLabelColor = MidnightBlack
                                    )
                                )
                            }
                        }
                    }

                    if (selectedProject != null) {
                        item {
                            SparkCard(borderColor = ChampagneGold.copy(alpha = 0.6f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(selectedProject.title, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "${selectedProject.genre} • ${SparkCatalog.formatDuration(selectedProject.targetDurationSec)} • ${selectedProject.resolution} (${selectedProject.aspectRatio})",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SecondaryIce
                                        )
                                    }
                                    SparkBadge(label = "${detailState.clips.size} clips")
                                }
                                Spacer(Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                    SparkGoldButton(
                                        text = "Stitch & Export MP4",
                                        icon = Icons.Default.MovieFilter,
                                        onClick = { onStitchExport(selectedProject) },
                                        modifier = Modifier.weight(1f).testTag("editor_stitch_btn")
                                    )
                                    SparkOutlinedButton(
                                        text = "Storyboard",
                                        onClick = { onNavigate(SparkRoute.ScriptStoryboard(selectedProject.id)) },
                                        modifier = Modifier.testTag("editor_open_storyboard_btn")
                                    )
                                }
                            }
                        }

                        if (detailState.clips.isEmpty()) {
                            item {
                                SparkCard {
                                    Text("No timeline clips yet", style = MaterialTheme.typography.titleSmall)
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Add a starter timeline clip to trim, reorder, add subtitles, or attach an MP4 stream.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SecondaryIce
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    SparkOutlinedButton(
                                        text = "Add Timeline Clip",
                                        onClick = {
                                            onSaveClip(
                                                selectedProject.id,
                                                ClipItem(
                                                    projectId = selectedProject.id,
                                                    clipOrder = 0,
                                                    title = "Opening Scene Clip",
                                                    prompt = selectedProject.description,
                                                    durationSec = 10,
                                                    trimStartMs = 0,
                                                    trimEndMs = 10000,
                                                    musicTrackName = selectedProject.musicStyle
                                                )
                                            )
                                        },
                                        modifier = Modifier.testTag("editor_add_clip_btn")
                                    )
                                }
                            }
                        } else {
                            items(detailState.clips, key = { it.id }) { clip ->
                                ClipEditorCard(
                                    clip = clip,
                                    onSave = { updated -> onSaveClip(selectedProject.id, updated) },
                                    onSplit = { onSplitClip(selectedProject.id, clip) },
                                    onMoveUp = { onMoveClip(selectedProject.id, clip, true) },
                                    onMoveDown = { onMoveClip(selectedProject.id, clip, false) },
                                    onAttachVideo = {
                                        targetClipForLocalAttach = clip
                                        videoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                        )
                                    },
                                    onDelete = { onDeleteClip(selectedProject.id, clip.id) }
                                )
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }

            if (detailState.isBusy) {
                SparkBusyOverlay("Stitching timeline clips via Video Rendering Worker...")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClipEditorCard(
    clip: ClipItem,
    onSave: (ClipItem) -> Unit,
    onSplit: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onAttachVideo: () -> Unit,
    onDelete: () -> Unit
) {
    var subtitleText by remember(clip.id, clip.subtitleText) { mutableStateOf(clip.subtitleText) }
    var musicName by remember(clip.id, clip.musicTrackName) { mutableStateOf(clip.musicTrackName) }
    var transition by remember(clip.id, clip.transitionType) { mutableStateOf(clip.transitionType) }
    var trimStartSec by remember(clip.id, clip.trimStartMs) { mutableStateOf((clip.trimStartMs / 1000).toString()) }
    var trimEndSec by remember(clip.id, clip.trimEndMs) { mutableStateOf((clip.trimEndMs / 1000).toString()) }
    var transitionExpanded by remember { mutableStateOf(false) }

    val transitions = listOf("Cross Dissolve", "Hard Cut", "Gold Fade", "Whip Pan", "Cinematic Zoom")

    SparkCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("#${clip.clipOrder + 1} • ${clip.title}", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${clip.durationSec}s • Status: ${clip.status}${if (clip.videoUrl.isNotBlank()) " • MP4 Linked" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (clip.videoUrl.isNotBlank()) SuccessGreen else SoftGold
                )
            }
            Row {
                IconButton(onClick = onMoveUp) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Move clip earlier", tint = ChampagneGold)
                }
                IconButton(onClick = onMoveDown) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = "Move clip later", tint = ChampagneGold)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete clip", tint = ErrorRed)
                }
            }
        }

        if (clip.prompt.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(clip.prompt, style = MaterialTheme.typography.bodySmall, color = SecondaryIce)
        }

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = trimStartSec,
                onValueChange = { trimStartSec = it.filter { ch -> ch.isDigit() } },
                label = { Text("Trim Start (s)") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = trimEndSec,
                onValueChange = { trimEndSec = it.filter { ch -> ch.isDigit() } },
                label = { Text("Trim End (s)") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(8.dp))
        ExposedDropdownMenuBox(
            expanded = transitionExpanded,
            onExpandedChange = { transitionExpanded = !transitionExpanded }
        ) {
            OutlinedTextField(
                value = transition,
                onValueChange = {},
                readOnly = true,
                label = { Text("Transition") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = transitionExpanded) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = transitionExpanded,
                onDismissRequest = { transitionExpanded = false }
            ) {
                transitions.forEach { t ->
                    DropdownMenuItem(
                        text = { Text(t) },
                        onClick = {
                            transition = t
                            transitionExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = subtitleText,
            onValueChange = { subtitleText = it },
            label = { Text("Subtitles / Captions") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = musicName,
            onValueChange = { musicName = it },
            label = { Text("Background Music Track") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            SparkOutlinedButton(
                text = "Split",
                icon = Icons.Default.ContentCut,
                onClick = onSplit,
                modifier = Modifier.weight(1f)
            )
            SparkOutlinedButton(
                text = "Attach MP4",
                icon = Icons.Default.AttachFile,
                onClick = onAttachVideo,
                modifier = Modifier.weight(1f)
            )
            SparkGoldButton(
                text = "Save",
                icon = Icons.Default.Save,
                onClick = {
                    val startMs = (trimStartSec.toIntOrNull() ?: 0).coerceAtLeast(0) * 1000
                    val endMs = (trimEndSec.toIntOrNull() ?: 10).coerceAtLeast((startMs / 1000) + 1) * 1000
                    onSave(
                        clip.copy(
                            trimStartMs = startMs,
                            trimEndMs = endMs,
                            durationSec = ((endMs - startMs) / 1000).coerceAtLeast(1),
                            transitionType = transition,
                            subtitleText = subtitleText,
                            musicTrackName = musicName
                        )
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun GenerationJobsScreen(
    studioState: StudioUiState,
    isFounderAdmin: Boolean,
    onUpdateJobStatus: (GenerationJob, String) -> Unit,
    onBack: () -> Unit,
    onNavigate: (SparkRoute) -> Unit
) {
    BackHandler { onBack() }

    Scaffold(
        topBar = {
            SparkTopBar(
                title = "Render & Generation Queue",
                subtitle = "${studioState.jobs.size} jobs tracked",
                onBack = onBack,
                credits = studioState.credits?.balance,
                onCreditsClick = { onNavigate(SparkRoute.SubscriptionCredits) }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(SparkBrushes.CosmicBackground)
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SparkCard(borderColor = ChampagneGold.copy(alpha = 0.5f)) {
                    Text("Credit Protection Guarantee", style = MaterialTheme.typography.titleSmall, color = ChampagneGold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Credits are only deducted when a video job finishes rendering. Paused, failed, or unconfigured jobs charge 0 credits and can be resumed safely.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryIce
                    )
                }
            }

            if (studioState.jobs.isEmpty()) {
                item {
                    SparkCard {
                        Text("No generation jobs yet", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Start a Text-to-Movie, Singing Photo, Music Video, or Template Remix to monitor render progress here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryIce
                        )
                    }
                }
            } else {
                items(studioState.jobs, key = { it.id }) { job ->
                    val statusColor = when (job.status) {
                        "completed" -> SuccessGreen
                        "failed", "cancelled" -> ErrorRed
                        "paused" -> AmberGold
                        else -> ChampagneGold
                    }
                    val progress = if (job.totalClips > 0) {
                        (job.completedClips.toFloat() / job.totalClips.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    SparkCard(borderColor = statusColor.copy(alpha = 0.6f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(job.movieTitle, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${job.jobType} • ${job.providerId}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SecondaryIce
                                )
                            }
                            SparkBadge(label = job.status.uppercase(), color = statusColor)
                        }
                        Spacer(Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            color = statusColor,
                            trackColor = SteelBorder,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Clips: ${job.completedClips}/${job.totalClips} • Estimated: ${job.estimatedCredits} cr • Charged: ${job.creditsCharged} cr",
                            style = MaterialTheme.typography.labelSmall,
                            color = SoftGold
                        )
                        if (job.errorMessage.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                job.errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (job.status == "failed") ErrorRed else SecondaryIce
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            if (job.status == "processing" || job.status == "queued") {
                                SparkOutlinedButton(
                                    text = "Pause",
                                    icon = Icons.Default.Pause,
                                    onClick = { onUpdateJobStatus(job, "paused") },
                                    modifier = Modifier.weight(1f)
                                )
                            } else if (job.status == "paused" || job.status == "failed") {
                                SparkOutlinedButton(
                                    text = "Resume",
                                    icon = Icons.Default.Refresh,
                                    onClick = { onUpdateJobStatus(job, "queued") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (job.status != "completed" && job.status != "cancelled") {
                                SparkOutlinedButton(
                                    text = "Cancel",
                                    icon = Icons.Default.Cancel,
                                    accent = ErrorRed,
                                    onClick = { onUpdateJobStatus(job, "cancelled") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (job.projectId.isNotBlank()) {
                                SparkGoldButton(
                                    text = "Editor",
                                    icon = Icons.Default.PlayArrow,
                                    onClick = { onNavigate(SparkRoute.MovieEditor(job.projectId)) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        if (isFounderAdmin && job.status == "paused") {
                            Spacer(Modifier.height(8.dp))
                            SparkOutlinedButton(
                                text = "Configure Video Provider API",
                                onClick = { onNavigate(SparkRoute.AdminApiConfig) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}
