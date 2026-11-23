package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.MediaUtils
import com.example.data.model.CharacterProfile
import com.example.data.model.SceneItem
import com.example.data.model.ScriptDoc
import com.example.ui.components.Base64Thumbnail
import com.example.ui.components.SparkBadge
import com.example.ui.components.SparkBusyOverlay
import com.example.ui.components.SparkCard
import com.example.ui.components.SparkGoldButton
import com.example.ui.components.SparkOutlinedButton
import com.example.ui.components.SparkStatusBanner
import com.example.ui.components.SparkTopBar
import kotlinx.coroutines.launch
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.MidnightBlack
import com.example.ui.theme.SecondaryIce
import com.example.ui.theme.SoftGold
import com.example.ui.theme.SparkBrushes
import com.example.ui.viewmodel.ProjectDetailViewModel
import com.example.ui.viewmodel.StudioUiState
import com.example.ui.viewmodel.StudioViewModel

@Composable
fun CharacterStudioScreen(
    uiState: StudioUiState,
    studioViewModel: StudioViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var editingId by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("Lead Protagonist") }
    var ageRange by remember { mutableStateOf("25-35") }
    var appearance by remember { mutableStateOf("") }
    var clothing by remember { mutableStateOf("") }
    var hairstyle by remember { mutableStateOf("") }
    var expression by remember { mutableStateOf("Determined") }
    var fictional by remember { mutableStateOf(true) }
    var consentConfirmed by remember { mutableStateOf(true) }
    var portraitBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                portraitBitmap = MediaUtils.uriToBitmapAsync(context, uri, 512)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SparkBrushes.CosmicBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SparkTopBar(title = "Character Studio", subtitle = "Consistent Movie Cast", onBack = onBack)

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
                    Text(
                        if (editingId.isBlank()) "Create New Character" else "Edit Character",
                        style = MaterialTheme.typography.titleMedium,
                        color = ChampagneGold
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Character Name") }, modifier = Modifier.fillMaxWidth().testTag("character_name_input"))
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = role, onValueChange = { role = it }, label = { Text("Role") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = ageRange, onValueChange = { ageRange = it }, label = { Text("Age") }, modifier = Modifier.weight(0.7f))
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = appearance, onValueChange = { appearance = it }, label = { Text("Facial & Physical Appearance") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = clothing, onValueChange = { clothing = it }, label = { Text("Wardrobe / Clothing") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = hairstyle, onValueChange = { hairstyle = it }, label = { Text("Hairstyle") }, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { fictional = !fictional }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Fictional Character", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = fictional, onCheckedChange = { fictional = it }, colors = SwitchDefaults.colors(checkedThumbColor = MidnightBlack, checkedTrackColor = ChampagneGold))
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { consentConfirmed = !consentConfirmed }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Likeness Rights / Consent Confirmed", style = MaterialTheme.typography.bodySmall, color = SecondaryIce)
                        Switch(checked = consentConfirmed, onCheckedChange = { consentConfirmed = it }, colors = SwitchDefaults.colors(checkedThumbColor = MidnightBlack, checkedTrackColor = ChampagneGold))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SparkOutlinedButton(
                            text = "Upload Photo",
                            icon = Icons.Default.PhotoLibrary,
                            onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            modifier = Modifier.weight(1f)
                        )
                        SparkOutlinedButton(
                            text = "AI Portrait",
                            icon = Icons.Default.AutoAwesome,
                            onClick = {
                                studioViewModel.generateAiCharacterPortrait(
                                    CharacterProfile(name = name.ifBlank { "Lead Hero" }, role = role, ageRange = ageRange, appearance = appearance, clothing = clothing, hairstyle = hairstyle, facialExpression = expression),
                                    onGenerated = { portraitBitmap = it }
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (portraitBitmap != null) {
                        Spacer(Modifier.height(10.dp))
                        Image(
                            bitmap = portraitBitmap!!.asImageBitmap(),
                            contentDescription = "Character Portrait",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp))
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    SparkGoldButton(
                        text = if (editingId.isBlank()) "Save Character" else "Update Character",
                        onClick = {
                            if (name.isBlank()) {
                                studioViewModel.setErrorBanner("Please enter a character name.")
                                return@SparkGoldButton
                            }
                            if (!consentConfirmed) {
                                studioViewModel.setErrorBanner("Please confirm likeness rights / consent to save character.")
                                return@SparkGoldButton
                            }
                            studioViewModel.saveCharacter(
                                CharacterProfile(
                                    id = editingId,
                                    name = name,
                                    role = role,
                                    ageRange = ageRange,
                                    appearance = appearance,
                                    clothing = clothing,
                                    hairstyle = hairstyle,
                                    facialExpression = expression,
                                    fictional = fictional,
                                    likenessConsentConfirmed = consentConfirmed
                                ),
                                portraitBitmap
                            )
                            editingId = ""
                            name = ""
                            appearance = ""
                            clothing = ""
                            hairstyle = ""
                            portraitBitmap = null
                        },
                        modifier = Modifier.fillMaxWidth().testTag("save_character_btn")
                    )
                }
            }

            item { Text("Saved Cast (${uiState.characters.size})", style = MaterialTheme.typography.titleLarge) }
            items(uiState.characters, key = { it.id }) { ch ->
                SparkCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Base64Thumbnail(
                            base64Data = ch.referenceImageData,
                            contentDescription = ch.name,
                            placeholderIcon = Icons.Default.Face,
                            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(ch.name, style = MaterialTheme.typography.titleMedium)
                            Text("${ch.role} • ${ch.ageRange}", style = MaterialTheme.typography.bodySmall, color = SoftGold)
                            if (ch.clothing.isNotBlank()) {
                                Text(ch.clothing, style = MaterialTheme.typography.bodySmall, color = SecondaryIce, maxLines = 1)
                            }
                        }
                        IconButton(onClick = { studioViewModel.deleteCharacter(ch.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete character", tint = ErrorRed)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(92.dp)) }
        }
        }
        if (uiState.isBusy) {
            SparkBusyOverlay(uiState.busyLabel)
        }
    }
}

@Composable
fun ScriptStoryboardScreen(
    initialProjectId: String,
    uiState: StudioUiState,
    studioViewModel: StudioViewModel,
    projectDetailViewModel: ProjectDetailViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val detailState by projectDetailViewModel.state.collectAsStateWithLifecycle()
    val projects = uiState.projects
    var selectedProjId by remember(initialProjectId, projects) {
        mutableStateOf(initialProjectId.ifBlank { projects.firstOrNull()?.id.orEmpty() })
    }
    LaunchedEffect(selectedProjId) {
        if (selectedProjId.isNotBlank()) projectDetailViewModel.observeProject(selectedProjId)
    }

    val activeProject = projects.firstOrNull { it.id == selectedProjId }
    val matchingScript = uiState.scripts.firstOrNull { it.projectId == selectedProjId } ?: uiState.scripts.firstOrNull()

    var scriptTitle by remember(matchingScript?.id) { mutableStateOf(matchingScript?.title ?: "Untitled Screenplay") }
    var scriptContent by remember(matchingScript?.id) { mutableStateOf(matchingScript?.content ?: "") }
    var aiPrompt by remember { mutableStateOf("Sharpen dialogue tension and add camera cues") }
    var newSceneTitle by remember { mutableStateOf("") }
    var newSceneDesc by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SparkBrushes.CosmicBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SparkTopBar(title = "Script & Storyboard", subtitle = activeProject?.title ?: "Screenplay Desk", onBack = onBack)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    SparkStatusBanner(
                        statusMessage = uiState.statusBanner ?: detailState.statusMessage,
                        errorMessage = uiState.errorBanner ?: detailState.errorMessage,
                        onDismiss = { studioViewModel.clearBanners() }
                    )
                }
            if (projects.isNotEmpty()) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(projects, key = { it.id }) { p ->
                            FilterChip(
                                selected = p.id == selectedProjId,
                                onClick = { selectedProjId = p.id },
                                label = { Text(p.title) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ChampagneGold, selectedLabelColor = MidnightBlack)
                            )
                        }
                    }
                }
            }

            item {
                SparkCard {
                    Text("Non-Destructive Screenplay Editor", style = MaterialTheme.typography.titleMedium, color = ChampagneGold)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = scriptTitle, onValueChange = { scriptTitle = it }, label = { Text("Script Title") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = scriptContent, onValueChange = { scriptContent = it }, label = { Text("Screenplay Text") }, minLines = 5, maxLines = 12, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = aiPrompt, onValueChange = { aiPrompt = it }, label = { Text("AI Script Improvement Goal") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SparkOutlinedButton(
                            text = "Save Script",
                            onClick = {
                                studioViewModel.saveScript(
                                    (matchingScript ?: ScriptDoc(projectId = selectedProjId)).copy(
                                        title = scriptTitle,
                                        content = scriptContent
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                        SparkGoldButton(
                            text = "AI Suggest",
                            icon = Icons.Default.AutoAwesome,
                            onClick = {
                                studioViewModel.requestScriptSuggestion(
                                    (matchingScript ?: ScriptDoc(projectId = selectedProjId, title = scriptTitle, content = scriptContent))
                                        .copy(title = scriptTitle, content = scriptContent),
                                    aiPrompt
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (!matchingScript?.aiSuggestion.isNullOrBlank()) {
                        Spacer(Modifier.height(10.dp))
                        SparkBadge(label = "Gemini Script Notes (Original Preserved)")
                        Spacer(Modifier.height(6.dp))
                        Text(matchingScript!!.aiSuggestion, style = MaterialTheme.typography.bodySmall, color = SecondaryIce)
                    }
                }
            }

            if (selectedProjId.isNotBlank() && activeProject != null) {
                item {
                    SparkCard {
                        Text("Add Scene to Storyboard", style = MaterialTheme.typography.titleMedium, color = ChampagneGold)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = newSceneTitle, onValueChange = { newSceneTitle = it }, label = { Text("Scene Title") }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(value = newSceneDesc, onValueChange = { newSceneDesc = it }, label = { Text("Visual Action & Camera Shot") }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        SparkOutlinedButton(
                            text = "Add Scene",
                            icon = Icons.Default.Add,
                            onClick = {
                                if (newSceneTitle.isNotBlank()) {
                                    projectDetailViewModel.saveScene(
                                        selectedProjId,
                                        SceneItem(
                                            projectId = selectedProjId,
                                            sceneNumber = detailState.scenes.size + 1,
                                            title = newSceneTitle,
                                            description = newSceneDesc
                                        )
                                    )
                                    newSceneTitle = ""
                                    newSceneDesc = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                items(detailState.scenes, key = { it.id }) { sc ->
                    SparkCard {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Scene ${sc.sceneNumber}: ${sc.title}", style = MaterialTheme.typography.titleMedium, color = SoftGold)
                            SparkBadge(label = "${sc.durationSec}s")
                        }
                        Spacer(Modifier.height(6.dp))
                        Base64Thumbnail(
                            base64Data = sc.storyboardData,
                            contentDescription = sc.title,
                            modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(sc.description, style = MaterialTheme.typography.bodySmall, color = SecondaryIce)
                        Spacer(Modifier.height(8.dp))
                        SparkOutlinedButton(
                            text = "Generate AI Storyboard Frame",
                            icon = Icons.Default.AutoAwesome,
                            onClick = { projectDetailViewModel.generateStoryboardFrame(activeProject, sc) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(92.dp)) }
        }
        }
        if (uiState.isBusy || detailState.isBusy) {
            SparkBusyOverlay(if (detailState.isBusy) "Generating AI storyboard frame..." else uiState.busyLabel)
        }
    }
}

@Composable
fun VoiceMusicStudioScreen(
    studioViewModel: StudioViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val voices by studioViewModel.voiceAudio.supportedVoices.collectAsStateWithLifecycle()
    val isSpeaking by studioViewModel.voiceAudio.isSpeaking.collectAsStateWithLifecycle()
    val ttsReady by studioViewModel.voiceAudio.ttsReady.collectAsStateWithLifecycle()
    val statusMsg by studioViewModel.voiceAudio.statusMessage.collectAsStateWithLifecycle()

    var selectedVoice by remember(voices) { mutableStateOf(voices.firstOrNull()) }
    var text by remember { mutableStateOf("In the heart of Lagos, every shadow holds a secret. Imagine It. Create It. Watch It.") }
    var speed by remember { mutableFloatStateOf(1.0f) }
    var pitchOffset by remember { mutableFloatStateOf(0.0f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SparkBrushes.CosmicBackground)
    ) {
        SparkTopBar(title = "AI Voice Studio", subtitle = "Device Multi-Accent Narration", onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SparkCard {
                    Text(
                        if (ttsReady) "Text-to-Speech Engine Ready" else "Initializing Text-to-Speech Engine...",
                        style = MaterialTheme.typography.titleMedium,
                        color = ChampagneGold
                    )
                    if (!statusMsg.isNullOrBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(statusMsg!!, style = MaterialTheme.typography.bodySmall, color = SoftGold)
                    }
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(voices, key = { it.id }) { v ->
                            FilterChip(
                                selected = selectedVoice?.id == v.id,
                                onClick = { selectedVoice = v },
                                label = { Text(v.label) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ChampagneGold, selectedLabelColor = MidnightBlack)
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text("Narration or Character Dialogue") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth().testTag("tts_text_input")
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("Speed: ${"%.2f".format(speed)}x", style = MaterialTheme.typography.bodySmall, color = SoftGold)
                    Slider(
                        value = speed,
                        onValueChange = { speed = it },
                        valueRange = 0.5f..1.8f,
                        colors = SliderDefaults.colors(thumbColor = ChampagneGold, activeTrackColor = ChampagneGold)
                    )
                    Text("Tone / Pitch Offset: ${"%.2f".format(pitchOffset)}", style = MaterialTheme.typography.bodySmall, color = SoftGold)
                    Slider(
                        value = pitchOffset,
                        onValueChange = { pitchOffset = it },
                        valueRange = -0.35f..0.35f,
                        colors = SliderDefaults.colors(thumbColor = ChampagneGold, activeTrackColor = ChampagneGold)
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SparkGoldButton(
                            text = if (isSpeaking) "Speaking..." else "Preview Voice",
                            icon = if (isSpeaking) Icons.Default.Mic else Icons.Default.PlayArrow,
                            onClick = { studioViewModel.voiceAudio.speakPreview(text, selectedVoice, speed, pitchOffset) },
                            modifier = Modifier.weight(1f).testTag("tts_preview_btn")
                        )
                        SparkOutlinedButton(
                            text = "Stop",
                            icon = Icons.Default.Stop,
                            onClick = { studioViewModel.voiceAudio.stopSpeaking() },
                            modifier = Modifier.weight(0.6f)
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(92.dp)) }
        }
    }
}
