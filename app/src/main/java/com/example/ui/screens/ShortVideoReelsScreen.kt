package com.example.ui.screens

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import com.example.R
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.core.MediaUtils
import com.example.data.model.SocialPost
import com.example.ui.components.Base64Thumbnail
import com.example.ui.components.CreatorAvatarCircle
import com.example.ui.components.SparkBadge
import com.example.ui.components.SparkGoldButton
import com.example.ui.components.SparkOutlinedButton
import com.example.ui.navigation.SparkRoute
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.IceWhite
import com.example.ui.theme.MidnightBlack
import com.example.ui.theme.SecondaryIce
import com.example.ui.theme.SoftGold
import com.example.ui.viewmodel.SocialUiState

@OptIn(UnstableApi::class)
@Composable
fun ShortVideoReelsScreen(
    startPostId: String,
    soundFilterId: String,
    currentUid: String,
    socialState: SocialUiState,
    onToggleLike: (SocialPost) -> Unit,
    onToggleSave: (SocialPost) -> Unit,
    onToggleFollow: (String) -> Unit,
    onRecordShare: (SocialPost, String) -> Unit,
    onUseTemplate: (SocialPost) -> Unit,
    onBack: () -> Unit,
    onNavigate: (SparkRoute) -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current

    val reels = remember(socialState.publicPosts, socialState.blockedCreatorIds, soundFilterId) {
        val videoPosts = socialState.publicPosts
            .filterNot { socialState.blockedCreatorIds.contains(it.creatorId) }
            .filter {
                it.creationType in listOf("short_video", "singing_photo", "music_video", "animation", "movie") &&
                    (soundFilterId.isBlank() || it.soundId == soundFilterId)
            }
        videoPosts.ifEmpty {
            socialState.publicPosts.filterNot { socialState.blockedCreatorIds.contains(it.creatorId) }
        }
    }

    val startIdx = remember(reels, startPostId) {
        reels.indexOfFirst { it.id == startPostId }.coerceAtLeast(0)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightBlack)
    ) {
        if (reels.isEmpty()) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.SmartDisplay, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(10.dp))
                Text("No Short Reels Published Yet", style = MaterialTheme.typography.titleLarge, color = IceWhite)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Publish a short video, AI animation, singing photo, or music video to launch Spark Reels.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryIce
                )
                Spacer(Modifier.height(14.dp))
                SparkGoldButton(
                    text = "Create Short Reel",
                    onClick = { onNavigate(SparkRoute.UniversalPost(prefillType = "short_video")) }
                )
            }
        } else {
            val pagerState = rememberPagerState(initialPage = startIdx, pageCount = { reels.size })

            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize().testTag("short_reels_pager")
            ) { page ->
                val post = reels[page]
                val isCurrentPage = pagerState.settledPage == page
                val isLiked = socialState.myLikedPostIds.contains(post.id)
                val isSaved = socialState.savedPostIds.contains(post.id)
                val isFollowing = socialState.followingCreatorIds.contains(post.creatorId)

                var isPaused by remember(page) { mutableStateOf(false) }

                Box(modifier = Modifier.fillMaxSize().background(MidnightBlack)) {
                    if (isCurrentPage && post.previewVideoUrl.isNotBlank()) {
                        val player = remember(post.previewVideoUrl) {
                            ExoPlayer.Builder(context).build().apply {
                                setMediaItem(MediaItem.fromUri(post.previewVideoUrl))
                                repeatMode = Player.REPEAT_MODE_ONE
                                prepare()
                                playWhenReady = !isPaused
                            }
                        }
                        LaunchedEffect(player, isPaused) {
                            player.playWhenReady = !isPaused
                        }
                        DisposableEffect(player) {
                            onDispose { player.release() }
                        }
                        AndroidView(
                            factory = { ctx ->
                                (LayoutInflater.from(ctx).inflate(R.layout.view_spark_player, null, false) as PlayerView).apply {
                                    this.player = player
                                    useController = false
                                    layoutParams = FrameLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                }
                            },
                            update = {
                                it.player = player
                                it.useController = false
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { isPaused = !isPaused },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isPaused) {
                                Surface(
                                    color = MidnightBlack.copy(alpha = 0.72f),
                                    shape = CircleShape,
                                    border = BorderStroke(1.5.dp, ChampagneGold)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Resume reel",
                                        tint = ChampagneGold,
                                        modifier = Modifier.padding(16.dp).size(36.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Base64Thumbnail(
                            base64Data = post.thumbnailData,
                            contentDescription = post.title,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Bottom metadata overlay
                    Surface(
                        color = MidnightBlack.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(end = 68.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .weight(1f, fill = false)
                                        .clickable {
                                            onNavigate(SparkRoute.CreatorProfileRoute(post.creatorId))
                                        }
                                ) {
                                    CreatorAvatarCircle(post.creatorUsername)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "@${post.creatorUsername.ifBlank { "creator" }}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = IceWhite,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (post.creatorId != currentUid && post.creatorId.isNotBlank()) {
                                    SparkOutlinedButton(
                                        text = if (isFollowing) "Following" else "Follow",
                                        onClick = { onToggleFollow(post.creatorId) }
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(post.title, style = MaterialTheme.typography.titleMedium, color = IceWhite)
                            if (post.caption.isNotBlank()) {
                                Text(post.caption, style = MaterialTheme.typography.bodySmall, color = SecondaryIce, maxLines = 2)
                            }
                            if (post.hashtags.isNotBlank()) {
                                Text(post.hashtags, style = MaterialTheme.typography.labelSmall, color = ChampagneGold)
                            }
                            if (post.soundTitle.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { onNavigate(SparkRoute.SoundsDiscovery) }
                                ) {
                                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(post.soundTitle, style = MaterialTheme.typography.labelSmall, color = SoftGold)
                                }
                            }
                            if (post.allowReuse || post.linkedTemplateId.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                SparkGoldButton(
                                    text = "USE TEMPLATE",
                                    onClick = { onUseTemplate(post) },
                                    modifier = Modifier.testTag("reel_use_template_btn")
                                )
                            }
                        }
                    }

                    // Right action rail
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 10.dp, bottom = 24.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(onClick = { onToggleLike(post) }) {
                                Icon(
                                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Like",
                                    tint = if (isLiked) ChampagneGold else IceWhite
                                )
                            }
                            Text(post.likesCount.toString(), style = MaterialTheme.typography.labelSmall, color = IceWhite)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(onClick = { onNavigate(SparkRoute.PostDetail(post.id)) }) {
                                Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Comments", tint = IceWhite)
                            }
                            Text(post.commentsCount.toString(), style = MaterialTheme.typography.labelSmall, color = IceWhite)
                        }
                        IconButton(onClick = { onToggleSave(post) }) {
                            Icon(
                                imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Save",
                                tint = if (isSaved) ChampagneGold else IceWhite
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(onClick = {
                                MediaUtils.shareMovieTextOrLink(context, post.title, post.previewVideoUrl, post.caption)
                                onRecordShare(post, "reels_share")
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "Share", tint = IceWhite)
                            }
                            Text(post.sharesCount.toString(), style = MaterialTheme.typography.labelSmall, color = IceWhite)
                        }
                    }
                }
            }
        }

        // Top overlay back bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 36.dp, start = 8.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("reels_back_btn")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ChampagneGold)
            }
            SparkBadge(label = "SPARK REELS", color = ChampagneGold)
        }
    }
}
