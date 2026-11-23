package com.example.data.video

import com.example.data.ai.ConnectionCheckOutcome
import com.example.data.model.AdminSetting
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class VideoClipRequest(
    val projectId: String,
    val clipId: String,
    val prompt: String,
    val durationSec: Int,
    val aspectRatio: String,
    val resolution: String,
    val idempotencyKey: String,
    val referenceImageBase64: String? = null,
    val firstFrameBase64: String? = null,
    val lastFrameBase64: String? = null
)

data class LipSyncRequest(
    val projectId: String,
    val title: String,
    val photoBase64: String,
    val audioSourceLabel: String,
    val lyricsOrSpeech: String,
    val mood: String,
    val idempotencyKey: String
)

data class MusicVideoRequest(
    val projectId: String,
    val title: String,
    val trackTitle: String,
    val visualConcept: String,
    val style: String,
    val resolution: String,
    val aspectRatio: String,
    val durationSec: Int,
    val idempotencyKey: String
)

sealed interface VideoProviderResult {
    data class Completed(val providerJobId: String, val videoUrl: String) : VideoProviderResult
    data class Processing(val providerJobId: String) : VideoProviderResult
    data class NotConfigured(val setupGuide: String) : VideoProviderResult
    data class Failed(val reason: String) : VideoProviderResult
}

interface VideoProviderAdapter {
    suspend fun testConnection(setting: AdminSetting): ConnectionCheckOutcome
    suspend fun testStitcherConnection(setting: AdminSetting): ConnectionCheckOutcome
    suspend fun testStorageConnection(setting: AdminSetting): ConnectionCheckOutcome
    suspend fun submitClipGeneration(setting: AdminSetting, request: VideoClipRequest): VideoProviderResult
    suspend fun pollClipStatus(setting: AdminSetting, providerJobId: String): VideoProviderResult
    suspend fun submitMovieStitch(setting: AdminSetting, projectId: String, clipUrls: List<String>, title: String): VideoProviderResult
    suspend fun submitLipSyncJob(setting: AdminSetting, request: LipSyncRequest): VideoProviderResult
    suspend fun submitMusicVideoJob(setting: AdminSetting, request: MusicVideoRequest): VideoProviderResult
}

class HttpVideoProviderAdapter : VideoProviderAdapter {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    override suspend fun testConnection(setting: AdminSetting): ConnectionCheckOutcome =
        withContext(Dispatchers.IO) {
            val baseUrl = setting.providerBaseUrl.trim().trimEnd('/')
            if (!setting.providerConfigured || baseUrl.isBlank()) {
                return@withContext ConnectionCheckOutcome.Failed(
                    "Not configured: Set a server-side Video Provider Proxy URL (backed by VIDEO_PROVIDER_API_KEY) in Admin -> API Configuration."
                )
            }
            try {
                val req = Request.Builder()
                    .url("$baseUrl/health")
                    .get()
                    .build()
                client.newCall(req).execute().use { resp ->
                    when (resp.code) {
                        in 200..299 -> ConnectionCheckOutcome.Success("Provider ${setting.providerName} responded HTTP ${resp.code}.")
                        401, 403 -> ConnectionCheckOutcome.AuthError("Authentication error (HTTP ${resp.code}) from provider backend.")
                        429 -> ConnectionCheckOutcome.UsageLimit("Usage limit reached (HTTP 429) on provider backend.")
                        else -> ConnectionCheckOutcome.Failed("Provider responded HTTP ${resp.code}.")
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ConnectionCheckOutcome.Failed(e.localizedMessage ?: "Failed to reach provider endpoint.")
            }
        }

    override suspend fun testStitcherConnection(setting: AdminSetting): ConnectionCheckOutcome =
        withContext(Dispatchers.IO) {
            val baseUrl = setting.providerBaseUrl.trim().trimEnd('/')
            if (!setting.providerConfigured || baseUrl.isBlank()) {
                return@withContext ConnectionCheckOutcome.Failed(
                    "Not configured: Set your server-side FFmpeg / rendering service URL and VIDEO_STITCHER_SECRET in your backend proxy."
                )
            }
            try {
                val req = Request.Builder().url("$baseUrl/v1/stitch/health").get().build()
                client.newCall(req).execute().use { resp ->
                    when (resp.code) {
                        in 200..299 -> ConnectionCheckOutcome.Success("Rendering & stitching worker responded HTTP ${resp.code}.")
                        401, 403 -> ConnectionCheckOutcome.AuthError("Stitcher authentication failed (HTTP ${resp.code}).")
                        else -> ConnectionCheckOutcome.Failed("Stitcher endpoint returned HTTP ${resp.code}.")
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ConnectionCheckOutcome.Failed(e.localizedMessage ?: "Rendering & stitching service unreachable.")
            }
        }

    override suspend fun testStorageConnection(setting: AdminSetting): ConnectionCheckOutcome =
        withContext(Dispatchers.IO) {
            val baseUrl = setting.providerBaseUrl.trim().trimEnd('/')
            if (!setting.providerConfigured || baseUrl.isBlank()) {
                return@withContext ConnectionCheckOutcome.Failed(
                    "Not configured: Cloud Storage bucket & STORAGE_BUCKET_SIGNING_KEY are not provisioned. Using on-device MediaStore & compressed Firestore metadata for local assets."
                )
            }
            try {
                val req = Request.Builder().url("$baseUrl/v1/storage/health").get().build()
                client.newCall(req).execute().use { resp ->
                    when (resp.code) {
                        in 200..299 -> ConnectionCheckOutcome.Success("Cloud Storage bucket endpoint responded HTTP ${resp.code}.")
                        401, 403 -> ConnectionCheckOutcome.AuthError("Storage credentials rejected (HTTP ${resp.code}).")
                        else -> ConnectionCheckOutcome.Failed("Storage health endpoint returned HTTP ${resp.code}.")
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ConnectionCheckOutcome.Failed(e.localizedMessage ?: "Cloud storage service unreachable.")
            }
        }

    override suspend fun submitClipGeneration(
        setting: AdminSetting,
        request: VideoClipRequest
    ): VideoProviderResult = withContext(Dispatchers.IO) {
        val baseUrl = setting.providerBaseUrl.trim().trimEnd('/')
        if (!setting.providerConfigured || baseUrl.isBlank()) {
            return@withContext VideoProviderResult.NotConfigured(
                "External AI video generation provider is not configured. " +
                    "Gemini has generated your script & storyboard, and no credits were deducted. " +
                    "Configure your server-side video provider proxy URL in Founder Admin -> API Configuration, " +
                    "or attach a local MP4 clip in the Movie Editor."
            )
        }
        try {
            val payload = JSONObject().apply {
                put("projectId", request.projectId)
                put("clipId", request.clipId)
                put("prompt", request.prompt)
                put("durationSec", request.durationSec)
                put("aspectRatio", request.aspectRatio)
                put("resolution", request.resolution)
                put("idempotencyKey", request.idempotencyKey)
                if (!request.referenceImageBase64.isNullOrBlank()) {
                    put("referenceImageBase64", request.referenceImageBase64)
                }
                if (!request.firstFrameBase64.isNullOrBlank()) {
                    put("firstFrameBase64", request.firstFrameBase64)
                }
                if (!request.lastFrameBase64.isNullOrBlank()) {
                    put("lastFrameBase64", request.lastFrameBase64)
                }
            }
            val httpReq = Request.Builder()
                .url("$baseUrl/v1/generations")
                .addHeader("Idempotency-Key", request.idempotencyKey)
                .post(payload.toString().toRequestBody(jsonMedia))
                .build()

            client.newCall(httpReq).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return@withContext VideoProviderResult.Failed("Provider error HTTP ${resp.code}: ${body.take(200)}")
                }
                val json = JSONObject(body)
                val jobId = json.optString("jobId", request.clipId)
                val status = json.optString("status", "processing")
                val videoUrl = json.optString("videoUrl", "")
                if (status == "completed" && videoUrl.isNotBlank()) {
                    VideoProviderResult.Completed(jobId, videoUrl)
                } else {
                    VideoProviderResult.Processing(jobId)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            VideoProviderResult.Failed(e.localizedMessage ?: "Video generation network error.")
        }
    }

    override suspend fun pollClipStatus(
        setting: AdminSetting,
        providerJobId: String
    ): VideoProviderResult = withContext(Dispatchers.IO) {
        val baseUrl = setting.providerBaseUrl.trim().trimEnd('/')
        if (!setting.providerConfigured || baseUrl.isBlank()) {
            return@withContext VideoProviderResult.NotConfigured("Video provider is not configured.")
        }
        try {
            val req = Request.Builder().url("$baseUrl/v1/generations/$providerJobId").get().build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return@withContext VideoProviderResult.Failed("Poll HTTP ${resp.code}")
                }
                val json = JSONObject(body)
                val status = json.optString("status", "processing")
                val videoUrl = json.optString("videoUrl", "")
                when (status) {
                    "completed" -> if (videoUrl.isNotBlank()) VideoProviderResult.Completed(providerJobId, videoUrl)
                    else VideoProviderResult.Failed("Provider marked completed without videoUrl.")
                    "failed" -> VideoProviderResult.Failed(json.optString("error", "Provider generation failed."))
                    else -> VideoProviderResult.Processing(providerJobId)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            VideoProviderResult.Failed(e.localizedMessage ?: "Polling failed.")
        }
    }

    override suspend fun submitMovieStitch(
        setting: AdminSetting,
        projectId: String,
        clipUrls: List<String>,
        title: String
    ): VideoProviderResult = withContext(Dispatchers.IO) {
        val baseUrl = setting.providerBaseUrl.trim().trimEnd('/')
        if (!setting.providerConfigured || baseUrl.isBlank()) {
            return@withContext VideoProviderResult.NotConfigured(
                "Video rendering & stitching service is not configured. Configure a server-side stitching endpoint in Founder Admin -> API Configuration."
            )
        }
        if (clipUrls.isEmpty()) {
            return@withContext VideoProviderResult.Failed("No completed video clips available to stitch.")
        }
        try {
            val payload = JSONObject().apply {
                put("projectId", projectId)
                put("title", title)
                put("clipUrls", JSONArray(clipUrls))
            }
            val req = Request.Builder()
                .url("$baseUrl/v1/stitch")
                .post(payload.toString().toRequestBody(jsonMedia))
                .build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return@withContext VideoProviderResult.Failed("Stitch error HTTP ${resp.code}")
                }
                val json = JSONObject(body)
                val videoUrl = json.optString("videoUrl", "")
                if (videoUrl.isNotBlank()) {
                    VideoProviderResult.Completed(json.optString("jobId", projectId), videoUrl)
                } else {
                    VideoProviderResult.Processing(json.optString("jobId", projectId))
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            VideoProviderResult.Failed(e.localizedMessage ?: "Stitching request failed.")
        }
    }

    override suspend fun submitLipSyncJob(
        setting: AdminSetting,
        request: LipSyncRequest
    ): VideoProviderResult = withContext(Dispatchers.IO) {
        val baseUrl = setting.providerBaseUrl.trim().trimEnd('/')
        if (!setting.providerConfigured || baseUrl.isBlank()) {
            return@withContext VideoProviderResult.NotConfigured(
                "AI lip-sync & singing video renderer is not configured. " +
                    "We never fake a singing video from a static image. No credits were deducted. " +
                    "Configure your server-side lip-sync / video proxy in Founder Admin -> API Configuration."
            )
        }
        try {
            val payload = JSONObject().apply {
                put("projectId", request.projectId)
                put("title", request.title)
                put("photoBase64", request.photoBase64)
                put("audioSourceLabel", request.audioSourceLabel)
                put("lyricsOrSpeech", request.lyricsOrSpeech)
                put("mood", request.mood)
                put("idempotencyKey", request.idempotencyKey)
            }
            val req = Request.Builder()
                .url("$baseUrl/v1/lip-sync")
                .addHeader("Idempotency-Key", request.idempotencyKey)
                .post(payload.toString().toRequestBody(jsonMedia))
                .build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return@withContext VideoProviderResult.Failed("Lip-sync provider HTTP ${resp.code}: ${body.take(180)}")
                }
                val json = JSONObject(body)
                val videoUrl = json.optString("videoUrl", "")
                val jobId = json.optString("jobId", request.projectId)
                if (videoUrl.isNotBlank()) VideoProviderResult.Completed(jobId, videoUrl)
                else VideoProviderResult.Processing(jobId)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            VideoProviderResult.Failed(e.localizedMessage ?: "Lip-sync generation failed.")
        }
    }

    override suspend fun submitMusicVideoJob(
        setting: AdminSetting,
        request: MusicVideoRequest
    ): VideoProviderResult = withContext(Dispatchers.IO) {
        val baseUrl = setting.providerBaseUrl.trim().trimEnd('/')
        if (!setting.providerConfigured || baseUrl.isBlank()) {
            return@withContext VideoProviderResult.NotConfigured(
                "External AI music video renderer is not configured. " +
                    "Your beat-synced Gemini visual treatment & scenes are saved, and no credits were deducted. " +
                    "Configure your server-side video provider proxy in Founder Admin -> API Configuration."
            )
        }
        try {
            val payload = JSONObject().apply {
                put("projectId", request.projectId)
                put("title", request.title)
                put("trackTitle", request.trackTitle)
                put("visualConcept", request.visualConcept)
                put("style", request.style)
                put("resolution", request.resolution)
                put("aspectRatio", request.aspectRatio)
                put("durationSec", request.durationSec)
                put("idempotencyKey", request.idempotencyKey)
            }
            val req = Request.Builder()
                .url("$baseUrl/v1/music-video")
                .addHeader("Idempotency-Key", request.idempotencyKey)
                .post(payload.toString().toRequestBody(jsonMedia))
                .build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return@withContext VideoProviderResult.Failed("Music video provider HTTP ${resp.code}: ${body.take(180)}")
                }
                val json = JSONObject(body)
                val videoUrl = json.optString("videoUrl", "")
                val jobId = json.optString("jobId", request.projectId)
                if (videoUrl.isNotBlank()) VideoProviderResult.Completed(jobId, videoUrl)
                else VideoProviderResult.Processing(jobId)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            VideoProviderResult.Failed(e.localizedMessage ?: "Music video generation failed.")
        }
    }
}
