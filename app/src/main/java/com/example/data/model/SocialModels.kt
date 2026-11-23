package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class SocialPost(
    @DocumentId val id: String = "",
    val creatorId: String = "",
    val creatorUsername: String = "",
    val creatorAvatarUrl: String = "",
    val creationType: String = "movie", // text, photo, photo_album, short_video, movie, long_video, music_video, singing_photo, animation, template
    val mediaType: String = "video", // text, photo, photo_album, video
    val title: String = "",
    val caption: String = "",
    val hashtags: String = "",
    val mentions: String = "",
    val category: String = "Movie scenes",
    val previewVideoUrl: String = "",
    val thumbnailData: String = "",
    val photoThumbnails: List<String> = emptyList(),
    val soundId: String = "",
    val soundTitle: String = "",
    val durationSec: Int = 15,
    val aspectRatio: String = "9:16",
    val resolution: String = "1080p",
    val visibility: String = "public",
    val allowComments: Boolean = true,
    val allowReuse: Boolean = true,
    val allowDownload: Boolean = true,
    val viewsCount: Int = 0,
    val uniqueViewersCount: Int = 0,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val sharesCount: Int = 0,
    val repostsCount: Int = 0,
    val savesCount: Int = 0,
    val moderationStatus: String = "active",
    val linkedProjectId: String = "",
    val linkedTemplateId: String = "",
    val originalCreatorName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PostView(
    @DocumentId val id: String = "",
    val postId: String = "",
    val postCreatorId: String = "",
    val viewerUserId: String = "",
    val viewerUsername: String = "",
    val viewerAvatarUrl: String = "",
    val viewerPublic: Boolean = false,
    val viewCount: Int = 1,
    @ServerTimestamp val lastViewedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PostLike(
    @DocumentId val id: String = "",
    val postId: String = "",
    val postCreatorId: String = "",
    val userId: String = "",
    val username: String = "",
    val avatarUrl: String = "",
    val userPublic: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PostComment(
    @DocumentId val id: String = "",
    val postId: String = "",
    val postCreatorId: String = "",
    val parentCommentId: String = "",
    val userId: String = "",
    val username: String = "",
    val text: String = "",
    val likesCount: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PostCommentLike(
    @DocumentId val id: String = "",
    val commentId: String = "",
    val userId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PostShare(
    @DocumentId val id: String = "",
    val postId: String = "",
    val postCreatorId: String = "",
    val sharerUserId: String = "",
    val channel: String = "in_app",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SavedPostRef(
    @DocumentId val id: String = "",
    val userId: String = "",
    val postId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PostRepost(
    @DocumentId val id: String = "",
    val postId: String = "",
    val originalCreatorId: String = "",
    val reposterUserId: String = "",
    val reposterUsername: String = "",
    val note: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StoryItem(
    @DocumentId val id: String = "",
    val creatorId: String = "",
    val creatorUsername: String = "",
    val creatorAvatarUrl: String = "",
    val mediaType: String = "photo", // photo, video, text
    val mediaUrl: String = "",
    val thumbnailData: String = "",
    val overlayText: String = "",
    val stickerTag: String = "",
    val caption: String = "",
    val musicTrackTitle: String = "",
    val visibility: String = "public", // public, followers, private
    val viewsCount: Int = 0,
    val expiresAtMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StoryView(
    @DocumentId val id: String = "",
    val storyId: String = "",
    val storyCreatorId: String = "",
    val viewerUserId: String = "",
    val viewerUsername: String = "",
    val viewerAvatarUrl: String = "",
    val viewerPublic: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ConversationThread(
    @DocumentId val id: String = "",
    val participantIds: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val lastMessageText: String = "",
    val lastSenderId: String = "",
    val lastMessageType: String = "text",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DirectMessage(
    @DocumentId val id: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val senderUsername: String = "",
    val receiverId: String = "",
    val messageType: String = "text", // text, image, video, voice, audio
    val text: String = "",
    val mediaUrl: String = "",
    val mediaThumbnailData: String = "",
    val audioDurationSec: Int = 0,
    val replyToMessageId: String = "",
    val replyToPreview: String = "",
    val deliveryStatus: String = "sent", // sent, delivered, read
    val deletedForAll: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SavedSoundRef(
    @DocumentId val id: String = "",
    val userId: String = "",
    val soundId: String = "",
    val soundTitle: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LiveStreamSession(
    @DocumentId val id: String = "",
    val hostId: String = "",
    val hostUsername: String = "",
    val title: String = "",
    val category: String = "Live Cinema Q&A",
    val playbackUrl: String = "",
    val status: String = "ended",
    val viewerCount: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class MusicTrack(
    val id: String,
    val title: String,
    val artist: String,
    val category: String,
    val bpm: Int,
    val durationSec: Int,
    val mood: String,
    val licenseType: String,
    val localUri: String = ""
)

data class CreatorAnalyticsSummary(
    val totalPosts: Int = 0,
    val totalViews: Int = 0,
    val totalUniqueViewers: Int = 0,
    val totalLikes: Int = 0,
    val totalComments: Int = 0,
    val totalShares: Int = 0,
    val totalTemplates: Int = 0,
    val totalRemixes: Int = 0,
    val followersCount: Int = 0,
    val topPerformingPost: SocialPost? = null
)

object MusicLibraryCatalog {
    val categories = listOf(
        "All",
        "African",
        "Nigerian",
        "Afrobeats",
        "Gospel",
        "Hip-Hop",
        "Pop",
        "Instrumental",
        "Royalty-Free",
        "My Uploads"
    )

    val curatedTracks = listOf(
        MusicTrack(
            id = "trk_lagos_pulse",
            title = "Lagos Golden Pulse",
            artist = "Spark AI Studio Library",
            category = "Afrobeats",
            bpm = 108,
            durationSec = 180,
            mood = "Energetic & Urban",
            licenseType = "Royalty-Free Creator License"
        ),
        MusicTrack(
            id = "trk_naija_cinema",
            title = "Victoria Island Horizon",
            artist = "Spark AI Studio Library",
            category = "Nigerian",
            bpm = 98,
            durationSec = 210,
            mood = "Cinematic Afropop",
            licenseType = "Royalty-Free Creator License"
        ),
        MusicTrack(
            id = "trk_african_drums",
            title = "Kingdom of Bronze Drums",
            artist = "Spark AI Studio Library",
            category = "African",
            bpm = 116,
            durationSec = 240,
            mood = "Epic Percussion",
            licenseType = "Royalty-Free Creator License"
        ),
        MusicTrack(
            id = "trk_gospel_grace",
            title = "Morning Grace Choir",
            artist = "Spark AI Studio Library",
            category = "Gospel",
            bpm = 78,
            durationSec = 225,
            mood = "Uplifting & Soulful",
            licenseType = "Royalty-Free Creator License"
        ),
        MusicTrack(
            id = "trk_hiphop_midnight",
            title = "Midnight Mainland 808",
            artist = "Spark AI Studio Library",
            category = "Hip-Hop",
            bpm = 92,
            durationSec = 165,
            mood = "Confident Street",
            licenseType = "Royalty-Free Creator License"
        ),
        MusicTrack(
            id = "trk_pop_starlight",
            title = "Champagne Starlight",
            artist = "Spark AI Studio Library",
            category = "Pop",
            bpm = 120,
            durationSec = 190,
            mood = "Bright & Modern",
            licenseType = "Royalty-Free Creator License"
        ),
        MusicTrack(
            id = "trk_inst_noir",
            title = "Obsidian Suspense Strings",
            artist = "Spark AI Studio Library",
            category = "Instrumental",
            bpm = 84,
            durationSec = 260,
            mood = "Thriller Score",
            licenseType = "Royalty-Free Creator License"
        ),
        MusicTrack(
            id = "trk_royalty_trailer",
            title = "Trailer Rise & Impact",
            artist = "Spark AI Studio Library",
            category = "Royalty-Free",
            bpm = 130,
            durationSec = 95,
            mood = "Blockbuster Epic",
            licenseType = "Royalty-Free Creator License"
        )
    )

    val creationTypes = listOf(
        "text" to "Text Post",
        "photo" to "Photo",
        "photo_album" to "Photo Album",
        "short_video" to "Short Video",
        "long_video" to "Long Video",
        "movie" to "Full Movie",
        "music_video" to "Music Video",
        "singing_photo" to "Singing Video",
        "animation" to "AI Animation",
        "template" to "Template"
    )

    val stickerTags = listOf(
        "Cinema Gold",
        "Lagos Night",
        "Director Cut",
        "New Drop",
        "Afrobeats",
        "AI Remix"
    )
}
