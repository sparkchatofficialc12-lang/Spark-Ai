package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.VoiceAndAudioEngine
import com.example.data.model.CommunityCatalog
import com.example.data.model.CreatorProfile
import com.example.data.model.MusicLibraryCatalog
import com.example.data.model.MusicTrack
import com.example.data.model.SocialPost
import com.example.ui.components.CreatorAvatarCircle
import com.example.ui.components.SparkBadge
import com.example.ui.components.SparkCard
import com.example.ui.components.SparkGoldButton
import com.example.ui.components.SparkOutlinedButton
import com.example.ui.components.SparkTopBar
import com.example.ui.navigation.SparkRoute
import com.example.ui.theme.AmberGold
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.IceWhite
import com.example.ui.theme.MidnightBlack
import com.example.ui.theme.SecondaryIce
import com.example.ui.theme.SoftGold
import com.example.ui.theme.SparkBrushes
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.SocialUiState
import com.example.ui.viewmodel.StudioUiState

@Composable
fun DiscoverScreen(
    currentUid: String,
    myUsername: String,
    socialState: SocialUiState,
    onToggleFollow: (String) -> Unit,
    onStartChat: (CreatorProfile) -> Unit,
    onNavigate: (SparkRoute) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val matchingCreators = remember(socialState.allCreatorProfiles, socialState.blockedCreatorIds, query) {
        socialState.allCreatorProfiles
            .filter { it.creatorId != currentUid && !socialState.blockedCreatorIds.contains(it.creatorId) }
            .filter {
                query.isBlank() ||
                    it.username.contains(query, true) ||
                    it.displayName.contains(query, true) ||
                    it.bio.contains(query, true)
            }
            .take(15)
    }

    val matchingPosts = remember(socialState.publicPosts, socialState.blockedCreatorIds, query, selectedCategory) {
        socialState.publicPosts
            .filterNot { socialState.blockedCreatorIds.contains(it.creatorId) }
            .filter { selectedCategory == "All" || it.category.equals(selectedCategory, true) }
            .filter {
                query.isBlank() ||
                    it.title.contains(query, true) ||
                    it.caption.contains(query, true) ||
                    it.hashtags.contains(query, true) ||
                    it.creatorUsername.contains(query, true)
            }
            .sortedByDescending { it.viewsCount + it.likesCount * 2 }
    }

    val matchingTemplates = remember(socialState.publicTemplates, query, selectedCategory) {
        socialState.publicTemplates
            .filter { selectedCategory == "All" || it.category.equals(selectedCategory, true) }
            .filter {
                query.isBlank() ||
                    it.title.contains(query, true) ||
                    it.tags.contains(query, true)
            }
            .take(10)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SparkBrushes.CosmicBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Discover Spark Social", style = MaterialTheme.typography.headlineSmall, color = ChampagneGold)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                label = { Text("Search creators, videos, movies, #hashtags, templates") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("discover_search_input")
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                SparkOutlinedButton(
                    text = "Sounds",
                    icon = Icons.Default.MusicNote,
                    onClick = { onNavigate(SparkRoute.SoundsDiscovery) },
                    modifier = Modifier.weight(1f)
                )
                SparkOutlinedButton(
                    text = "Templates",
                    icon = Icons.Default.Style,
                    onClick = { onNavigate(SparkRoute.CommunityHub) },
                    modifier = Modifier.weight(1f)
                )
                SparkOutlinedButton(
                    text = "Live",
                    icon = Icons.Default.LiveTv,
                    onClick = { onNavigate(SparkRoute.LiveStreaming) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (matchingCreators.isNotEmpty()) {
            item {
                Text("Creators to Follow & Connect", style = MaterialTheme.typography.titleMedium, color = SoftGold)
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(matchingCreators, key = { it.creatorId }) { creator ->
                        val isFollowing = socialState.followingCreatorIds.contains(creator.creatorId)
                        SparkCard(
                            modifier = Modifier.width(210.dp),
                            onClick = { onNavigate(SparkRoute.CreatorProfileRoute(creator.creatorId)) }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CreatorAvatarCircle(creator.username)
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("@${creator.username}", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "${creator.followersCount} followers",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SoftGold
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                SparkOutlinedButton(
                                    text = if (isFollowing) "Following" else "Follow",
                                    onClick = { onToggleFollow(creator.creatorId) },
                                    modifier = Modifier.weight(1f)
                                )
                                SparkGoldButton(
                                    text = "Chat",
                                    onClick = { onStartChat(creator) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (matchingTemplates.isNotEmpty()) {
            item {
                Text("Trending AI Templates", style = MaterialTheme.typography.titleMedium, color = SoftGold)
            }
            items(matchingTemplates, key = { it.id }) { tpl ->
                SparkCard(onClick = { onNavigate(SparkRoute.TemplateDetail(tpl.id)) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tpl.title, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${tpl.category} • @${tpl.creatorUsername} • ${tpl.remixesCount} remixes",
                                style = MaterialTheme.typography.bodySmall,
                                color = SecondaryIce
                            )
                        }
                        SparkBadge(label = "TEMPLATE")
                    }
                }
            }
        }

        item {
            Text("Trending Videos, Movies & Posts (${matchingPosts.size})", style = MaterialTheme.typography.titleMedium, color = SoftGold)
        }

        if (matchingPosts.isEmpty()) {
            item {
                SparkCard {
                    Text("No matching posts found", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Try another keyword or clear filters to explore all Spark Social creations.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryIce
                    )
                }
            }
        } else {
            items(matchingPosts, key = { it.id }) { post ->
                DiscoverPostRow(
                    post = post,
                    onClick = { onNavigate(SparkRoute.PostDetail(post.id)) }
                )
            }
        }

        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun DiscoverPostRow(post: SocialPost, onClick: () -> Unit) {
    SparkCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(post.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    "@${post.creatorUsername} • ${post.creationType.replace('_', ' ')} • ${post.viewsCount} views • ${post.likesCount} likes",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryIce
                )
                if (post.hashtags.isNotBlank()) {
                    Text(post.hashtags, style = MaterialTheme.typography.labelSmall, color = ChampagneGold)
                }
            }
            SparkBadge(label = post.category)
        }
    }
}

@Composable
fun SoundsDiscoveryScreen(
    allTracks: List<MusicTrack>,
    savedSoundIds: Set<String>,
    socialState: SocialUiState,
    voiceAudio: VoiceAndAudioEngine,
    onToggleSaveSound: (MusicTrack) -> Unit,
    onBack: () -> Unit,
    onNavigate: (SparkRoute) -> Unit
) {
    BackHandler { onBack() }
    val isPlaying by voiceAudio.isAudioPlaying.collectAsStateWithLifecycle()
    var playingTrackId by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            SparkTopBar(
                title = "Sounds & Studio Audio",
                subtitle = "Royalty-free & uploaded soundtracks",
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
            items(allTracks, key = { it.id }) { track ->
                val saved = savedSoundIds.contains(track.id)
                val usageCount = socialState.publicPosts.count { it.soundId == track.id }

                SparkCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(track.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${track.category} • ${track.bpm} BPM • $usageCount videos",
                                style = MaterialTheme.typography.bodySmall,
                                color = SoftGold
                            )
                            Text(track.licenseType, style = MaterialTheme.typography.labelSmall, color = SecondaryIce)
                        }
                        IconButton(onClick = { onToggleSaveSound(track) }) {
                            Icon(
                                imageVector = if (saved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Save sound",
                                tint = if (saved) ChampagneGold else IceWhite
                            )
                        }
                        if (track.localUri.isNotBlank()) {
                            IconButton(onClick = {
                                if (isPlaying && playingTrackId == track.id) {
                                    voiceAudio.stopAudio()
                                    playingTrackId = ""
                                } else {
                                    playingTrackId = track.id
                                    voiceAudio.playLocalAudio(android.net.Uri.parse(track.localUri), 1f)
                                }
                            }) {
                                Icon(
                                    imageVector = if (isPlaying && playingTrackId == track.id) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = "Preview track",
                                    tint = ChampagneGold
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        SparkGoldButton(
                            text = "Use in Post",
                            onClick = {
                                onNavigate(
                                    SparkRoute.UniversalPost(
                                        prefillSoundId = track.id,
                                        prefillSoundTitle = track.title
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                        SparkOutlinedButton(
                            text = "Music Video",
                            onClick = { onNavigate(SparkRoute.MusicVideoStudio(track.id)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LiveStreamingScreen(
    studioState: StudioUiState,
    socialState: SocialUiState,
    isFounderAdmin: Boolean,
    onBack: () -> Unit,
    onNavigate: (SparkRoute) -> Unit
) {
    BackHandler { onBack() }
    val setting = studioState.adminSetting
    val configured = setting?.liveStreamingConfigured == true && setting.liveStreamingBaseUrl.isNotBlank()

    Scaffold(
        topBar = {
            SparkTopBar(
                title = "Spark Live Streaming",
                subtitle = if (configured) setting?.liveStreamingProviderName else "Provider Not Configured",
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
            if (!configured) {
                item {
                    SparkCard(borderColor = AmberGold) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LiveTv, contentDescription = null, tint = AmberGold, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Live Streaming Backend Not Configured", style = MaterialTheme.typography.titleMedium, color = AmberGold)
                                Text("Requires Mux, LiveKit, or Agora server token proxy", style = MaterialTheme.typography.bodySmall, color = SecondaryIce)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Spark AI never displays fake or simulated live broadcasts. Live streaming activates automatically once the Founder Admin configures a real streaming ingest/playback backend URL.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryIce
                        )
                        if (isFounderAdmin) {
                            Spacer(Modifier.height(12.dp))
                            SparkGoldButton(
                                text = "Configure Live Streaming in Admin",
                                onClick = { onNavigate(SparkRoute.AdminApiConfig) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            } else if (socialState.liveStreams.isEmpty()) {
                item {
                    SparkCard {
                        Text("No Active Broadcasts Right Now", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Streaming provider (${setting?.liveStreamingProviderName}) is connected. Active creator streams will appear here in real time.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryIce
                        )
                    }
                }
            } else {
                items(socialState.liveStreams, key = { it.id }) { stream ->
                    SparkCard(borderColor = SuccessGreen) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stream.title, style = MaterialTheme.typography.titleMedium)
                                Text("@${stream.hostUsername} • ${stream.viewerCount} watching", style = MaterialTheme.typography.bodySmall, color = SoftGold)
                            }
                            SparkBadge(label = stream.status.uppercase(), color = SuccessGreen)
                        }
                        if (stream.playbackUrl.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            SparkGoldButton(
                                text = "Watch Live",
                                onClick = {
                                    onNavigate(
                                        SparkRoute.VideoPlayer(
                                            title = stream.title,
                                            videoUrl = stream.playbackUrl
                                        )
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}
