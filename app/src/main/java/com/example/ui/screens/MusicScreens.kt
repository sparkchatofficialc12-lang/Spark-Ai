package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicVideo
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MusicLibraryCatalog
import com.example.data.model.MusicTrack
import com.example.data.model.SparkCatalog
import com.example.ui.components.SparkBadge
import com.example.ui.components.SparkBusyOverlay
import com.example.ui.components.SparkCard
import com.example.ui.components.SparkGoldButton
import com.example.ui.components.SparkOutlinedButton
import com.example.ui.components.SparkStatusBanner
import com.example.ui.components.SparkTopBar
import com.example.ui.navigation.SparkRoute
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.MidnightBlack
import com.example.ui.theme.SecondaryIce
import com.example.ui.theme.SoftGold
import com.example.ui.theme.SparkBrushes
import com.example.ui.viewmodel.SocialUiState
import com.example.ui.viewmodel.SocialViewModel
import com.example.ui.viewmodel.StudioUiState
import com.example.ui.viewmodel.StudioViewModel

@Composable
fun MusicAndSoundsScreen(
    title: String,
    uiState: StudioUiState,
    socialState: SocialUiState,
    studioViewModel: StudioViewModel,
    socialViewModel: SocialViewModel,
    onBack: () -> Unit,
    onNavigate: (SparkRoute) -> Unit
) {
    BackHandler(onBack = onBack)
    val isAudioPlaying by studioViewModel.voiceAudio.isAudioPlaying.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val allTracks = studioViewModel.allMusicTracks()
    val filtered = remember(allTracks, searchQuery, selectedCategory) {
        allTracks.filter { trk ->
            val catOk = selectedCategory == "All" || trk.category.equals(selectedCategory, ignoreCase = true)
            val qOk = searchQuery.isBlank() ||
                trk.title.contains(searchQuery, ignoreCase = true) ||
                trk.artist.contains(searchQuery, ignoreCase = true) ||
                trk.mood.contains(searchQuery, ignoreCase = true)
            catOk && qOk
        }
    }

    val audioUploadLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            studioViewModel.addUploadedAudioTrack("My Audio ${uiState.uploadedTracks.size + 1}", "My Uploads", uri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SparkBrushes.CosmicBackground)
    ) {
        SparkTopBar(title = title, subtitle = "Licensed Tracks, Uploads & Sound Reuse", onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SparkStatusBanner(
                    statusMessage = uiState.statusBanner,
                    errorMessage = uiState.errorBanner,
                    onDismiss = { studioViewModel.clearBanners() }
                )
            }
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search songs, Afrobeats, Gospel, moods...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("music_search_input")
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(MusicLibraryCatalog.categories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ChampagneGold, selectedLabelColor = MidnightBlack)
                        )
                    }
                }
            }
            item {
                SparkCard(borderColor = ChampagneGold.copy(alpha = 0.5f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Upload Your Own Music / Audio", style = MaterialTheme.typography.titleSmall, color = ChampagneGold)
                            Text("Only upload audio you own or hold permission to use (MP3 / WAV / M4A).", style = MaterialTheme.typography.bodySmall, color = SecondaryIce)
                        }
                        Spacer(Modifier.width(8.dp))
                        SparkOutlinedButton(
                            text = "Upload",
                            icon = Icons.Default.AudioFile,
                            onClick = { audioUploadLauncher.launch("audio/*") }
                        )
                    }
                }
            }

            items(filtered, key = { it.id }) { track ->
                val isSaved = socialState.savedSoundIds.contains(track.id)
                val usedInCount = socialState.publicPosts.count { it.soundId == track.id }
                SparkCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MusicNote, contentDescription = null, tint = ChampagneGold)
                                Spacer(Modifier.width(6.dp))
                                Text(track.title, style = MaterialTheme.typography.titleMedium)
                            }
                            Text(
                                "${track.artist} • ${track.category} • ${track.bpm} BPM • ${track.mood}",
                                style = MaterialTheme.typography.bodySmall,
                                color = SecondaryIce
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                SparkBadge(label = track.licenseType)
                                SparkBadge(label = "$usedInCount videos", color = SoftGold)
                            }
                        }
                        IconButton(onClick = { socialViewModel.toggleSaveSound(track.id, track.title) }) {
                            Icon(
                                if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Save sound",
                                tint = ChampagneGold
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        if (track.localUri.isNotBlank()) {
                            SparkOutlinedButton(
                                text = if (isAudioPlaying) "Stop" else "Preview",
                                icon = if (isAudioPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                onClick = {
                                    if (isAudioPlaying) studioViewModel.voiceAudio.stopAudio()
                                    else studioViewModel.voiceAudio.playLocalAudio(Uri.parse(track.localUri), 0.9f)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        SparkGoldButton(
                            text = "Make Video",
                            icon = Icons.Default.MusicVideo,
                            onClick = { onNavigate(SparkRoute.MusicVideoStudio(track.id)) },
                            modifier = Modifier.weight(1f)
                        )
                        if (usedInCount > 0) {
                            SparkOutlinedButton(
                                text = "Videos",
                                onClick = { onNavigate(SparkRoute.ShortVideoReels(soundFilterId = track.id)) }
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(92.dp)) }
        }
    }
}

@Composable
fun MusicVideoStudioScreen(
    preselectedTrackId: String,
    uiState: StudioUiState,
    studioViewModel: StudioViewModel,
    onBack: () -> Unit,
    onNavigateJobs: () -> Unit
) {
    BackHandler(onBack = onBack)
    val tracks = studioViewModel.allMusicTracks()
    var selectedTrack by remember(preselectedTrackId, tracks) {
        mutableStateOf(tracks.firstOrNull { it.id == preselectedTrackId } ?: tracks.first())
    }
    var title by remember { mutableStateOf("") }
    var concept by remember { mutableStateOf("") }
    var style by remember { mutableStateOf(SparkCatalog.visualStyles.first()) }
    var aspectRatio by remember { mutableStateOf("9:16") }
    var resolution by remember { mutableStateOf("1080p") }
    var durationSec by remember { mutableIntStateOf(60) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SparkBrushes.CosmicBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SparkTopBar(
                title = "Music Video Creator",
                subtitle = "Beat-Synced Gemini Storyboard & Render",
                onBack = onBack,
                credits = uiState.credits?.balance
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    SparkStatusBanner(
                        statusMessage = uiState.statusBanner,
                        errorMessage = uiState.errorBanner,
                        onDismiss = { studioViewModel.clearBanners() }
                    )
                }
                item {
                SparkCard {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Music Video Title") },
                        placeholder = { Text("e.g., Lagos Gold Midnight Video") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("music_video_title_input")
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("Select Soundtrack", style = MaterialTheme.typography.labelLarge, color = SoftGold)
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(tracks, key = { it.id }) { trk ->
                            FilterChip(
                                selected = selectedTrack.id == trk.id,
                                onClick = { selectedTrack = trk },
                                label = { Text("${trk.title} (${trk.bpm} BPM)") },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ChampagneGold, selectedLabelColor = MidnightBlack)
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = concept,
                        onValueChange = { concept = it },
                        label = { Text("Visual Story & Choreography Concept") },
                        placeholder = { Text("Neon rooftop dance sequence with golden rain reflections in Victoria Island...") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                SparkCard {
                    Text("Visual Style & Format", style = MaterialTheme.typography.titleMedium, color = ChampagneGold)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(SparkCatalog.visualStyles) { st ->
                            FilterChip(
                                selected = style == st,
                                onClick = { style = st },
                                label = { Text(st) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ChampagneGold, selectedLabelColor = MidnightBlack)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(30, 60, 180).forEach { sec ->
                            FilterChip(
                                selected = durationSec == sec,
                                onClick = { durationSec = sec },
                                label = { Text(SparkCatalog.formatDuration(sec)) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ChampagneGold, selectedLabelColor = MidnightBlack)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("9:16", "16:9", "1:1").forEach { ar ->
                            FilterChip(
                                selected = aspectRatio == ar,
                                onClick = { aspectRatio = ar },
                                label = { Text(ar) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ChampagneGold, selectedLabelColor = MidnightBlack)
                            )
                        }
                    }
                }
            }

            item {
                SparkGoldButton(
                    text = "Generate Music Video (${SparkCatalog.estimateCredits(durationSec, resolution)} cr)",
                    icon = Icons.Default.MusicVideo,
                    onClick = {
                        studioViewModel.createMusicVideoJob(
                            title = title,
                            track = selectedTrack,
                            visualConcept = concept,
                            visualStyle = style,
                            aspectRatio = aspectRatio,
                            resolution = resolution,
                            durationSec = durationSec,
                            onDone = { if (it != null) onNavigateJobs() }
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("submit_music_video_btn")
                )
                Spacer(Modifier.height(92.dp))
            }
        }
        }
        if (uiState.isBusy) {
            SparkBusyOverlay(uiState.busyLabel)
        }
    }
}
