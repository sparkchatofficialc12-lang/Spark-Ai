package com.example.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.AuthManager.authStateFlow
import com.example.core.DeviceSessionHelper
import com.example.core.MediaUtils
import com.example.core.ThemePreferences
import com.example.core.VoiceAndAudioEngine
import com.example.data.ai.SparkAiService
import com.example.data.model.ActiveSessionItem
import com.example.data.model.AdminSetting
import com.example.data.model.CharacterProfile
import com.example.data.model.ClipItem
import com.example.data.model.CreditBalance
import com.example.data.model.DownloadRecord
import com.example.data.model.GenerationJob
import com.example.data.model.MovieItem
import com.example.data.model.MovieProject
import com.example.data.model.MusicLibraryCatalog
import com.example.data.model.MusicTrack
import com.example.data.model.NotificationItem
import com.example.data.model.RecentlyDeletedItem
import com.example.data.model.SceneItem
import com.example.data.model.ScriptDoc
import com.example.data.model.SecurityAlertItem
import com.example.data.model.SecurityLogItem
import com.example.data.model.SparkCatalog
import com.example.data.model.SubscriptionPlan
import com.example.data.model.SupportTicket
import com.example.data.model.TransactionRecord
import com.example.data.model.UserAccount
import com.example.data.model.UserProfile
import com.example.data.repository.CommunityRepository
import com.example.data.repository.GuideAndSecurityRepository
import com.example.data.repository.SparkRepository
import com.example.data.video.HttpVideoProviderAdapter
import com.example.data.video.LipSyncRequest
import com.example.data.video.MusicVideoRequest
import com.example.data.video.VideoClipRequest
import com.example.data.video.VideoProviderResult
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID

data class StudioUiState(
    val isBootstrapping: Boolean = true,
    val account: UserAccount? = null,
    val profile: UserProfile? = null,
    val credits: CreditBalance? = null,
    val subscription: SubscriptionPlan? = null,
    val adminSetting: AdminSetting? = null,
    val projects: List<MovieProject> = emptyList(),
    val movies: List<MovieItem> = emptyList(),
    val jobs: List<GenerationJob> = emptyList(),
    val characters: List<CharacterProfile> = emptyList(),
    val scripts: List<ScriptDoc> = emptyList(),
    val transactions: List<TransactionRecord> = emptyList(),
    val downloads: List<DownloadRecord> = emptyList(),
    val notifications: List<NotificationItem> = emptyList(),
    val securityLogs: List<SecurityLogItem> = emptyList(),
    val recentlyDeleted: List<RecentlyDeletedItem> = emptyList(),
    val activeSessions: List<ActiveSessionItem> = emptyList(),
    val securityAlerts: List<SecurityAlertItem> = emptyList(),
    val currentSessionId: String = "",
    val sessionRevoked: Boolean = false,
    val tickets: List<SupportTicket> = emptyList(),
    val uploadedTracks: List<MusicTrack> = emptyList(),
    val isBusy: Boolean = false,
    val busyLabel: String = "",
    val statusBanner: String? = null,
    val errorBanner: String? = null
)

class StudioViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    val repo = SparkRepository(appContext)
    private val communityRepo = CommunityRepository(appContext)
    private val securityRepo = GuideAndSecurityRepository(appContext)
    val aiService = SparkAiService()
    val videoAdapter = HttpVideoProviderAdapter()
    val voiceAudio = VoiceAndAudioEngine(appContext)
    val themePrefs = ThemePreferences(appContext)
    val deviceSessionId: String = DeviceSessionHelper.getOrCreateSessionId(appContext)

    private val _uiState = MutableStateFlow(StudioUiState(currentSessionId = deviceSessionId))
    val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

    val isDarkMode = themePrefs.darkModeFlow
    val appLockEnabled = themePrefs.appLockFlow

    private var userCollectorsJob: Job? = null
    private var activeUid: String? = null

    init {
        viewModelScope.launch {
            themePrefs.uploadedTracksFlow.catch {}.collect { tracks ->
                _uiState.value = _uiState.value.copy(uploadedTracks = tracks)
            }
        }
        viewModelScope.launch {
            Firebase.auth.authStateFlow().collectLatest { user ->
                userCollectorsJob?.cancel()
                if (user == null) {
                    activeUid = null
                    _uiState.value = StudioUiState(isBootstrapping = false)
                } else {
                    activeUid = user.uid
                    _uiState.value = _uiState.value.copy(isBootstrapping = true, errorBanner = null)
                    val bootResult = repo.bootstrapUserIfNeeded(user)
                    if (bootResult.isFailure) {
                        _uiState.value = _uiState.value.copy(
                            isBootstrapping = false,
                            errorBanner = bootResult.exceptionOrNull()?.localizedMessage
                                ?: "Failed to initialize Spark AI studio profile."
                        )
                    } else {
                        startObservers(user.uid)
                    }
                }
            }
        }
    }

    private fun startObservers(uid: String) {
        userCollectorsJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBootstrapping = false)
            launch { repo.observeAccount(uid).catch {}.collect { _uiState.value = _uiState.value.copy(account = it) } }
            launch {
                repo.observeProfile(uid).catch {}.collect { prof ->
                    _uiState.value = _uiState.value.copy(profile = prof)
                    if (prof != null) themePrefs.setDarkMode(prof.darkMode)
                }
            }
            launch { repo.observeCredits(uid).catch {}.collect { _uiState.value = _uiState.value.copy(credits = it) } }
            launch { repo.observeSubscription(uid).catch {}.collect { _uiState.value = _uiState.value.copy(subscription = it) } }
            launch { repo.observeAdminProviderSettings().catch {}.collect { _uiState.value = _uiState.value.copy(adminSetting = it) } }
            launch { repo.observeProjects(uid).catch {}.collect { _uiState.value = _uiState.value.copy(projects = it) } }
            launch { repo.observeMovies(uid).catch {}.collect { _uiState.value = _uiState.value.copy(movies = it) } }
            launch { repo.observeJobs(uid).catch {}.collect { _uiState.value = _uiState.value.copy(jobs = it) } }
            launch { repo.observeCharacters(uid).catch {}.collect { _uiState.value = _uiState.value.copy(characters = it) } }
            launch { repo.observeScripts(uid).catch {}.collect { _uiState.value = _uiState.value.copy(scripts = it) } }
            launch { repo.observeTransactions(uid).catch {}.collect { _uiState.value = _uiState.value.copy(transactions = it) } }
            launch { repo.observeDownloads(uid).catch {}.collect { _uiState.value = _uiState.value.copy(downloads = it) } }
            launch { repo.observeNotifications(uid).catch {}.collect { _uiState.value = _uiState.value.copy(notifications = it) } }
            launch { repo.observeSecurityLogs(uid).catch {}.collect { _uiState.value = _uiState.value.copy(securityLogs = it) } }
            launch { repo.observeRecentlyDeleted(uid).catch {}.collect { _uiState.value = _uiState.value.copy(recentlyDeleted = it) } }
            launch { securityRepo.observeUserActiveSessions(uid).catch {}.collect { _uiState.value = _uiState.value.copy(activeSessions = it) } }
            launch { securityRepo.observeUserSecurityAlerts(uid).catch {}.collect { _uiState.value = _uiState.value.copy(securityAlerts = it) } }
            launch {
                val user = Firebase.auth.currentUser
                val res = securityRepo.registerOrHeartbeatDeviceSession(
                    sessionId = deviceSessionId,
                    deviceLabel = DeviceSessionHelper.getDeviceLabel(),
                    email = user?.email.orEmpty(),
                    username = user?.displayName.orEmpty()
                )
                if (res.getOrNull() == false) {
                    _uiState.value = _uiState.value.copy(sessionRevoked = true)
                }
            }
            launch {
                securityRepo.observeCurrentSessionStatus(uid, deviceSessionId).catch {}.collect { status ->
                    if (status == "revoked") {
                        _uiState.value = _uiState.value.copy(sessionRevoked = true)
                    }
                }
            }
            launch { repo.observeTickets(uid).catch {}.collect { _uiState.value = _uiState.value.copy(tickets = it) } }
        }
    }

    fun clearBanners() {
        _uiState.value = _uiState.value.copy(statusBanner = null, errorBanner = null)
    }

    fun acknowledgeSessionRevoked() {
        _uiState.value = _uiState.value.copy(sessionRevoked = false)
    }

    fun setErrorBanner(msg: String) {
        _uiState.value = _uiState.value.copy(errorBanner = msg)
    }

    fun setStatusBanner(msg: String) {
        _uiState.value = _uiState.value.copy(statusBanner = msg)
    }

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            themePrefs.setDarkMode(enabled)
            val current = _uiState.value.profile ?: return@launch
            repo.updateUserProfile(current.copy(darkMode = enabled))
        }
    }

    fun toggleAppLock(enabled: Boolean) {
        viewModelScope.launch {
            themePrefs.setAppLockEnabled(enabled)
            _uiState.value = _uiState.value.copy(
                statusBanner = if (enabled) "Spark AI App Lock protection enabled on this device." else "App Lock disabled."
            )
        }
    }

    fun updateAccountInfo(
        fullName: String,
        username: String,
        country: String,
        phoneNumber: String,
        avatarBitmap: Bitmap? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true, busyLabel = "Updating account...")
            try {
                val encodedAvatar = avatarBitmap?.let {
                    "data:image/jpeg;base64,${MediaUtils.bitmapToCompactBase64Async(it, 180_000)}"
                }
                val res = repo.updateAccountInfo(fullName, username, country, phoneNumber, encodedAvatar)
                _uiState.value = _uiState.value.copy(
                    statusBanner = if (res.isSuccess) "Account profile saved." else null,
                    errorBanner = res.exceptionOrNull()?.localizedMessage
                )
            } finally {
                _uiState.value = _uiState.value.copy(isBusy = false, busyLabel = "")
            }
        }
    }

    fun updateProfileSettings(updated: UserProfile) {
        viewModelScope.launch {
            themePrefs.setDarkMode(updated.darkMode)
            val res = repo.updateUserProfile(updated)
            _uiState.value = _uiState.value.copy(
                statusBanner = if (res.isSuccess) "Studio preferences saved." else null,
                errorBanner = res.exceptionOrNull()?.localizedMessage
            )
        }
    }

    fun generateAndCreateMovieProject(
        title: String,
        description: String,
        genre: String,
        countryLocation: String,
        language: String,
        mainCharacterSummary: String,
        characterAppearance: String,
        characterClothing: String,
        characterAgeRange: String,
        cameraAngle: String,
        lighting: String,
        atmosphere: String,
        backgroundSetting: String,
        visualStyle: String,
        voiceStyle: String,
        musicStyle: String,
        aspectRatio: String,
        resolution: String,
        targetDurationSec: Int,
        publicProject: Boolean,
        jobType: String = "text_to_movie",
        referenceBitmap: Bitmap? = null,
        onProjectCreated: (String) -> Unit = {}
    ) {
        if (title.isBlank()) {
            setErrorBanner("Please enter a movie title.")
            return
        }
        val state = _uiState.value
        val plan = state.subscription
        val setting = state.adminSetting ?: AdminSetting()
        val maxAllowedDuration = if (plan == null || plan.planId == "free") {
            setting.maxFreeDurationSec.coerceAtLeast(plan?.maxDurationSec ?: 60)
        } else {
            setting.maxPaidDurationSec.coerceAtLeast(plan.maxDurationSec)
        }

        val estimatedCredits = SparkCatalog.estimateCredits(targetDurationSec, resolution)
        val currentCredits = state.credits?.balance ?: 0

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isBusy = true,
                busyLabel = "Gemini AI is directing your screenplay, scenes & storyboard..."
            )
            try {
                val project = MovieProject(
                    title = title.trim(),
                    description = description.trim(),
                    genre = genre,
                    countryLocation = countryLocation,
                    language = language.trim().ifBlank { "English" },
                    mainCharacterSummary = mainCharacterSummary,
                    characterAppearance = characterAppearance,
                    characterClothing = characterClothing,
                    characterAgeRange = characterAgeRange,
                    cameraAngle = cameraAngle,
                    lighting = lighting,
                    atmosphere = atmosphere,
                    backgroundSetting = backgroundSetting,
                    visualStyle = visualStyle,
                    voiceStyle = voiceStyle,
                    musicStyle = musicStyle,
                    aspectRatio = aspectRatio,
                    resolution = resolution,
                    targetDurationSec = targetDurationSec,
                    estimatedCredits = estimatedCredits,
                    status = "scripting",
                    publicProject = publicProject
                )

                val saveRes = repo.saveProject(project)
                if (saveRes.isFailure) {
                    _uiState.value = _uiState.value.copy(
                        errorBanner = saveRes.exceptionOrNull()?.localizedMessage ?: "Failed to save movie project."
                    )
                    return@launch
                }
                val projectId = saveRes.getOrThrow()
                val refThumbBase64 = referenceBitmap?.let { MediaUtils.bitmapToCompactBase64Async(it, 180_000) }.orEmpty()

                // Generate structured screenplay & scene plan with Firebase AI Logic (Gemini)
                val blueprintRes = aiService.generateMovieBlueprint(project.copy(id = projectId))
                val blueprint = blueprintRes.getOrNull()
                if (blueprint != null) {
                    repo.saveScript(
                        ScriptDoc(
                            projectId = projectId,
                            title = "${project.title} — Screenplay",
                            synopsis = blueprint.synopsis,
                            charactersSummary = blueprint.characterSheet,
                            content = blueprint.fullScript,
                            version = 1
                        )
                    )
                    blueprint.scenes.forEachIndexed { idx, sc ->
                        val sceneId = repo.saveScene(
                            projectId,
                            SceneItem(
                                projectId = projectId,
                                sceneNumber = sc.sceneNumber,
                                title = sc.title,
                                location = sc.location,
                                description = sc.description,
                                cameraInstruction = sc.cameraInstruction,
                                shotDescription = sc.shotDescription,
                                narration = sc.narration,
                                dialogue = sc.dialogue,
                                soundEffects = sc.soundEffects,
                                durationSec = sc.durationSec,
                                storyboardData = if (idx == 0) refThumbBase64 else "",
                                status = "planned"
                            )
                        ).getOrNull() ?: "scene_${idx + 1}"

                        repo.saveClip(
                            projectId,
                            ClipItem(
                                projectId = projectId,
                                sceneId = sceneId,
                                clipOrder = idx,
                                title = "Clip ${idx + 1}: ${sc.title}",
                                prompt = "${sc.description} Camera: ${sc.cameraInstruction}. Lighting: $lighting. Style: $visualStyle.",
                                durationSec = sc.durationSec.coerceIn(5, 600),
                                trimStartMs = 0,
                                trimEndMs = sc.durationSec * 1000,
                                subtitleText = sc.dialogue.ifBlank { sc.narration },
                                voiceoverText = sc.narration,
                                musicTrackName = musicStyle,
                                status = "pending"
                            )
                        )
                    }
                    repo.saveProject(project.copy(id = projectId, status = "storyboard_ready"))
                }

                // Create draft MovieItem so it appears in My Movies & Editor immediately
                repo.saveMovie(
                    MovieItem(
                        projectId = projectId,
                        title = project.title,
                        genre = project.genre,
                        durationSec = targetDurationSec,
                        resolution = resolution,
                        aspectRatio = aspectRatio,
                        status = "draft",
                        thumbnailData = refThumbBase64,
                        subtitlesText = blueprint?.fullScript?.take(6000) ?: description,
                        publicMovie = publicProject
                    )
                )

                // Queue Video Generation Job (only deduct credits if provider completes!)
                val sceneCount = blueprint?.scenes?.size ?: 1
                val jobId = repo.createGenerationJob(
                    projectId = projectId,
                    movieTitle = project.title,
                    jobType = jobType,
                    totalScenes = sceneCount,
                    totalClips = sceneCount,
                    estimatedCredits = estimatedCredits,
                    providerId = setting.providerName
                ).getOrNull()

                val message = when {
                    targetDurationSec > maxAllowedDuration -> {
                        if (jobId != null) {
                            repo.updateGenerationJobProgress(
                                jobId,
                                status = "paused",
                                completedClips = 0,
                                failedClips = 0,
                                creditsCharged = 0,
                                errorMessage = "Target duration (${SparkCatalog.formatDuration(targetDurationSec)}) exceeds ${plan?.planName ?: "Free"} limit (${SparkCatalog.formatDuration(maxAllowedDuration)}). Upgrade plan or trim scenes in Editor. 0 credits charged."
                            )
                        }
                        "Screenplay & storyboard created! Video rendering paused: upgrade plan for ${SparkCatalog.formatDuration(targetDurationSec)} movies (0 credits charged)."
                    }
                    currentCredits < estimatedCredits -> {
                        if (jobId != null) {
                            repo.updateGenerationJobProgress(
                                jobId,
                                status = "paused",
                                completedClips = 0,
                                failedClips = 0,
                                creditsCharged = 0,
                                errorMessage = "Insufficient credits (need $estimatedCredits, balance $currentCredits). 0 credits deducted."
                            )
                        }
                        "Screenplay & storyboard saved! Add credits to render full video (0 credits deducted)."
                    }
                    else -> {
                        val clipPrompt = blueprint?.scenes?.firstOrNull()?.description ?: description
                        val firstFrame = refThumbBase64.ifBlank { null }
                        when (
                            val res = videoAdapter.submitClipGeneration(
                                setting,
                                VideoClipRequest(
                                    projectId = projectId,
                                    clipId = "clip_1",
                                    prompt = clipPrompt,
                                    durationSec = targetDurationSec.coerceAtMost(30),
                                    aspectRatio = aspectRatio,
                                    resolution = resolution,
                                    idempotencyKey = "idem_$projectId",
                                    referenceImageBase64 = firstFrame
                                )
                            )
                        ) {
                            is VideoProviderResult.NotConfigured -> {
                                if (jobId != null) {
                                    repo.updateGenerationJobProgress(
                                        jobId, "paused", 0, 0, 0, res.setupGuide
                                    )
                                }
                                "Screenplay & ${sceneCount}-scene Storyboard generated with Gemini! Video rendering paused until external video provider is configured (0 credits charged)."
                            }
                            is VideoProviderResult.Completed -> {
                                if (jobId != null) {
                                    repo.updateGenerationJobProgress(jobId, "completed", sceneCount, 0, estimatedCredits, "")
                                }
                                repo.deductCreditsOnSuccess(estimatedCredits, "Rendered movie: ${project.title}")
                                "Movie \"${project.title}\" generated successfully!"
                            }
                            is VideoProviderResult.Processing -> {
                                if (jobId != null) {
                                    repo.updateGenerationJobProgress(jobId, "processing", 0, 0, 0, "Provider job ${res.providerJobId} running.")
                                }
                                "Movie job submitted to ${setting.providerName} (Job ${res.providerJobId})."
                            }
                            is VideoProviderResult.Failed -> {
                                if (jobId != null) {
                                    repo.updateGenerationJobProgress(jobId, "failed", 0, 1, 0, res.reason)
                                }
                                "Script & storyboard saved, video provider returned error: ${res.reason} (0 credits charged)."
                            }
                        }
                    }
                }

                _uiState.value = _uiState.value.copy(
                    isBusy = false,
                    busyLabel = "",
                    statusBanner = message
                )
                onProjectCreated(projectId)
            } finally {
                if (_uiState.value.isBusy) {
                    _uiState.value = _uiState.value.copy(isBusy = false, busyLabel = "")
                }
            }
        }
    }

    fun createSingingPhotoJob(
        title: String,
        portraitBitmap: Bitmap,
        audioSourceLabel: String,
        lyricsOrSpeech: String,
        mood: String,
        onDone: (String?) -> Unit = {}
    ) {
        if (title.isBlank()) {
            setErrorBanner("Enter a title for your Singing Photo project.")
            return
        }
        val setting = _uiState.value.adminSetting ?: AdminSetting()
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true, busyLabel = "Preparing Singing Photo lip-sync pipeline...")
            try {
                val compactPhoto = MediaUtils.bitmapToCompactBase64Async(portraitBitmap, 180_000)
                val projId = repo.saveProject(
                    MovieProject(
                        title = title.trim(),
                        description = "AI Singing Photo with $audioSourceLabel ($mood). Lyrics: ${lyricsOrSpeech.take(300)}",
                        genre = "Musical",
                        visualStyle = "Photorealistic Lip-Sync Portrait",
                        musicStyle = audioSourceLabel,
                        aspectRatio = "9:16",
                        resolution = "1080p",
                        targetDurationSec = 30,
                        estimatedCredits = 3,
                        status = "queued"
                    )
                ).getOrNull()

                if (projId == null) {
                    _uiState.value = _uiState.value.copy(isBusy = false, busyLabel = "", errorBanner = "Failed to save Singing Photo project.")
                    onDone(null)
                    return@launch
                }

                val jobId = repo.createGenerationJob(
                    projectId = projId,
                    movieTitle = title.trim(),
                    jobType = "singing_photo",
                    totalScenes = 1,
                    totalClips = 1,
                    estimatedCredits = 3,
                    providerId = setting.providerName
                ).getOrNull()

                val res = videoAdapter.submitLipSyncJob(
                    setting,
                    LipSyncRequest(
                        projectId = projId,
                        title = title.trim(),
                        photoBase64 = compactPhoto,
                        audioSourceLabel = audioSourceLabel,
                        lyricsOrSpeech = lyricsOrSpeech,
                        mood = mood,
                        idempotencyKey = "sing_${UUID.randomUUID().toString().take(12)}"
                    )
                )
                val msg = when (res) {
                    is VideoProviderResult.NotConfigured -> {
                        if (jobId != null) repo.updateGenerationJobProgress(jobId, "paused", 0, 0, 0, res.setupGuide)
                        res.setupGuide
                    }
                    is VideoProviderResult.Completed -> {
                        if (jobId != null) repo.updateGenerationJobProgress(jobId, "completed", 1, 0, 3, "")
                        repo.deductCreditsOnSuccess(3, "Singing Photo: ${title.trim()}")
                        repo.saveMovie(
                            MovieItem(
                                projectId = projId,
                                title = title.trim(),
                                genre = "Singing Photo",
                                durationSec = 30,
                                resolution = "1080p",
                                aspectRatio = "9:16",
                                status = "completed",
                                thumbnailData = compactPhoto,
                                videoUrl = res.videoUrl
                            )
                        )
                        "Singing Photo video generated!"
                    }
                    is VideoProviderResult.Processing -> {
                        if (jobId != null) repo.updateGenerationJobProgress(jobId, "processing", 0, 0, 0, "Processing lip-sync ${res.providerJobId}")
                        "Singing Photo job queued (${res.providerJobId})."
                    }
                    is VideoProviderResult.Failed -> {
                        if (jobId != null) repo.updateGenerationJobProgress(jobId, "failed", 0, 1, 0, res.reason)
                        "Lip-sync failed: ${res.reason} (0 credits charged)."
                    }
                }
                _uiState.value = _uiState.value.copy(isBusy = false, busyLabel = "", statusBanner = msg)
                onDone(projId)
            } finally {
                if (_uiState.value.isBusy) {
                    _uiState.value = _uiState.value.copy(isBusy = false, busyLabel = "")
                }
            }
        }
    }

    fun createMusicVideoJob(
        title: String,
        track: MusicTrack,
        visualConcept: String,
        visualStyle: String,
        aspectRatio: String,
        resolution: String,
        durationSec: Int,
        onDone: (String?) -> Unit = {}
    ) {
        if (title.isBlank()) {
            setErrorBanner("Enter a title for your Music Video.")
            return
        }
        val setting = _uiState.value.adminSetting ?: AdminSetting()
        val estCredits = SparkCatalog.estimateCredits(durationSec, resolution)
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true, busyLabel = "Creating beat-synced Music Video scenes with Gemini...")
            try {
                val project = MovieProject(
                    title = title.trim(),
                    description = "Music Video for track \"${track.title}\" (${track.category}, ${track.bpm} BPM). Visual concept: $visualConcept",
                    genre = "Musical",
                    visualStyle = visualStyle,
                    musicStyle = "${track.title} — ${track.artist}",
                    aspectRatio = aspectRatio,
                    resolution = resolution,
                    targetDurationSec = durationSec,
                    estimatedCredits = estCredits,
                    status = "scripting"
                )
                val projId = repo.saveProject(project).getOrNull()
                if (projId == null) {
                    _uiState.value = _uiState.value.copy(isBusy = false, busyLabel = "", errorBanner = "Failed to save Music Video project.")
                    onDone(null)
                    return@launch
                }
                val blueprint = aiService.generateMovieBlueprint(project.copy(id = projId)).getOrNull()
                blueprint?.scenes?.forEach { sc ->
                    repo.saveScene(
                        projId,
                        SceneItem(
                            projectId = projId,
                            sceneNumber = sc.sceneNumber,
                            title = sc.title,
                            location = sc.location,
                            description = sc.description,
                            cameraInstruction = sc.cameraInstruction,
                            shotDescription = sc.shotDescription,
                            durationSec = sc.durationSec,
                            status = "planned"
                        )
                    )
                }
                val jobId = repo.createGenerationJob(
                    projectId = projId,
                    movieTitle = title.trim(),
                    jobType = "music_video",
                    totalScenes = blueprint?.scenes?.size ?: 1,
                    totalClips = blueprint?.scenes?.size ?: 1,
                    estimatedCredits = estCredits,
                    providerId = setting.providerName
                ).getOrNull()

                val res = videoAdapter.submitMusicVideoJob(
                    setting,
                    MusicVideoRequest(
                        projectId = projId,
                        title = title.trim(),
                        trackTitle = track.title,
                        visualConcept = visualConcept,
                        style = visualStyle,
                        resolution = resolution,
                        aspectRatio = aspectRatio,
                        durationSec = durationSec,
                        idempotencyKey = "mv_${UUID.randomUUID().toString().take(12)}"
                    )
                )
                val msg = when (res) {
                    is VideoProviderResult.NotConfigured -> {
                        if (jobId != null) repo.updateGenerationJobProgress(jobId, "paused", 0, 0, 0, res.setupGuide)
                        res.setupGuide
                    }
                    is VideoProviderResult.Completed -> {
                        if (jobId != null) repo.updateGenerationJobProgress(jobId, "completed", 1, 0, estCredits, "")
                        repo.deductCreditsOnSuccess(estCredits, "Music Video: ${title.trim()}")
                        repo.saveMovie(
                            MovieItem(
                                projectId = projId,
                                title = title.trim(),
                                genre = "Music Video",
                                durationSec = durationSec,
                                resolution = resolution,
                                aspectRatio = aspectRatio,
                                status = "completed",
                                videoUrl = res.videoUrl
                            )
                        )
                        "Music Video rendered!"
                    }
                    is VideoProviderResult.Processing -> {
                        if (jobId != null) repo.updateGenerationJobProgress(jobId, "processing", 0, 0, 0, "Rendering ${res.providerJobId}")
                        "Music Video rendering queued (${res.providerJobId})."
                    }
                    is VideoProviderResult.Failed -> {
                        if (jobId != null) repo.updateGenerationJobProgress(jobId, "failed", 0, 1, 0, res.reason)
                        "Music Video rendering failed: ${res.reason} (0 credits charged)."
                    }
                }
                _uiState.value = _uiState.value.copy(isBusy = false, busyLabel = "", statusBanner = msg)
                onDone(projId)
            } finally {
                if (_uiState.value.isBusy) {
                    _uiState.value = _uiState.value.copy(isBusy = false, busyLabel = "")
                }
            }
        }
    }

    fun addUploadedAudioTrack(title: String, category: String, uri: Uri) {
        try {
            appContext.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {
        }
        val track = MusicTrack(
            id = "usr_${System.currentTimeMillis()}",
            title = title.ifBlank { "Uploaded Audio Track" },
            artist = _uiState.value.account?.username ?: "My Upload",
            category = category.ifBlank { "My Uploads" },
            bpm = 100,
            durationSec = 180,
            mood = "Custom User Audio",
            licenseType = "User-Supplied Rights",
            localUri = uri.toString()
        )
        _uiState.value = _uiState.value.copy(
            uploadedTracks = listOf(track) + _uiState.value.uploadedTracks,
            statusBanner = "Added \"${track.title}\" to your Music Library."
        )
        viewModelScope.launch {
            themePrefs.saveUploadedTrack(track)
        }
    }

    fun allMusicTracks(): List<MusicTrack> =
        _uiState.value.uploadedTracks + MusicLibraryCatalog.curatedTracks

    fun saveCharacter(character: CharacterProfile, portraitBitmap: Bitmap? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true, busyLabel = "Saving character...")
            try {
                val imgData = portraitBitmap?.let { MediaUtils.bitmapToCompactBase64Async(it, 200_000) }
                    ?: character.referenceImageData
                val res = repo.saveCharacter(character.copy(referenceImageData = imgData))
                _uiState.value = _uiState.value.copy(
                    statusBanner = if (res.isSuccess) "Character \"${character.name}\" saved." else null,
                    errorBanner = res.exceptionOrNull()?.localizedMessage
                )
            } finally {
                _uiState.value = _uiState.value.copy(isBusy = false, busyLabel = "")
            }
        }
    }

    fun generateAiCharacterPortrait(
        character: CharacterProfile,
        onGenerated: (Bitmap) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true, busyLabel = "Generating character portrait with Gemini Nano Banana...")
            try {
                val prompt = "Cinematic studio character concept portrait of ${character.name}, " +
                    "role: ${character.role}, age ${character.ageRange}, appearance: ${character.appearance}, " +
                    "clothing: ${character.clothing}, hairstyle: ${character.hairstyle}, " +
                    "expression: ${character.facialExpression}, dramatic champagne gold key lighting on dark obsidian background."
                val res = aiService.generateVisualAsset(prompt)
                _uiState.value = _uiState.value.copy(
                    isBusy = false,
                    busyLabel = "",
                    errorBanner = res.exceptionOrNull()?.localizedMessage
                )
                res.getOrNull()?.let(onGenerated)
            } finally {
                if (_uiState.value.isBusy) {
                    _uiState.value = _uiState.value.copy(isBusy = false, busyLabel = "")
                }
            }
        }
    }

    fun generateMovieConceptImage(
        title: String,
        description: String,
        genre: String,
        countryLocation: String,
        visualStyle: String,
        referenceBitmap: Bitmap? = null,
        onGenerated: (Bitmap) -> Unit
    ) {
        val cleanTitle = title.trim().ifBlank { "Untitled Spark AI Film" }
        val cleanDesc = description.trim().ifBlank { "$genre cinematic scene in $countryLocation" }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isBusy = true,
                busyLabel = "Generating AI poster/scene image with Gemini..."
            )
            try {
                val prompt = "Cinematic movie poster / keyframe concept art for \"$cleanTitle\" " +
                    "(Genre: $genre, Location: $countryLocation, Style: $visualStyle). " +
                    "Scene: $cleanDesc. Dramatic lighting, rich cinema composition, no watermarks."
                val res = aiService.generateVisualAsset(prompt, referenceBitmap)
                if (res.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        statusBanner = "AI concept image generated for \"$cleanTitle\"."
                    )
                    res.getOrNull()?.let(onGenerated)
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorBanner = res.exceptionOrNull()?.localizedMessage ?: "AI image generation failed."
                    )
                }
            } finally {
                _uiState.value = _uiState.value.copy(isBusy = false, busyLabel = "")
            }
        }
    }

    fun deleteCharacter(id: String) {
        viewModelScope.launch {
            repo.deleteCharacter(id)
        }
    }

    fun saveScript(script: ScriptDoc) {
        viewModelScope.launch {
            val res = repo.saveScript(script)
            _uiState.value = _uiState.value.copy(
                statusBanner = if (res.isSuccess) "Script \"${script.title}\" saved." else null,
                errorBanner = res.exceptionOrNull()?.localizedMessage
            )
        }
    }

    fun requestScriptSuggestion(script: ScriptDoc, instruction: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true, busyLabel = "Gemini AI is reviewing your script...")
            try {
                val res = aiService.suggestScriptImprovement(script.title, script.content, instruction)
                if (res.isSuccess) {
                    val updated = script.copy(aiSuggestion = res.getOrThrow())
                    repo.saveScript(updated)
                    _uiState.value = _uiState.value.copy(
                        statusBanner = "AI suggestions saved alongside your original script."
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorBanner = res.exceptionOrNull()?.localizedMessage
                    )
                }
            } finally {
                _uiState.value = _uiState.value.copy(isBusy = false, busyLabel = "")
            }
        }
    }

    fun attachLocalVideoToMovie(movie: MovieItem, localVideoUri: Uri) {
        viewModelScope.launch {
            val res = repo.saveMovie(
                movie.copy(
                    videoUrl = localVideoUri.toString(),
                    status = "completed"
                )
            )
            _uiState.value = _uiState.value.copy(
                statusBanner = if (res.isSuccess) "Attached local MP4 video to \"${movie.title}\"." else null,
                errorBanner = res.exceptionOrNull()?.localizedMessage
            )
        }
    }

    fun toggleMovieFavorite(movie: MovieItem) {
        viewModelScope.launch { repo.toggleMovieFavorite(movie) }
    }

    fun deleteMovie(movieId: String) {
        val movie = _uiState.value.movies.firstOrNull { it.id == movieId }
        viewModelScope.launch {
            val res = if (movie != null) {
                repo.softDeleteMovieToRecycleBin(movie)
            } else {
                repo.deleteMovie(movieId)
            }
            _uiState.value = _uiState.value.copy(
                statusBanner = if (res.isSuccess) "Moved movie to Recently Deleted (30-day recovery)." else null,
                errorBanner = res.exceptionOrNull()?.localizedMessage
            )
        }
    }

    fun batchSoftDeleteMovies(movies: List<MovieItem>) {
        if (movies.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true, busyLabel = "Moving ${movies.size} items to Recycle Bin...")
            try {
                val res = repo.batchSoftDeleteMoviesToRecycleBin(movies)
                val count = res.getOrDefault(0)
                _uiState.value = _uiState.value.copy(
                    statusBanner = "Moved $count items to Recently Deleted.",
                    errorBanner = res.exceptionOrNull()?.localizedMessage
                )
            } finally {
                _uiState.value = _uiState.value.copy(isBusy = false, busyLabel = "")
            }
        }
    }

    fun deleteProject(projectId: String) {
        val project = _uiState.value.projects.firstOrNull { it.id == projectId }
        viewModelScope.launch {
            val res = if (project != null) {
                repo.softDeleteProjectToRecycleBin(project)
            } else {
                repo.deleteProject(projectId)
            }
            _uiState.value = _uiState.value.copy(
                statusBanner = if (res.isSuccess) "Project moved to Recently Deleted." else null,
                errorBanner = res.exceptionOrNull()?.localizedMessage
            )
        }
    }

    fun restoreDeletedItem(item: RecentlyDeletedItem) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true, busyLabel = "Restoring ${item.title}...")
            try {
                val res = repo.restoreDeletedItem(item)
                _uiState.value = _uiState.value.copy(
                    statusBanner = if (res.isSuccess) "Restored \"${item.title}\"." else null,
                    errorBanner = res.exceptionOrNull()?.localizedMessage
                )
            } finally {
                _uiState.value = _uiState.value.copy(isBusy = false, busyLabel = "")
            }
        }
    }

    fun permanentlyDeleteTrashItem(itemId: String) {
        viewModelScope.launch {
            val res = repo.permanentlyDeleteFromRecycleBin(itemId)
            _uiState.value = _uiState.value.copy(
                statusBanner = if (res.isSuccess) "Permanently deleted from Recycle Bin." else null,
                errorBanner = res.exceptionOrNull()?.localizedMessage
            )
        }
    }

    fun purgeExpiredTrash() {
        val items = _uiState.value.recentlyDeleted
        viewModelScope.launch {
            val res = repo.purgeExpiredRecycleBin(items)
            val count = res.getOrDefault(0)
            _uiState.value = _uiState.value.copy(
                statusBanner = if (res.isSuccess) "Purged $count expired item(s) older than 30 days." else null,
                errorBanner = res.exceptionOrNull()?.localizedMessage
            )
        }
    }

    fun revokeSession(sessionId: String) {
        val uid = activeUid ?: return
        viewModelScope.launch {
            val res = securityRepo.revokeSession(uid, sessionId, "Revoked by user from Security Center")
            _uiState.value = _uiState.value.copy(
                statusBanner = if (res.isSuccess) "Device session revoked." else null,
                errorBanner = res.exceptionOrNull()?.localizedMessage
            )
        }
    }

    fun logoutAllOtherDevices() {
        val uid = activeUid ?: return
        viewModelScope.launch {
            val res = securityRepo.revokeAllOtherSessions(uid, deviceSessionId)
            val count = res.getOrDefault(0)
            _uiState.value = _uiState.value.copy(
                statusBanner = if (res.isSuccess) "Signed out $count other device session(s)." else null,
                errorBanner = res.exceptionOrNull()?.localizedMessage
            )
        }
    }

    fun reportSecurityIssue(title: String, description: String) {
        val uid = activeUid ?: return
        viewModelScope.launch {
            val res = securityRepo.logSecurityAlert(
                targetUserId = uid,
                username = _uiState.value.account?.username.orEmpty(),
                alertType = "user_report",
                severity = "high",
                title = title.ifBlank { "User Security Report" },
                description = description,
                deviceLabel = DeviceSessionHelper.getDeviceLabel()
            )
            _uiState.value = _uiState.value.copy(
                statusBanner = if (res.isSuccess) "Security issue reported to Founder Admin Security Center." else null,
                errorBanner = res.exceptionOrNull()?.localizedMessage
            )
        }
    }

    fun downloadMovieToDevice(movie: MovieItem) {
        if (movie.videoUrl.isBlank()) {
            setErrorBanner("This movie has no rendered MP4 stream yet. Connect a video renderer or attach a local MP4 in My Creations / Editor.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true, busyLabel = "Downloading MP4 to Movies/SparkAI...")
            try {
                val dl = MediaUtils.downloadMp4ToDevice(
                    appContext,
                    movie.videoUrl,
                    movie.title,
                    movie.resolution
                )
                if (dl.isSuccess) {
                    val savedUri = dl.getOrThrow()
                    repo.recordDownload(movie.id, movie.title, movie.resolution, savedUri)
                    _uiState.value = _uiState.value.copy(
                        statusBanner = "Saved MP4 to Movies/SparkAI gallery."
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorBanner = dl.exceptionOrNull()?.localizedMessage ?: "Download failed."
                    )
                }
            } finally {
                _uiState.value = _uiState.value.copy(isBusy = false, busyLabel = "")
            }
        }
    }

    fun markMovieViewed(movieId: String) {
        viewModelScope.launch { repo.markMovieViewed(movieId) }
    }

    fun markNotificationRead(notifId: String) {
        viewModelScope.launch { repo.markNotificationRead(notifId) }
    }

    fun markAllNotificationsRead() {
        val unread = _uiState.value.notifications.filter { !it.readFlag }.map { it.id }
        if (unread.isEmpty()) return
        viewModelScope.launch { repo.markAllNotificationsRead(unread) }
    }

    fun submitSupportTicket(subject: String, message: String) {
        viewModelScope.launch {
            val res = repo.submitSupportTicket(subject, message)
            _uiState.value = _uiState.value.copy(
                statusBanner = if (res.isSuccess) "Support ticket submitted to Christian Okafor's Spark AI team." else null,
                errorBanner = res.exceptionOrNull()?.localizedMessage
            )
        }
    }

    fun cancelSubscription() {
        viewModelScope.launch {
            val res = repo.cancelSubscription()
            _uiState.value = _uiState.value.copy(
                statusBanner = if (res.isSuccess) "Subscription updated to cancelled." else null,
                errorBanner = res.exceptionOrNull()?.localizedMessage
            )
        }
    }

    fun deleteAccount(onDeleted: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true, busyLabel = "Deleting account and studio records...")
            try {
                val res = repo.deleteAccountAndData()
                _uiState.value = _uiState.value.copy(
                    isBusy = false,
                    busyLabel = "",
                    errorBanner = res.exceptionOrNull()?.localizedMessage
                )
                if (res.isSuccess) onDeleted()
            } finally {
                if (_uiState.value.isBusy) {
                    _uiState.value = _uiState.value.copy(isBusy = false, busyLabel = "")
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceAudio.release()
    }
}
