package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.MediaUtils
import com.example.data.model.CommunityCatalog
import com.example.data.model.CommunityTemplate
import com.example.data.model.RemixWorkspaceSeed
import com.example.data.model.SparkCatalog
import com.example.ui.components.Base64Thumbnail
import com.example.ui.components.CreatorAvatarCircle
import com.example.ui.components.SparkBadge
import com.example.ui.components.SparkBusyOverlay
import com.example.ui.components.SparkCard
import com.example.ui.components.SparkGoldButton
import com.example.ui.components.SparkOutlinedButton
import com.example.ui.components.SparkTopBar
import com.example.ui.navigation.SparkRoute
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.MidnightBlack
import com.example.ui.theme.SecondaryIce
import com.example.ui.theme.SoftGold
import com.example.ui.theme.SparkBrushes
import com.example.ui.viewmodel.SocialUiState
import com.example.ui.viewmodel.StudioUiState

@Composable
fun CommunityHubScreen(
    socialState: SocialUiState,
    onToggleLike: (CommunityTemplate) -> Unit,
    onToggleSave: (String) -> Unit,
    onUseTemplate: (CommunityTemplate) -> Unit,
    onBack: () -> Unit,
    onNavigate: (SparkRoute) -> Unit
) {
    BackHandler { onBack() }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var sortBy by remember { mutableStateOf("Trending") }

    val filtered = remember(
        socialState.publicTemplates,
        socialState.blockedCreatorIds,
        searchQuery,
        selectedCategory,
        sortBy
    ) {
        socialState.publicTemplates
            .filterNot { socialState.blockedCreatorIds.contains(it.creatorId) }
            .filter { selectedCategory == "All" || it.category.equals(selectedCategory, true) }
            .filter {
                searchQuery.isBlank() ||
                    it.title.contains(searchQuery, true) ||
                    it.tags.contains(searchQuery, true) ||
                    it.creatorUsername.contains(searchQuery, true)
            }
            .let { list ->
                when (sortBy) {
                    "Most Remixed" -> list.sortedByDescending { it.remixesCount }
                    "Newest" -> list.sortedByDescending { it.createdAt?.seconds ?: 0L }
                    else -> list.sortedByDescending { it.trendingScore }
                }
            }
    }

    Scaffold(
        topBar = {
            SparkTopBar(
                title = "Community Templates",
                subtitle = "${filtered.size} reusable AI film templates",
                onBack = onBack
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    SparkGoldButton(
                        text = "Publish Template",
                        icon = Icons.Default.Style,
                        onClick = { onNavigate(SparkRoute.UniversalPost(prefillType = "template")) },
                        modifier = Modifier.weight(1f)
                    )
                    SparkOutlinedButton(
                        text = "My Templates",
                        onClick = { onNavigate(SparkRoute.MyTemplates) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    label = { Text("Search templates, styles, creators") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(CommunityCatalog.categories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ChampagneGold,
                                selectedLabelColor = MidnightBlack
                            )
                        )
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Trending", "Most Remixed", "Newest").forEach { s ->
                        FilterChip(
                            selected = sortBy == s,
                            onClick = { sortBy = s },
                            label = { Text(s) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ChampagneGold,
                                selectedLabelColor = MidnightBlack
                            )
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    SparkCard {
                        Text("No templates in \"$selectedCategory\" yet", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Publish your own movie scene, singing effect, or visual style as a Community Template!",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryIce
                        )
                    }
                }
            } else {
                items(filtered, key = { it.id }) { tpl ->
                    val liked = socialState.myTemplateLikeIds.contains(tpl.id)
                    val saved = socialState.savedTemplateIds.contains(tpl.id)
                    SparkCard(onClick = { onNavigate(SparkRoute.TemplateDetail(tpl.id)) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Base64Thumbnail(
                                base64Data = tpl.thumbnailData,
                                contentDescription = tpl.title,
                                modifier = Modifier.width(88.dp).height(64.dp).clip(RoundedCornerShape(10.dp))
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(tpl.title, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "@${tpl.creatorUsername} • ${tpl.category}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SoftGold
                                )
                                Text(
                                    "${tpl.remixesCount} remixes • ${tpl.likesCount} likes",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SecondaryIce
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row {
                                IconButton(onClick = { onToggleLike(tpl) }) {
                                    Icon(
                                        imageVector = if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "Like template",
                                        tint = if (liked) ChampagneGold else SecondaryIce
                                    )
                                }
                                IconButton(onClick = { onToggleSave(tpl.id) }) {
                                    Icon(
                                        imageVector = if (saved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        contentDescription = "Save template",
                                        tint = if (saved) ChampagneGold else SecondaryIce
                                    )
                                }
                            }
                            if (tpl.allowReuse) {
                                SparkGoldButton(
                                    text = "USE TEMPLATE",
                                    onClick = { onUseTemplate(tpl) }
                                )
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(92.dp)) }
        }
    }
}

@Composable
fun TemplateDetailScreen(
    templateId: String,
    socialState: SocialUiState,
    onOpenTemplate: (CommunityTemplate) -> Unit,
    onToggleLike: (CommunityTemplate) -> Unit,
    onToggleSave: (String) -> Unit,
    onAddComment: (String, String) -> Unit,
    onUseTemplate: (CommunityTemplate) -> Unit,
    onBack: () -> Unit,
    onNavigate: (SparkRoute) -> Unit
) {
    BackHandler { onBack() }

    val tpl = socialState.selectedTemplate?.takeIf { it.id == templateId }
        ?: (socialState.publicTemplates + socialState.myTemplates).firstOrNull { it.id == templateId }

    LaunchedEffect(tpl?.id) {
        if (tpl != null && socialState.selectedTemplate?.id != tpl.id) {
            onOpenTemplate(tpl)
        }
    }

    var commentInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            SparkTopBar(
                title = tpl?.title ?: "Template Details",
                subtitle = tpl?.category,
                onBack = onBack
            )
        }
    ) { innerPadding ->
        if (tpl == null) {
            Box(
                modifier = Modifier.fillMaxSize().background(SparkBrushes.CosmicBackground).padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading template...", color = SecondaryIce)
            }
        } else {
            val liked = socialState.myTemplateLikeIds.contains(tpl.id)
            val saved = socialState.savedTemplateIds.contains(tpl.id)
            LazyColumn(
                modifier = Modifier.fillMaxSize().background(SparkBrushes.CosmicBackground).padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    SparkCard {
                        if (tpl.thumbnailData.isNotBlank()) {
                            Base64Thumbnail(
                                base64Data = tpl.thumbnailData,
                                contentDescription = tpl.title,
                                modifier = Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(14.dp))
                            )
                            Spacer(Modifier.height(10.dp))
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onNavigate(SparkRoute.CreatorProfileRoute(tpl.creatorId)) }
                        ) {
                            CreatorAvatarCircle(tpl.creatorUsername)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("@${tpl.creatorUsername}", style = MaterialTheme.typography.titleSmall, color = ChampagneGold)
                                Text(tpl.category, style = MaterialTheme.typography.labelSmall, color = SecondaryIce)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(tpl.title, style = MaterialTheme.typography.titleLarge)
                        Text(tpl.description, style = MaterialTheme.typography.bodySmall, color = SecondaryIce)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${tpl.remixesCount} remixes • ${tpl.likesCount} likes • ${tpl.aspectRatio} • ${SparkCatalog.formatDuration(tpl.durationSec)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SoftGold
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            SparkOutlinedButton(
                                text = if (liked) "Liked" else "Like",
                                icon = if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                onClick = { onToggleLike(tpl) },
                                modifier = Modifier.weight(1f)
                            )
                            SparkOutlinedButton(
                                text = if (saved) "Saved" else "Save",
                                icon = if (saved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                onClick = { onToggleSave(tpl.id) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (tpl.allowReuse) {
                            Spacer(Modifier.height(8.dp))
                            SparkGoldButton(
                                text = "USE TEMPLATE IN REMIX STUDIO",
                                onClick = { onUseTemplate(tpl) },
                                modifier = Modifier.fillMaxWidth().testTag("template_detail_use_btn")
                            )
                        }
                    }
                }

                item {
                    Text("Remixes (${socialState.selectedTemplateRemixes.size})", style = MaterialTheme.typography.titleMedium, color = SoftGold)
                }
                items(socialState.selectedTemplateRemixes, key = { it.id }) { remix ->
                    SparkCard {
                        Text(remix.newMovieTitle, style = MaterialTheme.typography.titleSmall)
                        Text("Remixed by @${remix.remixerUsername}", style = MaterialTheme.typography.bodySmall, color = ChampagneGold)
                    }
                }

                item {
                    Text("Comments (${socialState.selectedTemplateComments.size})", style = MaterialTheme.typography.titleMedium, color = SoftGold)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = commentInput,
                            onValueChange = { commentInput = it },
                            label = { Text("Comment on template...") },
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            if (commentInput.isNotBlank()) {
                                onAddComment(tpl.id, commentInput)
                                commentInput = ""
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send comment", tint = ChampagneGold)
                        }
                    }
                }

                items(socialState.selectedTemplateComments, key = { it.id }) { c ->
                    SparkCard {
                        Text("@${c.username}", style = MaterialTheme.typography.labelLarge, color = ChampagneGold)
                        Text(c.text, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun RemixWorkspaceScreen(
    seed: RemixWorkspaceSeed?,
    studioState: StudioUiState,
    onCreateRemixProject: (
        seed: RemixWorkspaceSeed,
        newTitle: String,
        customPrompt: String,
        aspectRatio: String,
        durationSec: Int,
        photoBitmap: Bitmap?
    ) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var newTitle by remember(seed) { mutableStateOf(seed?.let { "${it.template.title} Remix" } ?: "Spark AI Remix") }
    var promptText by remember(seed) { mutableStateOf(seed?.prompt?.basePrompt.orEmpty()) }
    var aspectRatio by remember(seed) { mutableStateOf(seed?.settings?.aspectRatio ?: "9:16") }
    var durationSec by remember(seed) { mutableIntStateOf(seed?.settings?.durationSec ?: 30) }
    var referencePhoto by remember { mutableStateOf<Bitmap?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                referencePhoto = MediaUtils.uriToBitmapAsync(context, uri, 960)
            }
        }
    }

    Scaffold(
        topBar = {
            SparkTopBar(
                title = "Template Remix Studio",
                subtitle = seed?.let { "Inspired by @${it.template.creatorUsername}" },
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
            if (seed == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No template selected.", color = SecondaryIce)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        SparkCard(borderColor = ChampagneGold.copy(alpha = 0.6f)) {
                            SparkBadge(label = "ORIGINAL CREATOR ATTRIBUTION")
                            Spacer(Modifier.height(6.dp))
                            Text(seed.template.title, style = MaterialTheme.typography.titleMedium)
                            Text("Original Creator: @${seed.template.creatorUsername}", style = MaterialTheme.typography.bodySmall, color = SoftGold)
                        }
                    }

                    item {
                        SparkCard {
                            OutlinedTextField(
                                value = newTitle,
                                onValueChange = { newTitle = it },
                                label = { Text("Your Remix Movie Title") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = promptText,
                                onValueChange = { promptText = it },
                                label = { Text("Customize Prompt & Scene") },
                                minLines = 4,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(10.dp))
                            SparkOutlinedButton(
                                text = if (referencePhoto == null) "Add Your Photo / Character" else "Change Reference Photo",
                                icon = Icons.Default.AddPhotoAlternate,
                                onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(10.dp))
                            Text("Aspect Ratio", style = MaterialTheme.typography.labelLarge, color = SoftGold)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(SparkCatalog.aspectRatios) { ratio ->
                                    FilterChip(
                                        selected = aspectRatio == ratio,
                                        onClick = { aspectRatio = ratio },
                                        label = { Text(ratio) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = ChampagneGold,
                                            selectedLabelColor = MidnightBlack
                                        )
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("Duration", style = MaterialTheme.typography.labelLarge, color = SoftGold)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(SparkCatalog.durationPresets.take(6)) { preset ->
                                    FilterChip(
                                        selected = durationSec == preset.seconds,
                                        onClick = { durationSec = preset.seconds },
                                        label = { Text(preset.label) },
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
                            text = "Generate Remix & Attribute Creator",
                            icon = Icons.Default.MovieCreation,
                            onClick = {
                                onCreateRemixProject(seed, newTitle, promptText, aspectRatio, durationSec, referencePhoto)
                            },
                            modifier = Modifier.fillMaxWidth().testTag("remix_generate_btn")
                        )
                        Spacer(Modifier.height(72.dp))
                    }
                }
            }

            if (studioState.isBusy) {
                SparkBusyOverlay(studioState.busyLabel)
            }
        }
    }
}
