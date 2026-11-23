package com.example.data.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.data.model.MovieProject
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class GeneratedScenePlan(
    val sceneNumber: Int,
    val title: String,
    val location: String,
    val description: String,
    val cameraInstruction: String,
    val shotDescription: String,
    val narration: String,
    val dialogue: String,
    val soundEffects: String,
    val durationSec: Int
)

data class GeneratedMovieBlueprint(
    val synopsis: String,
    val characterSheet: String,
    val fullScript: String,
    val scenes: List<GeneratedScenePlan>
)

sealed interface ConnectionCheckOutcome {
    data class Success(val message: String) : ConnectionCheckOutcome
    data class AuthError(val message: String) : ConnectionCheckOutcome
    data class UsageLimit(val message: String) : ConnectionCheckOutcome
    data class Failed(val message: String) : ConnectionCheckOutcome
}

class SparkAiService {
    private val blueprintSchema = Schema.obj(
        properties = mapOf(
            "synopsis" to Schema.string(description = "A compelling cinematic logline and synopsis"),
            "characterSheet" to Schema.string(description = "Consistent character appearance, wardrobe, and continuity rules"),
            "fullScript" to Schema.string(description = "Formatted screenplay text covering all scenes"),
            "scenes" to Schema.array(
                description = "Ordered scenes dividing the movie across its target duration",
                items = Schema.obj(
                    properties = mapOf(
                        "sceneNumber" to Schema.integer(),
                        "title" to Schema.string(),
                        "location" to Schema.string(),
                        "description" to Schema.string(),
                        "cameraInstruction" to Schema.string(),
                        "shotDescription" to Schema.string(),
                        "narration" to Schema.string(),
                        "dialogue" to Schema.string(),
                        "soundEffects" to Schema.string(),
                        "durationSec" to Schema.integer()
                    )
                )
            )
        )
    )

    private val scriptPlannerModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = blueprintSchema
            }
        )
    }

    private val textAssistantModel by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    private val imageGenModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-nano-banana-2.1",
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
            }
        )
    }

    suspend fun testGeminiConnection(): ConnectionCheckOutcome {
        return try {
            val response = withTimeout(30_000L) {
                textAssistantModel.generateContent("Reply with the single word: OK")
            }
            if (!response.text.isNullOrBlank()) {
                ConnectionCheckOutcome.Success("Gemini 3.8 Flash connected via Firebase AI Logic + App Check.")
            } else {
                ConnectionCheckOutcome.Failed("Gemini returned an empty response.")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: QuotaExceededException) {
            ConnectionCheckOutcome.UsageLimit(e.localizedMessage ?: "Gemini usage limit reached.")
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: "Gemini connection failed"
            if (msg.contains("403") || msg.contains("401") || msg.contains("AppCheck", ignoreCase = true)) {
                ConnectionCheckOutcome.AuthError(msg)
            } else {
                ConnectionCheckOutcome.Failed(msg)
            }
        }
    }

    suspend fun generateMovieBlueprint(project: MovieProject): Result<GeneratedMovieBlueprint> {
        return try {
            val desiredScenes = when {
                project.targetDurationSec <= 30 -> 2
                project.targetDurationSec <= 180 -> 5
                project.targetDurationSec <= 600 -> 8
                project.targetDurationSec <= 1800 -> 12
                else -> 16
            }
            val perSceneSec = (project.targetDurationSec / desiredScenes).coerceAtLeast(10)

            val prompt = buildString {
                appendLine("You are Spark AI's senior film director and screenwriter.")
                appendLine("Create a complete, production-ready movie blueprint for:")
                appendLine("Title: ${project.title}")
                appendLine("Premise: ${project.description}")
                val chosenLang = project.language.ifBlank { "English" }
                appendLine("Genre: ${project.genre} | Location: ${project.countryLocation} | Primary Language: $chosenLang")
                appendLine("Target Duration: ${project.targetDurationSec} seconds (~$desiredScenes major scenes, ~$perSceneSec s each).")
                appendLine("Main Character Summary: ${project.mainCharacterSummary.ifBlank { "Lead protagonist tailored to story" }}")
                appendLine("Appearance: ${project.characterAppearance} | Clothing: ${project.characterClothing} | Age: ${project.characterAgeRange}")
                appendLine("Camera: ${project.cameraAngle} | Lighting: ${project.lighting} | Atmosphere: ${project.atmosphere}")
                appendLine("Background: ${project.backgroundSetting} | Visual Style: ${project.visualStyle}")
                appendLine("Voice: ${project.voiceStyle} | Music/SFX: ${project.musicStyle}")
                appendLine("LANGUAGE REQUIREMENT: Write ALL spoken dialogue, narration, voiceover lines, and scene subtitles authentically in $chosenLang (if Nigerian Pidgin, use natural authentic Nigerian Pidgin expressions and cadence; if any other language, write natural fluent lines in that language). Keep JSON field keys in English.")
                appendLine("Ensure strict character wardrobe, appearance, and story continuity across ALL $desiredScenes scenes.")
            }

            val response = withTimeout(60_000L) {
                scriptPlannerModel.generateContent(prompt)
            }
            val jsonText = response.text ?: return Result.failure(IllegalStateException("Gemini returned empty output."))
            val json = JSONObject(jsonText)
            val scenesArr = json.getJSONArray("scenes")
            val scenes = mutableListOf<GeneratedScenePlan>()
            for (i in 0 until scenesArr.length()) {
                val obj = scenesArr.getJSONObject(i)
                scenes.add(
                    GeneratedScenePlan(
                        sceneNumber = obj.optInt("sceneNumber", i + 1),
                        title = obj.optString("title", "Scene ${i + 1}"),
                        location = obj.optString("location", project.countryLocation),
                        description = obj.optString("description", ""),
                        cameraInstruction = obj.optString("cameraInstruction", project.cameraAngle),
                        shotDescription = obj.optString("shotDescription", ""),
                        narration = obj.optString("narration", ""),
                        dialogue = obj.optString("dialogue", ""),
                        soundEffects = obj.optString("soundEffects", ""),
                        durationSec = obj.optInt("durationSec", perSceneSec).coerceAtLeast(5)
                    )
                )
            }
            Result.success(
                GeneratedMovieBlueprint(
                    synopsis = json.optString("synopsis", ""),
                    characterSheet = json.optString("characterSheet", ""),
                    fullScript = json.optString("fullScript", ""),
                    scenes = scenes
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Movie blueprint generation timed out", e)
            Result.failure(IllegalStateException("AI script planning timed out after 60s. Please retry."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Movie blueprint generation failed", e)
            Result.failure(e)
        }
    }

    suspend fun suggestScriptImprovement(
        originalTitle: String,
        originalScript: String,
        instruction: String
    ): Result<String> {
        return try {
            val prompt = """
                As an award-winning screenwriter, provide non-destructive script improvement suggestions and polished dialogue enhancements for "$originalTitle".
                Goal: $instruction
                Return clear scene-by-scene suggestions the writer can review without replacing their original draft.
                
                ORIGINAL SCRIPT:
                ${originalScript.take(8000)}
            """.trimIndent()
            val response = withTimeout(60_000L) {
                textAssistantModel.generateContent(prompt)
            }
            val text = response.text
            if (text.isNullOrBlank()) {
                Result.failure(IllegalStateException("AI returned an empty suggestion."))
            } else {
                Result.success(text)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Script improvement failed", e)
            Result.failure(e)
        }
    }

    suspend fun generateVisualAsset(promptText: String, referenceBitmap: Bitmap? = null): Result<Bitmap> {
        return try {
            val response = withTimeout(60_000L) {
                if (referenceBitmap != null) {
                    imageGenModel.generateContent(
                        content {
                            image(referenceBitmap)
                            text(promptText)
                        }
                    )
                } else {
                    imageGenModel.generateContent(promptText)
                }
            }
            val imagePart = response.candidates.firstOrNull()
                ?.content?.parts?.filterIsInstance<ImagePart>()
                ?.firstOrNull()
            val bmp = imagePart?.image
            if (bmp != null) {
                Result.success(bmp)
            } else {
                Result.failure(IllegalStateException("Image model did not return an image. Ensure Firebase Blaze billing is active for image generation."))
            }
        } catch (e: TimeoutCancellationException) {
            Result.failure(IllegalStateException("Visual generation timed out after 60 seconds."))
        } catch (e: QuotaExceededException) {
            Result.failure(IllegalStateException("Image generation quota exceeded. Please try again later."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Visual asset generation failed", e)
            Result.failure(e)
        }
    }

    fun streamSparkGuideReply(
        userQuestion: String,
        historyTurns: List<Pair<String, String>>,
        language: String,
        contextSection: String,
        kbArticlesContext: String
    ): Flow<String> = flow {
        val systemText = buildString {
            appendLine("You are SPARK GUIDE, the official built-in personal AI assistant for Spark AI and Spark Social (founded by Christian Okafor / Sparkling, tagline 'Imagine It. Create It. Watch It.').")
            appendLine("Respond in simple, friendly, beginner-clear language using numbered step-by-step instructions whenever explaining how to do something.")
            appendLine("Preferred response language: $language. Current app section: $contextSection.")
            appendLine("Only reference real screens and buttons in Spark AI:")
            appendLine("- Home tab (Spark Social Home): For You / Following feeds, 24-hour Stories row, Search, Live tab, Short Reels, Create Movie, POST button.")
            appendLine("- Discover tab: Trending posts, Sounds discovery, Live streams, Community Templates, Saved Posts.")
            appendLine("- Create (+) tab: Universal Post composer (Text, Photo, Photo Album, Short Video, Long Video, Full Movie, Music Video, Singing Video, AI Animation, Template) with Android Photo Picker, privacy, comments toggle, template permission.")
            appendLine("- Home Dashboard / Studio modules: Create Movie (Text/Image/Script to Movie), AI Singing Photo, Music Library (upload audio, curated Afrobeats/Nigerian/Gospel tracks, Make Video), Music Video Creator, Character Studio, Script & Storyboard, AI Voice Studio, My Movies / My Creations (with Multi-Select batch delete & Recently Deleted Recycle Bin), Movie Editor, Render Jobs, Creator Analytics, Subscription & Credits, Settings, Admin Console (Founder Admin only).")
            appendLine("- Three-dot menu (⋮) on any owned post: Edit Post, Replace Video (keep original post link OR publish as new post), Delete Post (moves to 30-day Recently Deleted Recycle Bin), Change Privacy, Disable/Enable Comments, Manage Template, Save, Download, Share.")
            appendLine("- Settings -> Security & Devices: Google Sign-In status, App Lock toggle, Active Sessions & Connected Devices (Revoke / Log out all other devices), Login Activity Logs, Security Alerts, Report Security Issue, Delete Account.")
            appendLine("- Note honestly: Full long-video external rendering and live streaming depend on backend providers configured in Admin API Config; text script planning, storyboards, character sheets, voice TTS previews, and Spark Guide work live right now.")
            if (kbArticlesContext.isNotBlank()) {
                appendLine("Additional Founder Help Articles:")
                appendLine(kbArticlesContext.take(2500))
            }
        }

        val guideModel = Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content { text(systemText) }
        )

        val chatHistory: List<Content> = historyTurns.takeLast(12).map { (role, text) ->
            content(role = if (role == "assistant" || role == "model") "model" else "user") {
                text(text.take(2000))
            }
        }

        val chat = guideModel.startChat(history = chatHistory)
        withTimeout(45_000L) {
            chat.sendMessageStream(userQuestion).collect { chunk ->
                val piece = chunk.text
                if (!piece.isNullOrEmpty()) {
                    emit(piece)
                }
            }
        }
    }

    companion object {
        private const val TAG = "SparkAiService"
    }
}
