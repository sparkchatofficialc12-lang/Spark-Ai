package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class CommunityTemplate(
    @DocumentId val id: String = "",
    val creatorId: String = "",
    val creatorUsername: String = "",
    val creatorAvatarUrl: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "Movie scenes",
    val previewVideoUrl: String = "",
    val thumbnailData: String = "",
    val durationSec: Int = 15,
    val aspectRatio: String = "9:16",
    val requiredInputs: String = "Face reference or character photo",
    val effectsSummary: String = "",
    val tags: String = "",
    val visibility: String = "public",
    val allowReuse: Boolean = true,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val remixesCount: Int = 0,
    val trendingScore: Int = 0,
    val moderationStatus: String = "active",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TemplatePrompt(
    @DocumentId val id: String = "",
    val templateId: String = "",
    val creatorId: String = "",
    val visibility: String = "public",
    val allowReuse: Boolean = true,
    val basePrompt: String = "",
    val sceneDescription: String = "",
    val characterTemplate: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TemplateSettingsDoc(
    @DocumentId val id: String = "",
    val templateId: String = "",
    val creatorId: String = "",
    val visibility: String = "public",
    val allowReuse: Boolean = true,
    val cameraMovement: String = "Dynamic Tracking Shot",
    val lighting: String = "Cinematic Golden Rim",
    val videoStyle: String = "Photorealistic Cinema 4K",
    val voiceStyle: String = "Deep Dramatic Narration",
    val musicStyle: String = "Cinematic Orchestral",
    val aspectRatio: String = "9:16",
    val resolution: String = "1080p",
    val durationSec: Int = 15,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TemplateLike(
    @DocumentId val id: String = "",
    val templateId: String = "",
    val userId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TemplateComment(
    @DocumentId val id: String = "",
    val templateId: String = "",
    val userId: String = "",
    val username: String = "",
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TemplateRemix(
    @DocumentId val id: String = "",
    val originalTemplateId: String = "",
    val originalTemplateTitle: String = "",
    val originalCreatorId: String = "",
    val originalCreatorName: String = "",
    val remixerUserId: String = "",
    val remixerUsername: String = "",
    val newProjectId: String = "",
    val newMovieTitle: String = "",
    val customPrompt: String = "",
    val previewVideoUrl: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CreatorProfile(
    @DocumentId val id: String = "",
    val creatorId: String = "",
    val username: String = "",
    val displayName: String = "",
    val avatarUrl: String = "",
    val bio: String = "",
    val coverImageData: String = "",
    val publishedCount: Int = 0,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val totalLikes: Int = 0,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CreatorFollower(
    @DocumentId val id: String = "",
    val creatorId: String = "",
    val followerId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TemplateReport(
    @DocumentId val id: String = "",
    val templateId: String = "",
    val creatorId: String = "",
    val reporterId: String = "",
    val reason: String = "Inappropriate content",
    val details: String = "",
    val status: String = "open",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SavedTemplateRef(
    @DocumentId val id: String = "",
    val userId: String = "",
    val templateId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class BlockedCreatorRef(
    @DocumentId val id: String = "",
    val userId: String = "",
    val creatorId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class RemixWorkspaceSeed(
    val template: CommunityTemplate,
    val prompt: TemplatePrompt,
    val settings: TemplateSettingsDoc
)

object CommunityCatalog {
    val categories = listOf(
        "All",
        "Flying effects",
        "Superhero transformations",
        "Movie scenes",
        "Romance",
        "Action",
        "Fighting",
        "Dancing",
        "Music videos",
        "Cartoon animation",
        "Horror",
        "Comedy",
        "Fashion transformations",
        "Cinematic portraits",
        "Fantasy",
        "Magic effects",
        "Nigerian and African movie styles"
    )

    val reportReasons = listOf(
        "Copyright infringement",
        "Non-consensual real-person likeness",
        "Inappropriate or harmful content",
        "Misleading or broken prompt",
        "Spam or abuse"
    )

    val visibilities = listOf("public", "private", "unlisted")
}
