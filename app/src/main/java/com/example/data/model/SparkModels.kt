package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserAccount(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val fullName: String = "",
    val username: String = "",
    val country: String = "Nigeria",
    val phoneNumber: String = "",
    val photoUrl: String = "",
    val role: String = "creator",
    val accountStatus: String = "active",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val bio: String = "",
    val darkMode: Boolean = true,
    val appLanguage: String = "English",
    val notificationsEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val videoAutoplay: Boolean = true,
    val defaultQuality: String = "1080p",
    val defaultAspectRatio: String = "16:9",
    val defaultResolution: String = "1080p",
    val defaultDurationSec: Int = 30,
    val defaultMovieLanguage: String = "English",
    val defaultVoice: String = "Nigerian English — Warm Female",
    val defaultGenre: String = "Action",
    val autoSubtitles: Boolean = true,
    val autoSaveProjects: Boolean = true,
    val defaultPrivacy: String = "private",
    val allowDownloads: Boolean = true,
    val dataUsageSaver: Boolean = false,
    val downloadWifiOnly: Boolean = false,
    val notifyLikes: Boolean = true,
    val notifyComments: Boolean = true,
    val notifyFollowers: Boolean = true,
    val notifyMessages: Boolean = true,
    val notifyRemixes: Boolean = true,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MovieProject(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val genre: String = "Action",
    val countryLocation: String = "Lagos, Nigeria",
    val language: String = "English",
    val mainCharacterSummary: String = "",
    val characterAppearance: String = "",
    val characterClothing: String = "",
    val characterAgeRange: String = "25-35",
    val cameraAngle: String = "Dynamic Tracking Shot",
    val lighting: String = "Cinematic Golden & Neon Rim",
    val atmosphere: String = "High-Stakes Suspense",
    val backgroundSetting: String = "Lagos Mainland & Victoria Island Skyline",
    val visualStyle: String = "Photorealistic Cinema 4K",
    val voiceStyle: String = "Deep Dramatic Narration",
    val musicStyle: String = "Afrobeats Orchestral Hybrid",
    val aspectRatio: String = "16:9",
    val resolution: String = "1080p",
    val targetDurationSec: Int = 60,
    val estimatedCredits: Int = 2,
    val status: String = "draft",
    val publicProject: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MovieItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val projectId: String = "",
    val title: String = "",
    val genre: String = "Action",
    val durationSec: Int = 0,
    val resolution: String = "1080p",
    val aspectRatio: String = "16:9",
    val status: String = "draft",
    val thumbnailData: String = "",
    val videoUrl: String = "",
    val subtitlesText: String = "",
    val favorite: Boolean = false,
    val downloaded: Boolean = false,
    val publicMovie: Boolean = false,
    @ServerTimestamp val lastViewedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SceneItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val projectId: String = "",
    val sceneNumber: Int = 1,
    val title: String = "",
    val location: String = "",
    val description: String = "",
    val cameraInstruction: String = "",
    val shotDescription: String = "",
    val narration: String = "",
    val dialogue: String = "",
    val soundEffects: String = "",
    val durationSec: Int = 15,
    val storyboardData: String = "",
    val status: String = "planned",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ClipItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val projectId: String = "",
    val sceneId: String = "",
    val clipOrder: Int = 0,
    val title: String = "",
    val prompt: String = "",
    val durationSec: Int = 10,
    val trimStartMs: Int = 0,
    val trimEndMs: Int = 10000,
    val transitionType: String = "Cross Dissolve",
    val subtitleText: String = "",
    val voiceoverText: String = "",
    val musicTrackName: String = "",
    val videoUrl: String = "",
    val status: String = "pending",
    val errorMessage: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class GenerationJob(
    @DocumentId val id: String = "",
    val userId: String = "",
    val projectId: String = "",
    val movieTitle: String = "",
    val jobType: String = "text_to_movie",
    val status: String = "queued",
    val totalScenes: Int = 0,
    val totalClips: Int = 0,
    val completedClips: Int = 0,
    val failedClips: Int = 0,
    val estimatedCredits: Int = 0,
    val creditsCharged: Int = 0,
    val idempotencyKey: String = "",
    val providerId: String = "unconfigured",
    val errorMessage: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CharacterProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val role: String = "Lead Protagonist",
    val ageRange: String = "25-35",
    val appearance: String = "",
    val clothing: String = "",
    val hairstyle: String = "",
    val facialExpression: String = "Determined",
    val bodyPosture: String = "Confident upright stance",
    val voiceProfile: String = "Deep Warm Baritone",
    val fictional: Boolean = true,
    val likenessConsentConfirmed: Boolean = true,
    val referenceImageData: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ScriptDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val projectId: String = "",
    val title: String = "",
    val synopsis: String = "",
    val charactersSummary: String = "",
    val content: String = "",
    val aiSuggestion: String = "",
    val version: Int = 1,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SubscriptionPlan(
    @DocumentId val id: String = "",
    val userId: String = "",
    val planId: String = "free",
    val planName: String = "Free Creator",
    val status: String = "active",
    val monthlyCredits: Int = 20,
    val maxDurationSec: Int = 60,
    val maxResolution: String = "720p",
    val maxSimultaneousProjects: Int = 3,
    val storageQuotaMb: Int = 250,
    val priceUsd: Double = 0.0,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CreditBalance(
    @DocumentId val id: String = "",
    val userId: String = "",
    val balance: Int = 20,
    val promoCredits: Int = 0,
    val lifetimeEarned: Int = 20,
    val lifetimeSpent: Int = 0,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TransactionRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val type: String = "welcome_bonus",
    val amountCredits: Int = 0,
    val amountUsd: Double = 0.0,
    val description: String = "",
    val status: String = "completed",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DownloadRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val movieId: String = "",
    val movieTitle: String = "",
    val resolution: String = "1080p",
    val format: String = "MP4",
    val fileUri: String = "",
    val status: String = "completed",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class NotificationItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val category: String = "studio",
    val actorUserId: String = "",
    val targetPostId: String = "",
    val targetCreatorId: String = "",
    val readFlag: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SecurityLogItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val event: String = "",
    val deviceInfo: String = "",
    val status: String = "verified",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SupportTicket(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val subject: String = "",
    val message: String = "",
    val status: String = "open",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AdminSetting(
    @DocumentId val id: String = "",
    val key: String = "providerConfig",
    val providerName: String = "Unconfigured (Configure Backend Proxy)",
    val providerBaseUrl: String = "",
    val providerConfigured: Boolean = false,
    val maxFreeDurationSec: Int = 60,
    val maxPaidDurationSec: Int = 7200,
    val promoOfferTitle: String = "Spark AI Welcome Bonus",
    val promoBonusCredits: Int = 20,
    val paymentProviderName: String = "Unconfigured (Configure Paystack / Stripe)",
    val paymentConfigured: Boolean = false,
    val liveStreamingProviderName: String = "Unconfigured (Configure Mux / LiveKit / Agora)",
    val liveStreamingBaseUrl: String = "",
    val liveStreamingConfigured: Boolean = false,
    val updatedBy: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

object SparkCatalog {
    val genres = listOf(
        "Action", "Romance", "Comedy", "Horror", "Thriller", "Adventure",
        "Science fiction", "Fantasy", "Drama", "Nollywood", "Hollywood",
        "Documentary", "Animation", "Historical", "Musical", "Family", "Crime", "Mystery"
    )

    val movieLanguages = listOf(
        "English",
        "Nigerian Pidgin",
        "Yoruba",
        "Igbo",
        "Hausa",
        "French",
        "Spanish",
        "Portuguese",
        "Arabic",
        "Hindi",
        "Swahili",
        "Chinese (Mandarin)"
    )

    data class DurationPreset(val label: String, val seconds: Int)

    val durationPresets = listOf(
        DurationPreset("10 sec", 10),
        DurationPreset("15 sec", 15),
        DurationPreset("30 sec", 30),
        DurationPreset("1 min", 60),
        DurationPreset("3 min", 180),
        DurationPreset("5 min", 300),
        DurationPreset("10 min", 600),
        DurationPreset("20 min", 1200),
        DurationPreset("30 min", 1800),
        DurationPreset("60 min", 3600),
        DurationPreset("90 min", 5400),
        DurationPreset("120 min", 7200)
    )

    val aspectRatios = listOf("16:9", "9:16", "21:9 Cinema", "1:1", "4:3")
    val resolutions = listOf("720p", "1080p", "4K UHD")
    val cameraAngles = listOf(
        "Dynamic Tracking Shot", "Drone Aerial Sweep", "Close-Up Emotional",
        "Low-Angle Hero Shot", "Over-the-Shoulder", "Handheld Action Cam", "Steadicam Orbit"
    )
    val lightingStyles = listOf(
        "Cinematic Golden & Neon Rim", "Moody Film Noir", "Lagos Sunlight Natural",
        "Rainy Night Reflections", "Studio Softbox Glamour", "High-Contrast Thriller"
    )
    val visualStyles = listOf(
        "Photorealistic Cinema 4K", "Nollywood Theatrical", "Hollywood Blockbuster IMAX",
        "3D Pixar-Style Animation", "Cyberpunk Neo-Africa", "Vintage 35mm Film"
    )

    data class StudioTemplate(
        val id: String,
        val title: String,
        val genre: String,
        val country: String,
        val durationSec: Int,
        val prompt: String,
        val visualStyle: String
    )

    val starterTemplates = listOf(
        StudioTemplate(
            id = "tpl_lagos_action",
            title = "Secret Syndicate of Lagos",
            genre = "Nollywood",
            country = "Lagos, Nigeria",
            durationSec = 180,
            prompt = "A young investigative tech courier in Lagos uncovers an encrypted ledger belonging to a secret organization operating beneath Victoria Island.",
            visualStyle = "Nollywood Theatrical"
        ),
        StudioTemplate(
            id = "tpl_scifi_horizon",
            title = "Abuja Orbital 2099",
            genre = "Science fiction",
            country = "Abuja, Nigeria",
            durationSec = 300,
            prompt = "An aerospace engineer defends Africa's first orbital elevator during a solar storm sabotage.",
            visualStyle = "Cyberpunk Neo-Africa"
        ),
        StudioTemplate(
            id = "tpl_romance_gold",
            title = "Letters Across the Lagoon",
            genre = "Romance",
            country = "Lagos & London",
            durationSec = 600,
            prompt = "Two classical musicians reunite at a rooftop concert overlooking Ikoyi Lagoon after five years apart.",
            visualStyle = "Photorealistic Cinema 4K"
        ),
        StudioTemplate(
            id = "tpl_historical_epic",
            title = "Bronze Kingdom Rising",
            genre = "Historical",
            country = "Benin City, Nigeria",
            durationSec = 3600,
            prompt = "A 60-minute historical epic following royal artisans and strategists defending ancient trade routes.",
            visualStyle = "Hollywood Blockbuster IMAX"
        )
    )

    data class PlanTier(
        val planId: String,
        val name: String,
        val priceLabel: String,
        val monthlyCredits: Int,
        val maxDurationLabel: String,
        val maxDurationSec: Int,
        val maxResolution: String,
        val storageLabel: String,
        val features: List<String>
    )

    val planTiers = listOf(
        PlanTier(
            planId = "free",
            name = "Free Creator",
            priceLabel = "$0 / Free Forever",
            monthlyCredits = 20,
            maxDurationLabel = "Up to 60s clips (Unlimited script planning)",
            maxDurationSec = 60,
            maxResolution = "720p",
            storageLabel = "250 MB",
            features = listOf(
                "Free account & full Studio dashboard",
                "Unlimited Gemini script & storyboard planning",
                "Character Studio & Voice TTS previews",
                "20 starter credits + founder promo grants"
            )
        ),
        PlanTier(
            planId = "creator_pro",
            name = "Creator Pro",
            priceLabel = "$19 / month",
            monthlyCredits = 300,
            maxDurationLabel = "Up to 30 min multi-scene movies",
            maxDurationSec = 1800,
            maxResolution = "1080p",
            storageLabel = "10 GB",
            features = listOf(
                "300 monthly video generation credits",
                "1080p Full HD multi-scene stitching",
                "Priority generation queue",
                "Up to 10 simultaneous projects"
            )
        ),
        PlanTier(
            planId = "studio_ultra",
            name = "Studio Ultra",
            priceLabel = "$49 / month",
            monthlyCredits = 1200,
            maxDurationLabel = "Up to 120 min feature films",
            maxDurationSec = 7200,
            maxResolution = "4K UHD",
            storageLabel = "100 GB",
            features = listOf(
                "1,200 monthly credits for feature films",
                "4K UHD & 120-minute long-movie pipeline",
                "Custom external video provider hooks",
                "Priority founder & studio support"
            )
        )
    )

    fun estimateCredits(durationSec: Int, resolution: String): Int {
        val clipCount = ((durationSec + 9) / 10).coerceAtLeast(1)
        val multiplier = when {
            resolution.contains("4K", ignoreCase = true) -> 2
            else -> 1
        }
        return clipCount * multiplier
    }

    fun formatDuration(seconds: Int): String {
        return when {
            seconds < 60 -> "${seconds}s"
            seconds < 3600 -> {
                val mins = seconds / 60
                val rem = seconds % 60
                if (rem == 0) "${mins}m" else "${mins}m ${rem}s"
            }
            else -> {
                val hrs = seconds / 3600
                val mins = (seconds % 3600) / 60
                if (mins == 0) "${hrs}h" else "${hrs}h ${mins}m"
            }
        }
    }
}
