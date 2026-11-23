package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.MediaUtils
import com.example.data.model.MovieItem
import com.example.data.model.SocialPost
import com.example.data.model.SparkCatalog
import com.example.ui.components.Base64Thumbnail
import com.example.ui.components.CreatorAvatarCircle
import com.example.ui.components.SparkBadge
import com.example.ui.components.SparkCard
import com.example.ui.components.SparkGoldButton
import com.example.ui.components.SparkOutlinedButton
import com.example.ui.components.SparkStatusBanner
import com.example.ui.navigation.SparkRoute
import com.example.ui.theme.ChampagneGold
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
fun SparkSocialHomeScreen(
    studioState: StudioUiState,
    socialState: SocialUiState,
    currentUid: String = studioState.account?.userId.orEmpty(),
    isFounderAdmin: Boolean,
    onToggleLike: (SocialPost) -> Unit,
    onToggleSave: (SocialPost) -> Unit,
    onRepost: (SocialPost) -> Unit,
    onRecordShare: (SocialPost, String) -> Unit,
    onToggleFollow: (String) -> Unit,
    onUseTemplatePost: (SocialPost) -> Unit,
    onEditPost: (postId: String, title: String, caption: String, hashtags: String, visibility: String, allowComments: Boolean, allowReuse: Boolean, allowDownload: Boolean) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onReplaceVideo: (post: SocialPost, newVideoUrl: String, newThumb: String, durationSec: Int, res: String, keepLink: Boolean, title: String, caption: String) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onSoftDeletePost: (SocialPost) -> Unit = {},
    onChangePrivacy: (String, String) -> Unit = { _, _ -> },
    onToggleComments: (String, Boolean) -> Unit = { _, _ -> },
    onUpdateTemplatePermission: (String, Boolean) -> Unit = { _, _ -> },
    onClearMessages: () -> Unit,
    onNavigate: (SparkRoute) -> Unit
) {
    var topSurfaceTab by rememberSaveable { mutableIntStateOf(0) } // 0 = Spark Social, 1 = AI Studio Hub
    var feedFilterTab by rememberSaveable { mutableIntStateOf(0) } // 0 = For You, 1 = Following

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SparkBrushes.CosmicBackground)
    ) {
        Surface(color = MidnightBlack.copy(alpha = 0.94f)) {
            TabRow(
                selectedTabIndex = topSurfaceTab,
                containerColor = MidnightBlack,
                contentColor = ChampagneGold,
                indicator = { positions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(positions[topSurfaceTab]),
                        color = ChampagneGold
                    )
                }
            ) {
                Tab(
                    selected = topSurfaceTab == 0,
                    onClick = { topSurfaceTab = 0 },
                    text = { Text("SPARK SOCIAL", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("home_mode_social")
                )
                Tab(
                    selected = topSurfaceTab == 1,
                    onClick = { topSurfaceTab = 1 },
                    text = { Text("AI STUDIO HUB", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("home_mode_studio")
                )
            }
        }

        if (topSurfaceTab == 1) {
            DashboardScreen(
                uiState = studioState,
                isFounderAdmin = isFounderAdmin,
                onNavigate = onNavigate
            )
            return
        }

        val visiblePosts = remember(
            socialState.publicPosts,
            socialState.blockedCreatorIds,
            socialState.followingCreatorIds,
            feedFilterTab
        ) {
            val unblocked = socialState.publicPosts.filterNot {
                socialState.blockedCreatorIds.contains(it.creatorId)
            }
            if (feedFilterTab == 1) {
                unblocked.filter { socialState.followingCreatorIds.contains(it.creatorId) }
            } else {
                unblocked
            }
        }

        val storyCreators = remember(socialState.activeStories, socialState.blockedCreatorIds) {
            socialState.activeStories
                .filterNot { socialState.blockedCreatorIds.contains(it.creatorId) }
                .distinctBy { it.creatorId }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SparkStatusBanner(
                    statusMessage = socialState.statusMessage,
                    errorMessage = socialState.errorMessage,
                    onDismiss = onClearMessages
                )
            }

            // 24-Hour Stories Bar
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(72.dp)
                                .clickable { onNavigate(SparkRoute.CreateStory) }
                                .testTag("add_story_btn")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(62.dp)
                                    .clip(CircleShape)
                                    .background(SecondarySlate)
                                    .border(1.5.dp, ChampagneGold, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Story", tint = ChampagneGold)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Your Story",
                                style = MaterialTheme.typography.labelSmall,
                                color = ChampagneGold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    items(storyCreators, key = { it.id }) { story ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(72.dp)
                                .clickable { onNavigate(SparkRoute.StoriesViewer(story.creatorId)) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(62.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, ChampagneGold, CircleShape)
                                    .padding(3.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (story.thumbnailData.isNotBlank()) {
                                    Base64Thumbnail(
                                        base64Data = story.thumbnailData,
                                        contentDescription = story.creatorUsername,
                                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                                    )
                                } else {
                                    CreatorAvatarCircle(story.creatorUsername, modifier = Modifier.fillMaxSize())
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "@${story.creatorUsername}",
                                style = MaterialTheme.typography.labelSmall,
                                color = IceWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Quick Social & Creation Bar
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        SparkGoldButton(
                            text = "Short Reels",
                            icon = Icons.Default.SmartDisplay,
                            onClick = { onNavigate(SparkRoute.ShortVideoReels()) },
                            modifier = Modifier.testTag("home_open_reels_btn")
                        )
                    }
                    item {
                        SparkOutlinedButton(
                            text = "Templates",
                            icon = Icons.Default.Style,
                            onClick = { onNavigate(SparkRoute.CommunityHub) },
                            modifier = Modifier.testTag("home_open_templates_btn")
                        )
                    }
                    item {
                        SparkOutlinedButton(
                            text = "Sounds",
                            icon = Icons.Default.LibraryMusic,
                            onClick = { onNavigate(SparkRoute.SoundsDiscovery) },
                            modifier = Modifier.testTag("home_open_sounds_btn")
                        )
                    }
                    item {
                        SparkOutlinedButton(
                            text = "Movie Library",
                            icon = Icons.Default.VideoLibrary,
                            onClick = { onNavigate(SparkRoute.MovieLibrary) },
                            modifier = Modifier.testTag("home_open_movie_library_btn")
                        )
                    }
                    item {
                        SparkOutlinedButton(
                            text = "AI Movie",
                            icon = Icons.Default.AutoAwesome,
                            onClick = { onNavigate(SparkRoute.CreateMovie("text_to_movie")) }
                        )
                    }
                }
            }

            // For You / Following sub-tabs
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf("For You", "Following").forEachIndexed { idx, label ->
                        val selected = feedFilterTab == idx
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (selected) ChampagneGold else SecondarySlate,
                            border = BorderStroke(1.dp, if (selected) ChampagneGold else SteelBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(50))
                                .clickable { feedFilterTab = idx }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) MidnightBlack else IceWhite,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            if (visiblePosts.isEmpty()) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SparkCard {
                            Text(
                                text = if (feedFilterTab == 1) "No posts from followed creators yet" else "Spark Social feed is ready",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = if (feedFilterTab == 1) {
                                    "Follow creators in Discover or For You to see their movies, photos, and music videos here."
                                } else {
                                    "Publish your first movie, photo album, singing video, or AI template to kick off the community feed."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = SecondaryIce
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                SparkGoldButton(
                                    text = "Create Post",
                                    icon = Icons.Default.PostAdd,
                                    onClick = { onNavigate(SparkRoute.UniversalPost()) },
                                    modifier = Modifier.weight(1f)
                                )
                                SparkOutlinedButton(
                                    text = "Discover",
                                    onClick = { onNavigate(SparkRoute.Main(com.example.ui.navigation.SparkTab.DISCOVER)) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            } else {
                items(visiblePosts, key = { it.id }) { post ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SocialPostCard(
                            post = post,
                            isLiked = socialState.myLikedPostIds.contains(post.id),
                            isSaved = socialState.savedPostIds.contains(post.id),
                            isFollowing = socialState.followingCreatorIds.contains(post.creatorId),
                            currentUid = currentUid.ifBlank { studioState.account?.userId.orEmpty() },
                            onOpenPost = { onNavigate(SparkRoute.PostDetail(post.id)) },
                            onOpenPlayer = {
                                if (post.creationType == "short_video") {
                                    onNavigate(SparkRoute.ShortVideoReels(startPostId = post.id))
                                } else {
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
                            onOpenCreator = { onNavigate(SparkRoute.CreatorProfileRoute(post.creatorId)) },
                            onOpenSound = { onNavigate(SparkRoute.SoundsDiscovery) },
                            onToggleLike = { onToggleLike(post) },
                            onToggleSave = { onToggleSave(post) },
                            onRepost = { onRepost(post) },
                            onRecordShare = { channel -> onRecordShare(post, channel) },
                            onToggleFollow = { onToggleFollow(post.creatorId) },
                            onUseTemplate = { onUseTemplatePost(post) },
                            myMovies = studioState.movies,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SocialPostCard(
    post: SocialPost,
    isLiked: Boolean,
    isSaved: Boolean,
    isFollowing: Boolean,
    currentUid: String,
    onOpenPost: () -> Unit,
    onOpenPlayer: () -> Unit,
    onOpenCreator: () -> Unit,
    onOpenSound: () -> Unit,
    onToggleLike: () -> Unit,
    onToggleSave: () -> Unit,
    onRepost: () -> Unit,
    onRecordShare: (String) -> Unit,
    onToggleFollow: () -> Unit,
    onUseTemplate: () -> Unit,
    myMovies: List<MovieItem> = emptyList(),
    onEditPost: (postId: String, title: String, caption: String, hashtags: String, visibility: String, allowComments: Boolean, allowReuse: Boolean, allowDownload: Boolean) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onReplaceVideo: (post: SocialPost, newVideoUrl: String, newThumb: String, durationSec: Int, res: String, keepLink: Boolean, title: String, caption: String) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onSoftDeletePost: (SocialPost) -> Unit = {},
    onChangePrivacy: (String, String) -> Unit = { _, _ -> },
    onToggleComments: (String, Boolean) -> Unit = { _, _ -> },
    onUpdateTemplatePermission: (String, Boolean) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current

    SparkCard(onClick = onOpenPost) {
        // Creator header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onOpenCreator)
            ) {
                CreatorAvatarCircle(post.creatorUsername)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = "@${post.creatorUsername.ifBlank { "creator" }}",
                        style = MaterialTheme.typography.titleSmall,
                        color = IceWhite,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${post.category}${if (post.durationSec > 0) " • ${SparkCatalog.formatDuration(post.durationSec)}" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryIce,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                SparkBadge(label = post.creationType.replace('_', ' ').uppercase())
                if (post.creatorId != currentUid && post.creatorId.isNotBlank()) {
                    SparkOutlinedButton(
                        text = if (isFollowing) "Following" else "Follow",
                        onClick = onToggleFollow
                    )
                }
                PostMoreMenuAndDialogs(
                    post = post,
                    isOwner = post.creatorId == currentUid && currentUid.isNotBlank(),
                    isSaved = isSaved,
                    myMovies = myMovies,
                    onToggleSave = { onToggleSave() },
                    onShare = { p ->
                        MediaUtils.shareMovieTextOrLink(context, p.title, p.previewVideoUrl, p.caption)
                        onRecordShare("system_share")
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

        Spacer(Modifier.height(10.dp))

        // Media body (Album, Single Photo, Video Preview, or Text)
        if (post.photoThumbnails.size > 1) {
            val pagerState = rememberPagerState(pageCount = { post.photoThumbnails.size })
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .clip(RoundedCornerShape(14.dp))
            ) {
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { idx ->
                    Base64Thumbnail(
                        base64Data = post.photoThumbnails[idx],
                        contentDescription = "${post.title} photo ${idx + 1}",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Surface(
                    color = MidnightBlack.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "${pagerState.currentPage + 1}/${post.photoThumbnails.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ChampagneGold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        } else if (post.thumbnailData.isNotBlank() || post.mediaType == "video") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = if (post.mediaType == "video") onOpenPlayer else onOpenPost),
                contentAlignment = Alignment.Center
            ) {
                Base64Thumbnail(
                    base64Data = post.thumbnailData,
                    contentDescription = post.title,
                    modifier = Modifier.fillMaxSize(),
                    placeholderIcon = Icons.Default.Movie
                )
                if (post.mediaType == "video") {
                    Surface(
                        color = MidnightBlack.copy(alpha = 0.72f),
                        shape = CircleShape,
                        border = BorderStroke(1.5.dp, ChampagneGold)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play video",
                            tint = ChampagneGold,
                            modifier = Modifier.padding(12.dp).size(28.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        Text(post.title, style = MaterialTheme.typography.titleMedium)
        if (post.caption.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = post.caption,
                style = MaterialTheme.typography.bodySmall,
                color = SecondaryIce,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (post.hashtags.isNotBlank() || post.mentions.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = listOf(post.hashtags, post.mentions).filter { it.isNotBlank() }.joinToString("  "),
                style = MaterialTheme.typography.labelSmall,
                color = ChampagneGold
            )
        }

        if (post.soundTitle.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = ChampagneGold.copy(alpha = 0.12f),
                modifier = Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onOpenSound)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(post.soundTitle, style = MaterialTheme.typography.labelSmall, color = SoftGold)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = "${post.viewsCount} views • ${post.likesCount} likes • ${post.commentsCount} comments • ${post.sharesCount} shares",
            style = MaterialTheme.typography.labelSmall,
            color = SoftGold
        )

        Spacer(Modifier.height(6.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggleLike) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like post",
                        tint = if (isLiked) ChampagneGold else IceWhite
                    )
                }
                IconButton(onClick = onOpenPost) {
                    Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Comments", tint = IceWhite)
                }
                IconButton(onClick = onRepost) {
                    Icon(Icons.Default.Repeat, contentDescription = "Repost", tint = IceWhite)
                }
                IconButton(onClick = onToggleSave) {
                    Icon(
                        imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Save post",
                        tint = if (isSaved) ChampagneGold else IceWhite
                    )
                }
                IconButton(onClick = {
                    MediaUtils.shareMovieTextOrLink(context, post.title, post.previewVideoUrl, post.caption)
                    onRecordShare("system_share")
                }) {
                    Icon(Icons.Default.Share, contentDescription = "Share post", tint = IceWhite)
                }
            }
            if (post.linkedTemplateId.isNotBlank() || post.allowReuse) {
                SparkGoldButton(
                    text = "USE TEMPLATE",
                    onClick = onUseTemplate
                )
            }
        }
    }
}
