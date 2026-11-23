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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.core.MediaUtils
import com.example.data.model.SparkCatalog
import com.example.ui.components.SparkBadge
import com.example.ui.components.SparkBusyOverlay
import com.example.ui.components.SparkCard
import com.example.ui.components.SparkGoldButton
import com.example.ui.components.SparkOutlinedButton
import com.example.ui.components.SparkStatusBanner
import com.example.ui.components.SparkTopBar
import kotlinx.coroutines.launch
import com.example.ui.navigation.SparkRoute
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.MidnightBlack
import com.example.ui.theme.SecondaryIce
import com.example.ui.theme.SoftGold
import com.example.ui.theme.SparkBrushes
import com.example.ui.viewmodel.StudioUiState
import com.example.ui.viewmodel.StudioViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateMovieScreen(
    route: SparkRoute.CreateMovie,
    uiState: StudioUiState,
    studioViewModel: StudioViewModel,
    onBack: () -> Unit,
    onProjectCreated: (String) -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedMode by remember(route.initialMode) { mutableStateOf(route.initialMode) }
    var title by remember(route.seedTitle) { mutableStateOf(route.seedTitle) }
    var description by remember(route.seedPrompt) { mutableStateOf(route.seedPrompt) }
    var genre by remember(route.seedGenre) { mutableStateOf(route.seedGenre) }
    var customGenre by remember { mutableStateOf("") }
    var countryLocation by remember(route.seedCountry) { mutableStateOf(route.seedCountry) }
    var language by remember { mutableStateOf(uiState.profile?.defaultMovieLanguage?.ifBlank { "English" } ?: "English") }
    var customLanguageMode by remember { mutableStateOf(false) }

    var selectedDurationSec by remember(route.seedDurationSec) { mutableIntStateOf(route.seedDurationSec) }
    var customDurationText by remember { mutableStateOf("") }
    var customDurationUnit by remember { mutableStateOf("sec") }

    var aspectRatio by remember { mutableStateOf(uiState.profile?.defaultAspectRatio ?: "16:9") }
    var resolution by remember { mutableStateOf(uiState.profile?.defaultResolution ?: "1080p") }
    var cameraAngle by remember { mutableStateOf(SparkCatalog.cameraAngles.first()) }
    var lighting by remember { mutableStateOf(SparkCatalog.lightingStyles.first()) }
    var atmosphere by remember { mutableStateOf("High-Stakes Suspense") }
    var backgroundSetting by remember { mutableStateOf("Lagos Victoria Island & Mainland Bridges") }
    var visualStyle by remember(route.seedVisualStyle) { mutableStateOf(route.seedVisualStyle) }
    var voiceStyle by remember { mutableStateOf(uiState.profile?.defaultVoice ?: "Nigerian English — Warm Female") }
    var musicStyle by remember { mutableStateOf("Afrobeats Orchestral Hybrid") }

    var characterSummary by remember { mutableStateOf("") }
    var characterAppearance by remember { mutableStateOf("") }
    var characterClothing by remember { mutableStateOf("") }
    var characterAgeRange by remember { mutableStateOf("25-35") }
    var publicProject by remember { mutableStateOf(false) }

    var referenceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                referenceBitmap = MediaUtils.uriToBitmapAsync(context, uri, 720)
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = tempCameraUri
        if (ok && uri != null) {
            scope.launch {
                referenceBitmap = MediaUtils.uriToBitmapAsync(context, uri, 720)
            }
        }
    }

    val cameraPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val uri = MediaUtils.createTempCameraUri(context)
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            studioViewModel.setErrorBanner("Camera permission denied. Use Photo Picker to select a reference image.")
        }
    }

    val effectiveDuration = remember(selectedDurationSec, customDurationText, customDurationUnit) {
        val customVal = customDurationText.toIntOrNull()
        if (customVal != null && customVal > 0) {
            if (customDurationUnit == "min") customVal * 60 else customVal
        } else {
            selectedDurationSec
        }
    }
    val effectiveGenre = if (genre == "Custom" && customGenre.isNotBlank()) customGenre.trim() else genre
    val estCredits = SparkCatalog.estimateCredits(effectiveDuration, resolution)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SparkBrushes.CosmicBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SparkTopBar(
                title = "Create Movie Studio",
                subtitle = "Gemini Screenplay & Video Pipeline",
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
                val modes = listOf(
                    "text_to_movie" to "Text to Movie",
                    "image_to_movie" to "Image to Movie",
                    "script_to_movie" to "Script to Movie",
                    "templates" to "Templates"
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(modes) { (key, label) ->
                        FilterChip(
                            selected = selectedMode == key,
                            onClick = { selectedMode = key },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ChampagneGold,
                                selectedLabelColor = MidnightBlack
                            ),
                            modifier = Modifier.testTag("mode_chip_$key")
                        )
                    }
                }
            }

            if (selectedMode == "templates") {
                item {
                    Text("Quick Starter Templates", style = MaterialTheme.typography.titleMedium, color = SoftGold)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(SparkCatalog.starterTemplates, key = { it.id }) { tpl ->
                            SparkCard(
                                modifier = Modifier.width(260.dp),
                                onClick = {
                                    title = tpl.title
                                    description = tpl.prompt
                                    genre = tpl.genre
                                    countryLocation = tpl.country
                                    visualStyle = tpl.visualStyle
                                    selectedDurationSec = tpl.durationSec
                                    selectedMode = "text_to_movie"
                                }
                            ) {
                                SparkBadge(label = tpl.genre)
                                Spacer(Modifier.height(6.dp))
                                Text(tpl.title, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${tpl.country} • ${SparkCatalog.formatDuration(tpl.durationSec)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SoftGold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(tpl.prompt, style = MaterialTheme.typography.bodySmall, color = SecondaryIce, maxLines = 3)
                            }
                        }
                    }
                }
            }

            item {
                SparkCard {
                    Text("1. Story & Prompt", style = MaterialTheme.typography.titleMedium, color = ChampagneGold)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Movie Title") },
                        placeholder = { Text("e.g., Secret Syndicate of Lagos") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("movie_title_input")
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = {
                            Text(
                                if (selectedMode == "script_to_movie") "Paste Full Script / Scenes"
                                else "Movie Idea or Scene Description"
                            )
                        },
                        placeholder = {
                            Text("Create a professional Nigerian action movie about a young man who discovers a secret organisation in Lagos.")
                        },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth().testTag("movie_description_input")
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = countryLocation,
                        onValueChange = { countryLocation = it },
                        label = { Text("Country / Location") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("Movie Language (Dialogue, Narration & Subtitles)", style = MaterialTheme.typography.labelLarge, color = SoftGold)
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(SparkCatalog.movieLanguages + "Custom") { langOpt ->
                            val isSelected = if (langOpt == "Custom") {
                                customLanguageMode || SparkCatalog.movieLanguages.none { it.equals(language, ignoreCase = true) }
                            } else {
                                !customLanguageMode && language.equals(langOpt, ignoreCase = true)
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (langOpt == "Custom") {
                                        customLanguageMode = true
                                        if (SparkCatalog.movieLanguages.any { it.equals(language, ignoreCase = true) }) {
                                            language = ""
                                        }
                                    } else {
                                        customLanguageMode = false
                                        language = langOpt
                                    }
                                },
                                label = { Text(langOpt) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ChampagneGold,
                                    selectedLabelColor = MidnightBlack
                                ),
                                modifier = Modifier.testTag("lang_chip_${langOpt.lowercase().replace(" ", "_")}")
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = language,
                        onValueChange = {
                            language = it
                            customLanguageMode = SparkCatalog.movieLanguages.none { preset ->
                                preset.equals(it.trim(), ignoreCase = true)
                            }
                        },
                        label = { Text("Movie Language (Select above or type any language)") },
                        placeholder = { Text("e.g., Nigerian Pidgin, Yoruba, French, Korean...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("movie_language_input")
                    )
                }
            }

            item {
                SparkCard {
                    Text("2. Reference Photo / AI Poster Image", style = MaterialTheme.typography.titleMedium, color = ChampagneGold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Pick a reference photo, take a picture, or generate an AI poster/scene image with Gemini.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryIce
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        SparkOutlinedButton(
                            text = "Photo Picker",
                            icon = Icons.Default.PhotoLibrary,
                            onClick = {
                                galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            modifier = Modifier.weight(1f).testTag("pick_reference_photo_btn")
                        )
                        SparkOutlinedButton(
                            text = "Camera",
                            icon = Icons.Default.CameraAlt,
                            onClick = {
                                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                                if (granted) {
                                    val uri = MediaUtils.createTempCameraUri(context)
                                    tempCameraUri = uri
                                    cameraLauncher.launch(uri)
                                } else {
                                    cameraPermLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("capture_reference_photo_btn")
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    SparkOutlinedButton(
                        text = "Generate AI Poster / Concept Image",
                        icon = Icons.Default.AutoAwesome,
                        onClick = {
                            studioViewModel.generateMovieConceptImage(
                                title = title,
                                description = description,
                                genre = effectiveGenre,
                                countryLocation = countryLocation,
                                visualStyle = visualStyle,
                                referenceBitmap = referenceBitmap,
                                onGenerated = { generated -> referenceBitmap = generated }
                            )
                        },
                        modifier = Modifier.fillMaxWidth().testTag("generate_ai_poster_btn")
                    )
                    if (referenceBitmap != null) {
                        Spacer(Modifier.height(10.dp))
                        Image(
                            bitmap = referenceBitmap!!.asImageBitmap(),
                            contentDescription = "Selected reference photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                    }
                }
            }

            item {
                SparkCard {
                    Text("3. Genre & Visual Style", style = MaterialTheme.typography.titleMedium, color = ChampagneGold)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        (SparkCatalog.genres + "Custom").forEach { item ->
                            FilterChip(
                                selected = genre == item,
                                onClick = { genre = item },
                                label = { Text(item) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ChampagneGold,
                                    selectedLabelColor = MidnightBlack
                                )
                            )
                        }
                    }
                    if (genre == "Custom") {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customGenre,
                            onValueChange = { customGenre = it },
                            label = { Text("Enter Custom Genre") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Visual Style", style = MaterialTheme.typography.labelLarge, color = SoftGold)
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(SparkCatalog.visualStyles) { st ->
                            FilterChip(
                                selected = visualStyle == st,
                                onClick = { visualStyle = st },
                                label = { Text(st) },
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
                SparkCard {
                    Text("4. Duration, Resolution & Aspect Ratio", style = MaterialTheme.typography.titleMedium, color = ChampagneGold)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SparkCatalog.durationPresets.forEach { preset ->
                            FilterChip(
                                selected = customDurationText.isBlank() && selectedDurationSec == preset.seconds,
                                onClick = {
                                    selectedDurationSec = preset.seconds
                                    customDurationText = ""
                                },
                                label = { Text(preset.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ChampagneGold,
                                    selectedLabelColor = MidnightBlack
                                )
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = customDurationText,
                            onValueChange = { customDurationText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Custom Duration") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = customDurationUnit == "sec",
                            onClick = { customDurationUnit = "sec" },
                            label = { Text("Sec") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ChampagneGold, selectedLabelColor = MidnightBlack)
                        )
                        FilterChip(
                            selected = customDurationUnit == "min",
                            onClick = { customDurationUnit = "min" },
                            label = { Text("Min") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ChampagneGold, selectedLabelColor = MidnightBlack)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SparkCatalog.resolutions.forEach { resOpt ->
                            FilterChip(
                                selected = resolution == resOpt,
                                onClick = { resolution = resOpt },
                                label = { Text(resOpt) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ChampagneGold, selectedLabelColor = MidnightBlack)
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(SparkCatalog.aspectRatios) { ar ->
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
                SparkCard {
                    Text("5. Direction, Camera & Character Continuity", style = MaterialTheme.typography.titleMedium, color = ChampagneGold)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(SparkCatalog.cameraAngles) { cam ->
                            FilterChip(
                                selected = cameraAngle == cam,
                                onClick = { cameraAngle = cam },
                                label = { Text(cam) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ChampagneGold, selectedLabelColor = MidnightBlack)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(SparkCatalog.lightingStyles) { lit ->
                            FilterChip(
                                selected = lighting == lit,
                                onClick = { lighting = lit },
                                label = { Text(lit) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ChampagneGold, selectedLabelColor = MidnightBlack)
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = characterSummary,
                        onValueChange = { characterSummary = it },
                        label = { Text("Lead Character Name & Bio") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = characterAppearance,
                            onValueChange = { characterAppearance = it },
                            label = { Text("Appearance") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = characterClothing,
                            onValueChange = { characterClothing = it },
                            label = { Text("Clothing") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { publicProject = !publicProject }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Allow Public Discovery", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = publicProject,
                            onCheckedChange = { publicProject = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = MidnightBlack, checkedTrackColor = ChampagneGold)
                        )
                    }
                }
            }

            item {
                SparkCard(borderColor = ChampagneGold) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Estimated Render Cost: $estCredits credits", style = MaterialTheme.typography.titleMedium, color = ChampagneGold)
                            Text(
                                "Duration: ${SparkCatalog.formatDuration(effectiveDuration)} • 0 credits deducted if provider is paused/unconfigured",
                                style = MaterialTheme.typography.bodySmall,
                                color = SecondaryIce
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    SparkGoldButton(
                        text = "Generate Screenplay & Movie",
                        icon = Icons.Default.MovieCreation,
                        onClick = {
                            studioViewModel.generateAndCreateMovieProject(
                                title = title,
                                description = description,
                                genre = effectiveGenre,
                                countryLocation = countryLocation,
                                language = language,
                                mainCharacterSummary = characterSummary,
                                characterAppearance = characterAppearance,
                                characterClothing = characterClothing,
                                characterAgeRange = characterAgeRange,
                                cameraAngle = cameraAngle,
                                lighting = lighting,
                                atmosphere = atmosphere,
                                backgroundSetting = backgroundSetting,
                                visualStyle = visualStyle,
                                voiceStyle = voiceStyle,
                                musicStyle = musicStyle,
                                aspectRatio = aspectRatio,
                                resolution = resolution,
                                targetDurationSec = effectiveDuration,
                                publicProject = publicProject,
                                jobType = if (selectedMode == "templates") "text_to_movie" else selectedMode,
                                referenceBitmap = referenceBitmap,
                                onProjectCreated = onProjectCreated
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("generate_movie_submit_btn")
                    )
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
