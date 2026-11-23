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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.example.data.model.CommunityTemplate
import com.example.data.model.MovieItem
import com.example.data.model.SocialPost
import com.example.ui.components.Base64Thumbnail
import com.example.ui.components.CreatorAvatarCircle
import com.example.ui.components.SparkBadge
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
import com.example.ui.viewmodel.SocialUiState
import com.example.ui.viewmodel.StudioUiState

@Composable
fun CreatorProfileScreen(
    targetCreatorId: String,
    currentUid: String,
    studioState: StudioUiState,
    socialState: SocialUiState,
    onOpenCreatorProfile: (String) -> Unit,
    onToggleFollow: (String) -> Unit,
    onStartChat: (String, String) -> Unit,
    onUpdateBioAndCover: (String, Bitmap?) -> Unit,
    onBack: (() -> Unit)? = null,
    onNavigate: (SparkRoute) -> Unit
) {
    if (onBack != null) BackHandler { onBack() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val effectiveId = targetCreatorId.ifBlank { currentUid }
    val isOwnProfile = effectiveId == currentUid

    LaunchedEffect(effectiveId) {
        if (effectiveId.isNotBlank()) onOpenCreatorProfile(effectiveId)
    }

    val creator = socialState.activeCreatorProfile?.takeIf { it.creatorId == effectiveId }
        ?: socialState.allCreatorProfiles.firstOrNull { it.creatorId == effectiveId }

    val displayName = if (isOwnProfile) {
        studioState.account?.fullName?.ifBlank { creator?.displayName ?: "Spark Creator" } ?: "Spark Creator"
    } else {
        creator?.displayName?.ifBlank { creator.username } ?: "Spark Creator"
    }
    val username = if (isOwnProfile) {
        studioState.account?.username?.ifBlank { creator?.username ?: "creator" } ?: "creator"
    } else {
        creator?.username?.ifBlank { "creator" } ?: "creator"
    }
    val bio = if (isOwnProfile) {
        studioState.profile?.bio?.ifBlank { creator?.bio.orEmpty() } ?: "Spark AI Filmmaker"
    } else {
        creator?.bio?.ifBlank { "Spark Social Creator" } ?: "Spark Social Creator"
    }

    val postsForProfile = remember(isOwnProfile, socialState.myPosts, socialState.activeCreatorPosts) {
        if (isOwnProfile) socialState.myPosts else socialState.activeCreatorPosts
    }
    val savedPosts = remember(socialState.publicPosts, socialState.myPosts, socialState.savedPostIds) {
        (socialState.publicPosts + socialState.myPosts)
            .distinctBy { it.id }
            .filter { socialState.savedPostIds.contains(it.id) }
    }

    val tabs = if (isOwnProfile) {
        listOf("Posts", "Videos", "Movies", "Music Videos", "Templates", "Saved", "Drafts")
    } else {
        listOf("Posts", "Videos", "Movies", "Music Videos")
    }
    var selectedTab by remember { mutableStateOf("Posts") }
    var showEditBioDialog by remember { mutableStateOf(false) }
    var editBioText by remember(bio) { mutableStateOf(bio) }
    var pickedCoverBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val bmp = MediaUtils.uriToBitmapAsync(context, uri, 960)
                if (bmp != null) {
                    pickedCoverBitmap = bmp
                    onUpdateBioAndCover(bio, bmp)
                }
            }
        }
    }

    val isFollowing = socialState.followingCreatorIds.contains(effectiveId)

    Scaffold(
        topBar = {
            if (onBack != null) {
                SparkTopBar(title = "@$username", subtitle = "Creator Profile", onBack = onBack)
            }
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
                SparkCard(borderColor = ChampagneGold.copy(alpha = 0.65f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        val coverB64 by produceState(
                            initialValue = creator?.coverImageData.orEmpty(),
                            key1 = pickedCoverBitmap,
                            key2 = creator?.coverImageData
                        ) {
                            value = pickedCoverBitmap?.let { MediaUtils.bitmapToCompactBase64Async(it, 150_000) }
                                ?: creator?.coverImageData.orEmpty()
                        }
                        Base64Thumbnail(
                            base64Data = coverB64,
                            contentDescription = "Profile cover",
                            modifier = Modifier.fillMaxSize(),
                            placeholderIcon = Icons.Default.Image
                        )
                        if (isOwnProfile) {
                            SparkOutlinedButton(
                                text = "Cover",
                                icon = Icons.Default.Edit,
                                onClick = { coverPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CreatorAvatarCircle(username)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(displayName, style = MaterialTheme.typography.titleLarge)
                            Text("@$username", style = MaterialTheme.typography.bodySmall, color = ChampagneGold)
                        }
                        if (isOwnProfile) {
                            IconButton(onClick = { onNavigate(SparkRoute.SettingsHub) }) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = ChampagneGold)
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(bio, style = MaterialTheme.typography.bodySmall, color = SecondaryIce)

                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatCounterColumn(
                            label = "Posts",
                            value = postsForProfile.size.toString()
                        )
                        StatCounterColumn(
                            label = "Followers",
                            value = (creator?.followersCount ?: socialState.activeCreatorFollowers.size).toString(),
                            onClick = { onNavigate(SparkRoute.FollowersList(effectiveId, false)) }
                        )
                        StatCounterColumn(
                            label = "Following",
                            value = (if (isOwnProfile) socialState.followingCreatorIds.size else creator?.followingCount ?: 0).toString(),
                            onClick = { onNavigate(SparkRoute.FollowersList(effectiveId, true)) }
                        )
                        StatCounterColumn(
                            label = "Likes",
                            value = postsForProfile.sumOf { it.likesCount }.toString()
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    if (isOwnProfile) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            SparkOutlinedButton(
                                text = "Edit Bio",
                                icon = Icons.Default.Edit,
                                onClick = {
                                    editBioText = bio
                                    showEditBioDialog = true
                                },
                                modifier = Modifier.weight(1f)
                            )
                            SparkGoldButton(
                                text = "Analytics",
                                icon = Icons.Default.Analytics,
                                onClick = { onNavigate(SparkRoute.CreatorAnalytics) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            SparkOutlinedButton(
                                text = "My Posts",
                                onClick = { onNavigate(SparkRoute.MyPosts) },
                                modifier = Modifier.weight(1f)
                            )
                            SparkOutlinedButton(
                                text = "My Creations",
                                onClick = { onNavigate(SparkRoute.MyCreations) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            SparkOutlinedButton(
                                text = "Movie Library",
                                onClick = { onNavigate(SparkRoute.MovieLibrary) },
                                modifier = Modifier.weight(1f).testTag("profile_movie_library_btn")
                            )
                            SparkOutlinedButton(
                                text = "Recycle Bin (${studioState.recentlyDeleted.size})",
                                icon = Icons.Default.DeleteSweep,
                                onClick = { onNavigate(SparkRoute.RecentlyDeleted) },
                                modifier = Modifier.weight(1f).testTag("profile_recycle_bin_btn")
                            )
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            SparkGoldButton(
                                text = if (isFollowing) "Following" else "Follow",
                                onClick = { onToggleFollow(effectiveId) },
                                modifier = Modifier.weight(1f)
                            )
                            SparkOutlinedButton(
                                text = "Message",
                                icon = Icons.Default.ChatBubbleOutline,
                                onClick = { onStartChat(effectiveId, username) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(tabs) { tab ->
                        FilterChip(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            label = { Text(tab) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ChampagneGold,
                                selectedLabelColor = MidnightBlack
                            )
                        )
                    }
                }
            }

            when (selectedTab) {
                "Templates" -> {
                    items(socialState.myTemplates, key = { it.id }) { tpl ->
                        SparkCard(onClick = { onNavigate(SparkRoute.TemplateDetail(tpl.id)) }) {
                            Text(tpl.title, style = MaterialTheme.typography.titleMedium)
                            Text("${tpl.category} • ${tpl.remixesCount} remixes", style = MaterialTheme.typography.bodySmall, color = SoftGold)
                        }
                    }
                }
                "Saved" -> {
                    items(savedPosts, key = { it.id }) { p ->
                        ProfilePostCompactCard(p) { onNavigate(SparkRoute.PostDetail(p.id)) }
                    }
                }
                "Drafts" -> {
                    val drafts = studioState.movies.filter { it.status == "draft" }
                    items(drafts, key = { it.id }) { d ->
                        SparkCard(onClick = { onNavigate(SparkRoute.MovieEditor(d.projectId)) }) {
                            Text(d.title, style = MaterialTheme.typography.titleMedium)
                            Text("${d.genre} • ${d.status}", style = MaterialTheme.typography.bodySmall, color = SecondaryIce)
                        }
                    }
                }
                else -> {
                    val filtered = postsForProfile.filter {
                        when (selectedTab) {
                            "Videos" -> it.mediaType == "video"
                            "Movies" -> it.creationType in listOf("movie", "long_video")
                            "Music Videos" -> it.creationType == "music_video"
                            else -> true
                        }
                    }
                    if (filtered.isEmpty()) {
                        item {
                            SparkCard {
                                Text("No posts in $selectedTab yet", style = MaterialTheme.typography.titleSmall)
                            }
                        }
                    } else {
                        items(filtered, key = { it.id }) { post ->
                            ProfilePostCompactCard(post) { onNavigate(SparkRoute.PostDetail(post.id)) }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(92.dp)) }
        }
    }

    if (showEditBioDialog) {
        AlertDialog(
            onDismissRequest = { showEditBioDialog = false },
            containerColor = SecondarySlate,
            title = { Text("Edit Creator Bio") },
            text = {
                OutlinedTextField(
                    value = editBioText,
                    onValueChange = { editBioText = it },
                    label = { Text("Bio") },
                    minLines = 3
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onUpdateBioAndCover(editBioText, null)
                    showEditBioDialog = false
                }) { Text("Save", color = ChampagneGold) }
            },
            dismissButton = {
                TextButton(onClick = { showEditBioDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun StatCounterColumn(label: String, value: String, onClick: (() -> Unit)? = null) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = if (onClick != null) {
            Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 6.dp)
        } else {
            Modifier.padding(6.dp)
        }
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = ChampagneGold, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = SecondaryIce)
    }
}

@Composable
private fun ProfilePostCompactCard(post: SocialPost, onClick: () -> Unit) {
    SparkCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(post.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${post.creationType.replace('_', ' ')} • ${post.viewsCount} views • ${post.likesCount} likes",
                    style = MaterialTheme.typography.bodySmall,
                    color = SoftGold
                )
            }
            SparkBadge(label = post.visibility.uppercase())
        }
    }
}

@Composable
fun FollowersListScreen(
    creatorId: String,
    showFollowing: Boolean,
    socialState: SocialUiState,
    onBack: () -> Unit,
    onNavigate: (SparkRoute) -> Unit
) {
    BackHandler { onBack() }
    val ids = if (showFollowing) socialState.followingCreatorIds else socialState.activeCreatorFollowers.map { it.followerId }.toSet()
    val matching = remember(ids, socialState.allCreatorProfiles) {
        socialState.allCreatorProfiles.filter { ids.contains(it.creatorId) }
    }

    Scaffold(
        topBar = {
            SparkTopBar(
                title = if (showFollowing) "Following" else "Followers",
                subtitle = "${ids.size} creators",
                onBack = onBack
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(SparkBrushes.CosmicBackground).padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (matching.isEmpty()) {
                item {
                    SparkCard {
                        Text(
                            text = if (showFollowing) "Not following any creators yet." else "No followers recorded yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SecondaryIce
                        )
                    }
                }
            } else {
                items(matching, key = { it.creatorId }) { prof ->
                    SparkCard(onClick = { onNavigate(SparkRoute.CreatorProfileRoute(prof.creatorId)) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CreatorAvatarCircle(prof.username)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("@${prof.username}", style = MaterialTheme.typography.titleMedium)
                                Text("${prof.followersCount} followers", style = MaterialTheme.typography.bodySmall, color = SoftGold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MyPostsScreen(
    socialState: SocialUiState,
    onDeletePost: (String) -> Unit,
    myMovies: List<MovieItem> = emptyList(),
    onToggleSave: (SocialPost) -> Unit = {},
    onShare: (SocialPost) -> Unit = {},
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
    Scaffold(
        topBar = {
            SparkTopBar(
                title = "My Posts & History",
                subtitle = "${socialState.myPosts.size} published posts",
                onBack = onBack
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(SparkBrushes.CosmicBackground).padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                SparkStatusBanner(socialState.statusMessage, socialState.errorMessage, onClearMessages)
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    SparkGoldButton(
                        text = "Create Another Video",
                        icon = Icons.Default.MovieCreation,
                        onClick = { onNavigate(SparkRoute.CreateMovie("text_to_movie")) },
                        modifier = Modifier.weight(1f).testTag("my_posts_create_another_btn")
                    )
                    SparkOutlinedButton(
                        text = "Recycle Bin",
                        icon = Icons.Default.DeleteSweep,
                        onClick = { onNavigate(SparkRoute.RecentlyDeleted) },
                        modifier = Modifier.weight(1f).testTag("my_posts_recycle_bin_btn")
                    )
                }
            }
            if (socialState.myPosts.isEmpty()) {
                item {
                    SparkCard {
                        Text("You haven't published any Spark Social posts yet", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        SparkGoldButton(
                            text = "Publish Your First Post",
                            onClick = { onNavigate(SparkRoute.UniversalPost()) }
                        )
                    }
                }
            } else {
                items(socialState.myPosts, key = { it.id }) { post ->
                    SparkCard(onClick = { onNavigate(SparkRoute.PostDetail(post.id)) }) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(post.title, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${post.visibility.uppercase()} • ${post.viewsCount} views (${post.uniqueViewersCount} unique) • ${post.likesCount} likes • ${post.commentsCount} comments",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SoftGold
                                )
                            }
                            PostMoreMenuAndDialogs(
                                post = post,
                                isOwner = true,
                                isSaved = socialState.savedPostIds.contains(post.id),
                                myMovies = myMovies,
                                onToggleSave = onToggleSave,
                                onShare = { p ->
                                    MediaUtils.shareMovieTextOrLink(context, p.title, p.previewVideoUrl, p.caption)
                                    onShare(p)
                                },
                                onEditPost = onEditPost,
                                onReplaceVideo = onReplaceVideo,
                                onSoftDeletePost = onSoftDeletePost,
                                onChangePrivacy = onChangePrivacy,
                                onToggleComments = onToggleComments,
                                onUpdateTemplatePermission = onUpdateTemplatePermission
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MyTemplatesScreen(
    socialState: SocialUiState,
    onUpdateTemplateMeta: (String, String, String, Boolean) -> Unit,
    onDeleteTemplate: (String) -> Unit,
    onBack: () -> Unit,
    onNavigate: (SparkRoute) -> Unit
) {
    BackHandler { onBack() }
    Scaffold(
        topBar = {
            SparkTopBar(
                title = "My Reusable Templates",
                subtitle = "${socialState.myTemplates.size} templates",
                onBack = onBack
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(SparkBrushes.CosmicBackground).padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(socialState.myTemplates, key = { it.id }) { tpl ->
                var allowReuse by remember(tpl.id, tpl.allowReuse) { mutableStateOf(tpl.allowReuse) }
                var vis by remember(tpl.id, tpl.visibility) { mutableStateOf(tpl.visibility) }
                SparkCard(onClick = { onNavigate(SparkRoute.TemplateDetail(tpl.id)) }) {
                    Text(tpl.title, style = MaterialTheme.typography.titleMedium)
                    Text("${tpl.remixesCount} remixes • ${tpl.likesCount} likes", style = MaterialTheme.typography.bodySmall, color = SoftGold)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CommunityCatalog.visibilities.forEach { v ->
                            FilterChip(selected = vis == v, onClick = { vis = v }, label = { Text(v) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        SparkOutlinedButton(
                            text = if (allowReuse) "Reuse On" else "Reuse Off",
                            onClick = { allowReuse = !allowReuse },
                            modifier = Modifier.weight(1f)
                        )
                        SparkGoldButton(
                            text = "Save",
                            onClick = { onUpdateTemplateMeta(tpl.id, tpl.description, vis, allowReuse) },
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onDeleteTemplate(tpl.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete template", tint = ErrorRed)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreatorAnalyticsScreen(
    socialState: SocialUiState,
    onBack: () -> Unit,
    onNavigate: (SparkRoute) -> Unit
) {
    BackHandler { onBack() }
    val stats = socialState.analytics

    Scaffold(
        topBar = {
            SparkTopBar(
                title = "Creator Analytics",
                subtitle = "Verified Firestore engagement metrics",
                onBack = onBack
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(SparkBrushes.CosmicBackground).padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SparkCard(borderColor = ChampagneGold.copy(alpha = 0.65f)) {
                    Text("Audience & Reach", style = MaterialTheme.typography.titleMedium, color = ChampagneGold)
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        StatCounterColumn("Followers", stats.followersCount.toString())
                        StatCounterColumn("Total Views", stats.totalViews.toString())
                        StatCounterColumn("Unique Viewers", stats.totalUniqueViewers.toString())
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        StatCounterColumn("Total Likes", stats.totalLikes.toString())
                        StatCounterColumn("Comments", stats.totalComments.toString())
                        StatCounterColumn("Shares", stats.totalShares.toString())
                    }
                }
            }

            item {
                SparkCard {
                    Text("Templates & Remixes", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${stats.totalPosts} published posts • ${stats.totalTemplates} templates • ${stats.totalRemixes} total template remixes",
                        style = MaterialTheme.typography.bodySmall,
                        color = SoftGold
                    )
                }
            }

            stats.topPerformingPost?.let { top ->
                item {
                    SparkCard(
                        borderColor = ChampagneGold,
                        onClick = { onNavigate(SparkRoute.PostDetail(top.id)) }
                    ) {
                        SparkBadge(label = "TOP PERFORMING POST")
                        Spacer(Modifier.height(6.dp))
                        Text(top.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${top.viewsCount} views • ${top.likesCount} likes • ${top.commentsCount} comments • ${top.sharesCount} shares",
                            style = MaterialTheme.typography.bodySmall,
                            color = SoftGold
                        )
                    }
                }
            }
        }
    }
}
