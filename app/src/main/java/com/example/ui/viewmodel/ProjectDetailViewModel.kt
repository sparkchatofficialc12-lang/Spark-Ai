package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.MediaUtils
import com.example.data.ai.SparkAiService
import com.example.data.model.AdminSetting
import com.example.data.model.ClipItem
import com.example.data.model.GenerationJob
import com.example.data.model.MovieItem
import com.example.data.model.MovieProject
import com.example.data.model.SceneItem
import com.example.data.repository.SparkRepository
import com.example.data.video.HttpVideoProviderAdapter
import com.example.data.video.VideoProviderResult
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class ProjectDetailUiState(
    val projectId: String = "",
    val scenes: List<SceneItem> = emptyList(),
    val clips: List<ClipItem> = emptyList(),
    val isBusy: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

class ProjectDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = SparkRepository(application.applicationContext)
    private val aiService = SparkAiService()
    private val videoAdapter = HttpVideoProviderAdapter()

    private val _state = MutableStateFlow(ProjectDetailUiState())
    val state: StateFlow<ProjectDetailUiState> = _state.asStateFlow()

    private var observerJob: Job? = null

    fun observeProject(projectId: String) {
        val uid = Firebase.auth.currentUser?.uid ?: return
        if (_state.value.projectId == projectId && observerJob?.isActive == true) return
        observerJob?.cancel()
        _state.value = ProjectDetailUiState(projectId = projectId)
        observerJob = viewModelScope.launch {
            launch {
                repo.observeScenes(uid, projectId).catch {}.collect { scenes ->
                    _state.value = _state.value.copy(scenes = scenes)
                }
            }
            launch {
                repo.observeClips(uid, projectId).catch {}.collect { clips ->
                    _state.value = _state.value.copy(clips = clips)
                }
            }
        }
    }

    fun clearMessages() {
        _state.value = _state.value.copy(statusMessage = null, errorMessage = null)
    }

    fun saveScene(projectId: String, scene: SceneItem) {
        viewModelScope.launch {
            val res = repo.saveScene(projectId, scene)
            _state.value = _state.value.copy(
                statusMessage = if (res.isSuccess) "Scene \"${scene.title}\" updated." else null,
                errorMessage = res.exceptionOrNull()?.localizedMessage
            )
        }
    }

    fun deleteScene(projectId: String, sceneId: String) {
        viewModelScope.launch { repo.deleteScene(projectId, sceneId) }
    }

    fun generateStoryboardFrame(project: MovieProject, scene: SceneItem) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isBusy = true, statusMessage = null, errorMessage = null)
            try {
                val prompt = "Film storyboard frame for movie '${project.title}' (${project.visualStyle}). " +
                    "Scene ${scene.sceneNumber}: ${scene.title} at ${scene.location}. " +
                    "Action: ${scene.description}. Camera: ${scene.cameraInstruction}. " +
                    "Lighting: ${project.lighting}. Character: ${project.mainCharacterSummary}."
                val res = aiService.generateVisualAsset(prompt)
                if (res.isSuccess) {
                    val bmp: Bitmap = res.getOrThrow()
                    val encoded = MediaUtils.bitmapToCompactBase64Async(bmp, 180_000)
                    repo.saveScene(project.id, scene.copy(storyboardData = encoded, status = "storyboarded"))
                    _state.value = _state.value.copy(
                        statusMessage = "Storyboard frame generated for Scene ${scene.sceneNumber}."
                    )
                } else {
                    _state.value = _state.value.copy(
                        errorMessage = res.exceptionOrNull()?.localizedMessage ?: "Storyboard generation failed."
                    )
                }
            } finally {
                _state.value = _state.value.copy(isBusy = false)
            }
        }
    }

    fun saveClip(projectId: String, clip: ClipItem) {
        viewModelScope.launch {
            val res = repo.saveClip(projectId, clip)
            _state.value = _state.value.copy(
                statusMessage = if (res.isSuccess) "Timeline clip updated." else null,
                errorMessage = res.exceptionOrNull()?.localizedMessage
            )
        }
    }

    fun splitClip(projectId: String, clip: ClipItem) {
        viewModelScope.launch {
            val totalMs = (clip.trimEndMs - clip.trimStartMs).coerceAtLeast(2000)
            val midMs = clip.trimStartMs + (totalMs / 2)
            val halfSec = ((midMs - clip.trimStartMs) / 1000).coerceAtLeast(1)
            repo.saveClip(
                projectId,
                clip.copy(
                    title = "${clip.title} (Part A)",
                    trimEndMs = midMs,
                    durationSec = halfSec
                )
            )
            repo.saveClip(
                projectId,
                clip.copy(
                    id = "",
                    clipOrder = clip.clipOrder + 1,
                    title = "${clip.title} (Part B)",
                    trimStartMs = midMs,
                    durationSec = halfSec
                )
            )
            _state.value = _state.value.copy(statusMessage = "Split \"${clip.title}\" into two timeline segments.")
        }
    }

    fun moveClipOrder(projectId: String, clip: ClipItem, moveUp: Boolean) {
        val current = _state.value.clips
        val idx = current.indexOfFirst { it.id == clip.id }
        if (idx < 0) return
        val swapIdx = if (moveUp) idx - 1 else idx + 1
        if (swapIdx !in current.indices) return
        val other = current[swapIdx]
        viewModelScope.launch {
            repo.saveClip(projectId, clip.copy(clipOrder = other.clipOrder))
            repo.saveClip(projectId, other.copy(clipOrder = clip.clipOrder))
        }
    }

    fun deleteClip(projectId: String, clipId: String) {
        viewModelScope.launch { repo.deleteClip(projectId, clipId) }
    }

    fun exportStitchedMovie(
        project: MovieProject,
        setting: AdminSetting?,
        existingMovie: MovieItem?
    ) {
        val clipUrls = _state.value.clips.mapNotNull { it.videoUrl.takeIf { url -> url.isNotBlank() } }
        viewModelScope.launch {
            _state.value = _state.value.copy(isBusy = true, statusMessage = null, errorMessage = null)
            try {
                val res = videoAdapter.submitMovieStitch(
                    setting ?: AdminSetting(),
                    project.id,
                    clipUrls,
                    project.title
                )
                when (res) {
                    is VideoProviderResult.NotConfigured -> {
                        _state.value = _state.value.copy(errorMessage = res.setupGuide)
                    }
                    is VideoProviderResult.Completed -> {
                        val mergedSubtitles = _state.value.clips
                            .mapNotNull { it.subtitleText.takeIf { s -> s.isNotBlank() } }
                            .joinToString("\n")
                        repo.saveMovie(
                            (existingMovie ?: MovieItem(projectId = project.id, title = project.title, genre = project.genre))
                                .copy(
                                    videoUrl = res.videoUrl,
                                    durationSec = _state.value.clips.sumOf { it.durationSec }.coerceAtLeast(project.targetDurationSec),
                                    resolution = project.resolution,
                                    aspectRatio = project.aspectRatio,
                                    subtitlesText = mergedSubtitles,
                                    status = "completed"
                                )
                        )
                        _state.value = _state.value.copy(statusMessage = "Movie stitched and saved to My Creations.")
                    }
                    is VideoProviderResult.Processing -> {
                        _state.value = _state.value.copy(statusMessage = "Stitching job queued (${res.providerJobId}).")
                    }
                    is VideoProviderResult.Failed -> {
                        _state.value = _state.value.copy(errorMessage = res.reason)
                    }
                }
            } finally {
                _state.value = _state.value.copy(isBusy = false)
            }
        }
    }

    fun updateJobStatus(job: GenerationJob, newStatus: String) {
        viewModelScope.launch {
            repo.updateGenerationJobProgress(
                jobId = job.id,
                status = newStatus,
                completedClips = job.completedClips,
                failedClips = job.failedClips,
                creditsCharged = job.creditsCharged,
                errorMessage = if (newStatus == "cancelled") "Cancelled by creator. No extra credits deducted." else job.errorMessage
            )
        }
    }
}
