package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class SparkGuideConversation(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "New Spark Guide Chat",
    val contextSection: String = "general",
    val language: String = "English",
    val lastMessagePreview: String = "",
    val messageCount: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SparkGuideMessage(
    @DocumentId val id: String = "",
    val conversationId: String = "",
    val userId: String = "",
    val role: String = "user", // user, assistant
    val text: String = "",
    val offlineFallback: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SparkGuideArticle(
    @DocumentId val id: String = "",
    val title: String = "",
    val category: String = "Getting Started",
    val stepsSummary: String = "",
    val keywords: String = "",
    val published: Boolean = true,
    val updatedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RecentlyDeletedItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val itemType: String = "post", // post, movie, project
    val originalId: String = "",
    val title: String = "",
    val subtitle: String = "",
    val creationType: String = "",
    val previewVideoUrl: String = "",
    val thumbnailData: String = "",
    val payloadJson: String = "",
    val deletedAtMs: Long = 0L,
    val expiresAtMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ActiveSessionItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val username: String = "",
    val deviceLabel: String = "",
    val platform: String = "Android",
    val status: String = "active", // active, revoked
    val currentSession: Boolean = false,
    @ServerTimestamp val lastActiveAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SecurityAlertItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val alertType: String = "suspicious_input", // new_device, rate_limit, suspicious_input, likeness_report, user_report, session_revoked
    val severity: String = "medium", // low, medium, high, critical
    val title: String = "",
    val description: String = "",
    val deviceLabel: String = "",
    val resolvedFlag: Boolean = false,
    val resolvedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AuditLogItem(
    @DocumentId val id: String = "",
    val actorUserId: String = "",
    val actorEmail: String = "",
    val action: String = "",
    val targetCollection: String = "",
    val targetId: String = "",
    val summary: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class OfflineFaqEntry(
    val id: String,
    val title: String,
    val keywords: List<String>,
    val answer: String
)

object SparkGuideCatalog {
    const val WELCOME_MESSAGE =
        "Hello! I'm Spark Guide, your personal Spark AI assistant.\n" +
            "Ask me anything about creating movies, singing videos, music videos, uploading photos, editing posts, deleting content, templates, settings or privacy."

    val suggestedQuestions = listOf(
        "How do I create a movie?",
        "How do I make a singing video?",
        "How do I post a video or photo?",
        "How do I replace or delete my post?",
        "Where do I find my saved videos?",
        "How do I change my privacy or security settings?"
    )

    val supportedLanguages = listOf(
        "English",
        "Nigerian Pidgin",
        "French",
        "Spanish",
        "Yoruba",
        "Igbo",
        "Hausa"
    )

    val settingsHelpQuestions = mapOf(
        "profile" to listOf("How do I change my profile picture?", "How do I update my bio and username?"),
        "security" to listOf("How do I log out of all devices?", "How do I check my login activity and security alerts?"),
        "privacy" to listOf("How do I change my privacy settings?", "How do I disable comments or downloads on my posts?"),
        "notifications" to listOf("How do I find my notifications?", "How do I mute likes or comment alerts?"),
        "studio" to listOf("How do I set default video quality and aspect ratio?", "How do I check my credits?"),
        "account" to listOf("How do I subscribe?", "How do I recover deleted videos from the Recycle Bin?"),
        "help" to listOf("How do I report a bug or security issue?", "How do I contact Spark AI support?")
    )

    val offlineFaqEntries = listOf(
        OfflineFaqEntry(
            id = "create_movie",
            title = "How do I create a movie?",
            keywords = listOf("movie", "film", "create movie", "text to movie", "storyboard"),
            answer = "1. Open the Home Dashboard and tap Create Movie (or the Create tab in the bottom bar).\n" +
                "2. Enter your Movie Title, Description, Genre, Location, Duration, and Visual Style.\n" +
                "3. Tap Generate AI Blueprint & Start Project to let Gemini build your script and scenes.\n" +
                "4. Open Script & Storyboard to review scenes or Movie Editor to refine clips.\n" +
                "5. Open Render Jobs or My Creations -> My Movies when your project is ready."
        ),
        OfflineFaqEntry(
            id = "singing_video",
            title = "How do I make a singing video?",
            keywords = listOf("singing", "lip sync", "portrait", "singing photo", "singing video"),
            answer = "1. On the Home Dashboard, tap AI Singing Photo (or open the Create tab).\n" +
                "2. Pick a portrait photo from your phone or generate one with Character Studio.\n" +
                "3. Choose a song from Music Library or enter lyrics/voice mood.\n" +
                "4. Confirm likeness permission for real faces and tap Generate Singing Video."
        ),
        OfflineFaqEntry(
            id = "music_and_upload",
            title = "How do I upload music or make a music video?",
            keywords = listOf("music", "song", "audio", "music video", "sound"),
            answer = "1. Open Music Library from the Home Dashboard or Discover -> Sounds.\n" +
                "2. Tap Upload Audio to import a song from your phone, or browse Afrobeats, Nigerian, Gospel, and Royalty-Free tracks.\n" +
                "3. Tap Make Video next to any track to open Music Video Creator and plan beat-synced scenes."
        ),
        OfflineFaqEntry(
            id = "upload_photo_post",
            title = "How do I upload a photo or publish a video?",
            keywords = listOf("upload", "photo", "photograph", "publish", "post", "album"),
            answer = "1. Tap the Create (+) tab or the POST button on the Home Dashboard.\n" +
                "2. Choose Post Type: Photo, Photo Album, Short Video, Long Video, Full Movie, Music Video, or Template.\n" +
                "3. Tap Pick Media from Phone (Android Photo Picker) or import from My Creations.\n" +
                "4. Add your Title, Caption, Hashtags, Sound, Privacy, and Template permissions.\n" +
                "5. Tap Publish Now to appear on Spark Social, Community, and your My Posts page."
        ),
        OfflineFaqEntry(
            id = "edit_replace_delete",
            title = "How do I edit, replace, delete or restore a post?",
            keywords = listOf("delete", "replace", "edit", "recycle", "bin", "restore", "trash"),
            answer = "1. Open your post on Spark Social, My Posts, or Post Detail and tap the three-dot menu (⋮).\n" +
                "2. Tap Edit Post to update the title, caption, hashtags, or visibility.\n" +
                "3. Tap Replace Video to swap the video from My Creations or your phone (you can keep the original post link or publish as a new post).\n" +
                "4. Tap Delete Post to move it safely to Recently Deleted (Recycle Bin) for 30 days.\n" +
                "5. To restore or permanently delete, go to Profile / My Creations / Settings -> Recently Deleted."
        ),
        OfflineFaqEntry(
            id = "saved_and_download",
            title = "Where do I find saved videos & download my movie?",
            keywords = listOf("saved", "save", "download", "bookmark", "my creations", "my movies"),
            answer = "1. For Saved Social Posts: open Profile -> Saved Posts or Discover -> Saved.\n" +
                "2. For Rendered Movies & Drafts: open Home Dashboard -> My Movies (or Profile -> My Creations).\n" +
                "3. Select your movie and tap Download or Export to save it or share via phone apps."
        ),
        OfflineFaqEntry(
            id = "templates",
            title = "How do I post or use a template?",
            keywords = listOf("template", "remix", "use template"),
            answer = "1. To post a template: open Universal Post, toggle Allow others to remix as template, or select Template as the post type.\n" +
                "2. To use someone else's template: open Community Hub or Short Video Reels, tap Use Template, customize the prompt/cast, and generate your remix."
        ),
        OfflineFaqEntry(
            id = "follow_notifications",
            title = "How do I follow creators & find notifications?",
            keywords = listOf("follow", "followers", "notifications", "inbox", "messages"),
            answer = "1. Tap Follow on any post card, Short Reel, or Creator Profile.\n" +
                "2. View Following Feed from the Spark Social Home tab.\n" +
                "3. Open the Inbox tab in the bottom bar to read Direct Messages and tap Notifications (bell icon) for likes, comments, and security alerts."
        ),
        OfflineFaqEntry(
            id = "settings_security_credits",
            title = "How do I open Settings, Security Center, Credits & Subscriptions?",
            keywords = listOf("settings", "privacy", "security", "password", "subscribe", "credits", "device", "logout"),
            answer = "1. Open Profile -> Settings (gear icon) or Home Dashboard -> Settings.\n" +
                "2. Open Security & Devices to enable App Lock, view active devices, log out all other devices, check login logs, or report a security issue.\n" +
                "3. Open Privacy Settings to control default post visibility, viewer list visibility, and downloads.\n" +
                "4. Open Subscription & Credits from Home Dashboard or Settings to check your balance and plans."
        )
    )

    fun matchOfflineAnswer(query: String, articles: List<SparkGuideArticle> = emptyList()): OfflineFaqEntry? {
        val q = query.lowercase()
        val matchedArticle = articles.firstOrNull { art ->
            art.published && (
                q.contains(art.title.lowercase()) ||
                    art.keywords.split(",").any { kw -> kw.trim().isNotBlank() && q.contains(kw.trim().lowercase()) }
                )
        }
        if (matchedArticle != null) {
            return OfflineFaqEntry(
                id = matchedArticle.id,
                title = matchedArticle.title,
                keywords = matchedArticle.keywords.split(","),
                answer = matchedArticle.stepsSummary
            )
        }
        return offlineFaqEntries.maxByOrNull { entry ->
            entry.keywords.count { kw -> q.contains(kw) }
        }?.takeIf { entry -> entry.keywords.any { q.contains(it) } }
    }
}
