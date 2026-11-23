package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.core.MediaUtils
import com.example.ui.components.SparkBadge
import com.example.ui.components.SparkBusyOverlay
import com.example.ui.components.SparkCard
import com.example.ui.components.SparkGoldButton
import com.example.ui.components.SparkOutlinedButton
import com.example.ui.components.SparkStatusBanner
import com.example.ui.components.SparkTopBar
import kotlinx.coroutines.launch
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.MidnightBlack
import com.example.ui.theme.SecondaryIce
import com.example.ui.theme.SoftGold
import com.example.ui.theme.SparkBrushes
import com.example.ui.viewmodel.StudioUiState
import com.example.ui.viewmodel.StudioViewModel

@Composable
fun SingingPhotoScreen(
    uiState: StudioUiState,
    studioViewModel: StudioViewModel,
    onBack: () -> Unit,
    onNavigateJobs: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val tracks = studioViewModel.allMusicTracks()
    var title by remember { mutableStateOf("") }
    var portraitBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedTrackLabel by remember { mutableStateOf(tracks.firstOrNull()?.title ?: "Lagos Golden Pulse") }
    var lyricsOrSpeech by remember { mutableStateOf("") }
    var mood by remember { mutableStateOf("Soulful & Expressive") }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val moods = listOf("Soulful & Expressive", "Afrobeats Energetic", "Gospel Choir Praise", "Cinematic Monologue", "Comedy Rap")

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                portraitBitmap = MediaUtils.uriToBitmapAsync(context, uri, 720)
            }
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = tempCameraUri
        if (ok && uri != null) {
            scope.launch {
                portraitBitmap = MediaUtils.uriToBitmapAsync(context, uri, 720)
            }
        }
    }
    val cameraPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val uri = MediaUtils.createTempCameraUri(context)
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            studioViewModel.setErrorBanner("Camera permission denied.")
        }
    }
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            studioViewModel.addUploadedAudioTrack("Custom Vocal/Beat", "My Uploads", uri)
            selectedTrackLabel = "Custom Vocal/Beat (Uploaded)"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SparkBrushes.CosmicBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SparkTopBar(
                title = "AI Singing Photo",
                subtitle = "Lip-Sync Portrait Studio",
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
                SparkCard(borderColor = ChampagneGold.copy(alpha = 0.65f)) {
                    Text("Transform a Portrait into a Singing Performance", style = MaterialTheme.typography.titleMedium, color = ChampagneGold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Upload a clear portrait photo and choose a licensed track or audio file. Spark AI dispatches a real facial lip-sync job when your server-side video/lip-sync provider is connected (never faked with static images).",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryIce
                    )
                }
            }

            item {
                SparkCard {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Performance Title") },
                        placeholder = { Text("e.g., Lagos Sunset Chorus") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("singing_title_input")
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        SparkOutlinedButton(
                            text = "Pick Portrait",
                            icon = Icons.Default.PhotoLibrary,
                            onClick = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            modifier = Modifier.weight(1f).testTag("singing_pick_photo_btn")
                        )
                        SparkOutlinedButton(
                            text = "Camera",
                            icon = Icons.Default.CameraAlt,
                            onClick = {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                    val uri = MediaUtils.createTempCameraUri(context)
                                    tempCameraUri = uri
                                    cameraLauncher.launch(uri)
                                } else {
                                    cameraPermLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (portraitBitmap != null) {
                        Spacer(Modifier.height(10.dp))
                        Image(
                            bitmap = portraitBitmap!!.asImageBitmap(),
                            contentDescription = "Selected singing portrait",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(14.dp))
                        )
                    }
                }
            }

            item {
                SparkCard {
                    Text("Song / Audio Selection", style = MaterialTheme.typography.titleMedium, color = ChampagneGold)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(tracks, key = { it.id }) { trk ->
                            FilterChip(
                                selected = selectedTrackLabel == trk.title,
                                onClick = { selectedTrackLabel = trk.title },
                                label = { Text("${trk.title} (${trk.category})") },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ChampagneGold, selectedLabelColor = MidnightBlack)
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    SparkOutlinedButton(
                        text = "Upload Custom MP3 / WAV / M4A",
                        icon = Icons.Default.AudioFile,
                        onClick = { audioPicker.launch("audio/*") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = lyricsOrSpeech,
                        onValueChange = { lyricsOrSpeech = it },
                        label = { Text("Lyrics or Spoken Dialogue") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("Expression & Mood", style = MaterialTheme.typography.labelLarge, color = SoftGold)
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(moods) { m ->
                            FilterChip(
                                selected = mood == m,
                                onClick = { mood = m },
                                label = { Text(m) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ChampagneGold, selectedLabelColor = MidnightBlack)
                            )
                        }
                    }
                }
            }

            item {
                SparkGoldButton(
                    text = "Submit Singing Photo Job (3 cr on success)",
                    icon = Icons.Default.RecordVoiceOver,
                    onClick = {
                        if (title.isBlank()) {
                            studioViewModel.setErrorBanner("Please enter a performance title first.")
                            return@SparkGoldButton
                        }
                        val bmp = portraitBitmap
                        if (bmp == null) {
                            studioViewModel.setErrorBanner("Please pick or capture a portrait photo first.")
                            return@SparkGoldButton
                        }
                        studioViewModel.createSingingPhotoJob(
                            title = title,
                            portraitBitmap = bmp,
                            audioSourceLabel = selectedTrackLabel,
                            lyricsOrSpeech = lyricsOrSpeech,
                            mood = mood,
                            onDone = { if (it != null) onNavigateJobs() }
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("submit_singing_photo_btn")
                )
                Spacer(Modifier.height(6.dp))
                SparkBadge(label = "Note: Portrait photo required • 0 credits deducted if provider unconfigured")
                Spacer(Modifier.height(92.dp))
            }
        }
        }
        if (uiState.isBusy) {
            SparkBusyOverlay(uiState.busyLabel)
        }
    }
}
