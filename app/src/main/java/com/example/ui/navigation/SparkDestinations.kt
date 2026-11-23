package com.example.ui.navigation

enum class SparkTab {
    HOME,
    DISCOVER,
    CREATE,
    INBOX,
    PROFILE
}

sealed interface SparkRoute {
    data class Main(val tab: SparkTab = SparkTab.HOME) : SparkRoute
    data class CreateMovie(
        val initialMode: String = "text_to_movie",
        val seedTitle: String = "",
        val seedPrompt: String = "",
        val seedGenre: String = "Action",
        val seedCountry: String = "Lagos, Nigeria",
        val seedVisualStyle: String = "Photorealistic Cinema 4K",
        val seedDurationSec: Int = 60
    ) : SparkRoute
    data object SingingPhoto : SparkRoute
    data class MusicLibrary(val pickerMode: Boolean = false) : SparkRoute
    data class MusicVideoStudio(val preselectedTrackId: String = "") : SparkRoute
    data object CharacterStudio : SparkRoute
    data class ScriptStoryboard(val projectId: String = "") : SparkRoute
    data object VoiceMusicStudio : SparkRoute
    data class MovieEditor(val projectId: String = "") : SparkRoute
    data object GenerationJobs : SparkRoute
    data class UniversalPost(
        val prefillTitle: String = "",
        val prefillType: String = "short_video",
        val prefillProjectId: String = "",
        val prefillVideoUrl: String = "",
        val prefillThumbnail: String = "",
        val prefillDurationSec: Int = 30,
        val prefillSoundId: String = "",
        val prefillSoundTitle: String = ""
    ) : SparkRoute
    data class PostDetail(val postId: String) : SparkRoute
    data class ShortVideoReels(val startPostId: String = "", val soundFilterId: String = "") : SparkRoute
    data class StoriesViewer(val startCreatorId: String = "") : SparkRoute
    data object CreateStory : SparkRoute
    data class ChatThread(
        val conversationId: String,
        val peerUserId: String,
        val peerUsername: String
    ) : SparkRoute
    data object SoundsDiscovery : SparkRoute
    data object LiveStreaming : SparkRoute
    data object SavedContent : SparkRoute
    data object CommunityHub : SparkRoute
    data class TemplateDetail(val templateId: String) : SparkRoute
    data object RemixWorkspace : SparkRoute
    data class CreatorProfileRoute(val creatorId: String) : SparkRoute
    data class FollowersList(val creatorId: String, val showFollowing: Boolean = false) : SparkRoute
    data object CreatorAnalytics : SparkRoute
    data object MyPosts : SparkRoute
    data object MyTemplates : SparkRoute
    data object MyCreations : SparkRoute
    data object MovieLibrary : SparkRoute
    data class VideoPlayer(
        val title: String,
        val videoUrl: String,
        val movieId: String = "",
        val subtitlesText: String = "",
        val resolution: String = "1080p",
        val postId: String = ""
    ) : SparkRoute
    data object Notifications : SparkRoute
    data object SubscriptionCredits : SparkRoute
    data class SettingsSub(val section: String) : SparkRoute
    data object SettingsHub : SparkRoute
    data class SparkGuide(val contextSection: String = "general", val initialQuestion: String = "") : SparkRoute
    data object RecentlyDeleted : SparkRoute
    data object AdminDashboard : SparkRoute
    data object AdminApiConfig : SparkRoute
    data object AdminSecurity : SparkRoute
    data object AdminGuideArticles : SparkRoute
}
