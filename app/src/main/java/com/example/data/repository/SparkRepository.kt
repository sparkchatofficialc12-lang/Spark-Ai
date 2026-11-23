package com.example.data.repository

import android.content.Context
import android.os.Build
import com.example.R
import com.example.core.AuthManager
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.model.AdminSetting
import com.example.data.model.CharacterProfile
import com.example.data.model.ClipItem
import com.example.data.model.CreditBalance
import com.example.data.model.DownloadRecord
import com.example.data.model.GenerationJob
import com.example.data.model.MovieItem
import com.example.data.model.MovieProject
import com.example.data.model.NotificationItem
import com.example.data.model.SceneItem
import com.example.data.model.ScriptDoc
import com.example.data.model.SecurityLogItem
import com.example.data.model.SubscriptionPlan
import com.example.data.model.SupportTicket
import com.example.data.model.TransactionRecord
import com.example.data.model.UserAccount
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class SparkRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Spark AI.")
    }

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    private inline fun <reified T> observeSubcollection(
        uid: String,
        subcollection: String,
        orderByField: String? = "updatedAt"
    ): Flow<List<T>> = flow {
        val currentUid = requireUserId()
        check(currentUid == uid) { "Session mismatch" }
        val baseRef = userDoc(uid).collection(subcollection)
        val query: Query = if (orderByField != null) {
            baseRef.orderBy(orderByField, Query.Direction.DESCENDING)
        } else {
            baseRef
        }
        val path = "users/$uid/$subcollection"
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(T::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    private fun logStreamErrorIfActive(
        error: Throwable,
        op: OperationType,
        path: String,
        uid: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, op, path)
        }
    }

    suspend fun bootstrapUserIfNeeded(user: FirebaseUser): Result<Unit> {
        val uid = user.uid
        val path = "users/$uid"
        return try {
            val accountSnap = userDoc(uid).get().await()
            val email = user.email.orEmpty().ifBlank { "creator@sparkai.app" }
            val fullName = user.displayName.orEmpty().ifBlank { email.substringBefore("@").ifBlank { "Spark Creator" } }
            val baseUsername = fullName.lowercase().replace(Regex("[^a-z0-9_]"), "_").take(24).ifBlank { "creator_${uid.take(6)}" }
            val isFounder = AuthManager.isFounderAdmin(user)

            if (!accountSnap.exists()) {
                val accountPayload = mapOf(
                    "userId" to uid,
                    "email" to email,
                    "fullName" to fullName,
                    "username" to baseUsername,
                    "country" to "Nigeria",
                    "phoneNumber" to user.phoneNumber.orEmpty(),
                    "photoUrl" to (user.photoUrl?.toString() ?: ""),
                    "role" to if (isFounder) "admin" else "creator",
                    "accountStatus" to "active",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                userDoc(uid).set(accountPayload).await()
            }

            val profileSnap = db.collection("userProfiles").document(uid).get().await()
            if (!profileSnap.exists()) {
                db.collection("userProfiles").document(uid).set(
                    mapOf(
                        "userId" to uid,
                        "bio" to "Creating cinematic AI stories on Spark AI.",
                        "darkMode" to true,
                        "appLanguage" to "English",
                        "notificationsEnabled" to true,
                        "soundEnabled" to true,
                        "videoAutoplay" to true,
                        "defaultQuality" to "1080p",
                        "defaultAspectRatio" to "16:9",
                        "defaultResolution" to "1080p",
                        "defaultDurationSec" to 30,
                        "defaultMovieLanguage" to "English",
                        "defaultVoice" to "Nigerian English — Warm Female",
                        "defaultGenre" to "Action",
                        "autoSubtitles" to true,
                        "autoSaveProjects" to true,
                        "defaultPrivacy" to "private",
                        "allowDownloads" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            val subSnap = userDoc(uid).collection("subscriptions").document("current").get().await()
            if (!subSnap.exists()) {
                userDoc(uid).collection("subscriptions").document("current").set(
                    mapOf(
                        "userId" to uid,
                        "planId" to "free",
                        "planName" to "Free Creator",
                        "status" to "active",
                        "monthlyCredits" to 20,
                        "maxDurationSec" to 60,
                        "maxResolution" to "720p",
                        "maxSimultaneousProjects" to 3,
                        "storageQuotaMb" to 250,
                        "priceUsd" to 0.0,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            val creditSnap = userDoc(uid).collection("credits").document("balance").get().await()
            if (!creditSnap.exists()) {
                userDoc(uid).collection("credits").document("balance").set(
                    mapOf(
                        "userId" to uid,
                        "balance" to 20,
                        "promoCredits" to 0,
                        "lifetimeEarned" to 20,
                        "lifetimeSpent" to 0,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                userDoc(uid).collection("transactions").document().set(
                    mapOf(
                        "userId" to uid,
                        "type" to "welcome_bonus",
                        "amountCredits" to 20,
                        "amountUsd" to 0.0,
                        "description" to "Welcome Free Creator starter credits",
                        "status" to "completed",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                userDoc(uid).collection("notifications").document().set(
                    mapOf(
                        "userId" to uid,
                        "title" to "Welcome to Spark AI",
                        "message" to "Imagine It. Create It. Watch It. You received 20 starter credits and unlimited Gemini script & storyboard planning.",
                        "category" to "welcome",
                        "readFlag" to false,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            // Record login security event
            val deviceLabel = "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})"
            userDoc(uid).collection("securityLogs").document().set(
                mapOf(
                    "userId" to uid,
                    "event" to "Google Sign-In session verified",
                    "deviceInfo" to deviceLabel.take(180),
                    "status" to "verified",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeAccount(uid: String): Flow<UserAccount?> =
        userDoc(uid).snapshots()
            .map { it.toObject(UserAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                logStreamErrorIfActive(error, OperationType.GET, "users/$uid", uid)
                throw error
            }

    fun observeProfile(uid: String): Flow<UserProfile?> =
        db.collection("userProfiles").document(uid).snapshots()
            .map { it.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                logStreamErrorIfActive(error, OperationType.GET, "userProfiles/$uid", uid)
                throw error
            }

    fun observeCredits(uid: String): Flow<CreditBalance?> =
        userDoc(uid).collection("credits").document("balance").snapshots()
            .map { it.toObject(CreditBalance::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                logStreamErrorIfActive(error, OperationType.GET, "users/$uid/credits/balance", uid)
                throw error
            }

    fun observeSubscription(uid: String): Flow<SubscriptionPlan?> =
        userDoc(uid).collection("subscriptions").document("current").snapshots()
            .map { it.toObject(SubscriptionPlan::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                logStreamErrorIfActive(error, OperationType.GET, "users/$uid/subscriptions/current", uid)
                throw error
            }

    fun observeAdminProviderSettings(): Flow<AdminSetting?> =
        db.collection("adminSettings").document("providerConfig").snapshots()
            .map { it.toObject(AdminSetting::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                emit(null)
            }

    fun observeProjects(uid: String): Flow<List<MovieProject>> = observeSubcollection(uid, "projects")
    fun observeMovies(uid: String): Flow<List<MovieItem>> = observeSubcollection(uid, "movies")
    fun observeJobs(uid: String): Flow<List<GenerationJob>> = observeSubcollection(uid, "generationJobs")
    fun observeCharacters(uid: String): Flow<List<CharacterProfile>> = observeSubcollection(uid, "characters")
    fun observeScripts(uid: String): Flow<List<ScriptDoc>> = observeSubcollection(uid, "scripts")
    fun observeTransactions(uid: String): Flow<List<TransactionRecord>> = observeSubcollection(uid, "transactions", "createdAt")
    fun observeDownloads(uid: String): Flow<List<DownloadRecord>> = observeSubcollection(uid, "downloads", "createdAt")
    fun observeNotifications(uid: String): Flow<List<NotificationItem>> = observeSubcollection(uid, "notifications", "createdAt")
    fun observeSecurityLogs(uid: String): Flow<List<SecurityLogItem>> = observeSubcollection(uid, "securityLogs", "createdAt")
    fun observeTickets(uid: String): Flow<List<SupportTicket>> = observeSubcollection(uid, "supportTickets")

    fun observeScenes(uid: String, projectId: String): Flow<List<SceneItem>> = flow {
        val currentUid = requireUserId()
        check(currentUid == uid)
        val path = "users/$uid/projects/$projectId/scenes"
        emitAll(
            userDoc(uid).collection("projects").document(projectId).collection("scenes")
                .orderBy("sceneNumber", Query.Direction.ASCENDING)
                .snapshots()
                .map { it.toObjects(SceneItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeClips(uid: String, projectId: String): Flow<List<ClipItem>> = flow {
        val currentUid = requireUserId()
        check(currentUid == uid)
        val path = "users/$uid/projects/$projectId/clips"
        emitAll(
            userDoc(uid).collection("projects").document(projectId).collection("clips")
                .orderBy("clipOrder", Query.Direction.ASCENDING)
                .snapshots()
                .map { it.toObjects(ClipItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun updateAccountInfo(
        fullName: String,
        username: String,
        country: String,
        phoneNumber: String,
        photoUrl: String? = null
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid"
        return try {
            require(fullName.trim().isNotEmpty()) { "Full name cannot be empty." }
            require(username.trim().length >= 2) { "Username must be at least 2 characters." }
            val updates = mutableMapOf<String, Any>(
                "fullName" to fullName.trim().take(120),
                "username" to username.trim().take(40),
                "country" to country.trim().take(100),
                "phoneNumber" to phoneNumber.trim().take(30),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (photoUrl != null) {
                updates["photoUrl"] = photoUrl.take(2000)
            }
            userDoc(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(profile: UserProfile): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "userProfiles/$uid"
        return try {
            db.collection("userProfiles").document(uid).set(
                mapOf(
                    "userId" to uid,
                    "bio" to profile.bio.take(500),
                    "darkMode" to profile.darkMode,
                    "appLanguage" to profile.appLanguage.take(60),
                    "notificationsEnabled" to profile.notificationsEnabled,
                    "soundEnabled" to profile.soundEnabled,
                    "videoAutoplay" to profile.videoAutoplay,
                    "defaultQuality" to profile.defaultQuality.take(30),
                    "defaultAspectRatio" to profile.defaultAspectRatio.take(20),
                    "defaultResolution" to profile.defaultResolution.take(20),
                    "defaultDurationSec" to profile.defaultDurationSec.coerceIn(5, 7200),
                    "defaultMovieLanguage" to profile.defaultMovieLanguage.take(60),
                    "defaultVoice" to profile.defaultVoice.take(80),
                    "defaultGenre" to profile.defaultGenre.take(60),
                    "autoSubtitles" to profile.autoSubtitles,
                    "autoSaveProjects" to profile.autoSaveProjects,
                    "defaultPrivacy" to profile.defaultPrivacy,
                    "allowDownloads" to profile.allowDownloads,
                    "dataUsageSaver" to profile.dataUsageSaver,
                    "downloadWifiOnly" to profile.downloadWifiOnly,
                    "notifyLikes" to profile.notifyLikes,
                    "notifyComments" to profile.notifyComments,
                    "notifyFollowers" to profile.notifyFollowers,
                    "notifyMessages" to profile.notifyMessages,
                    "notifyRemixes" to profile.notifyRemixes,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun saveProject(project: MovieProject): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val isNew = project.id.isBlank()
        val docRef = if (isNew) {
            userDoc(uid).collection("projects").document()
        } else {
            userDoc(uid).collection("projects").document(project.id)
        }
        return try {
            require(project.title.trim().isNotEmpty()) { "Movie title is required." }
            if (isNew) {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "title" to project.title.trim().take(200),
                        "description" to project.description.take(5000),
                        "genre" to project.genre.take(80),
                        "countryLocation" to project.countryLocation.take(120),
                        "language" to project.language.take(80),
                        "mainCharacterSummary" to project.mainCharacterSummary.take(1000),
                        "characterAppearance" to project.characterAppearance.take(500),
                        "characterClothing" to project.characterClothing.take(500),
                        "characterAgeRange" to project.characterAgeRange.take(80),
                        "cameraAngle" to project.cameraAngle.take(100),
                        "lighting" to project.lighting.take(100),
                        "atmosphere" to project.atmosphere.take(120),
                        "backgroundSetting" to project.backgroundSetting.take(300),
                        "visualStyle" to project.visualStyle.take(120),
                        "voiceStyle" to project.voiceStyle.take(120),
                        "musicStyle" to project.musicStyle.take(120),
                        "aspectRatio" to project.aspectRatio.take(20),
                        "resolution" to project.resolution.take(20),
                        "targetDurationSec" to project.targetDurationSec.coerceIn(5, 14400),
                        "estimatedCredits" to project.estimatedCredits.coerceAtLeast(0),
                        "status" to project.status,
                        "publicProject" to project.publicProject,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "title" to project.title.trim().take(200),
                        "description" to project.description.take(5000),
                        "genre" to project.genre.take(80),
                        "countryLocation" to project.countryLocation.take(120),
                        "language" to project.language.take(80),
                        "mainCharacterSummary" to project.mainCharacterSummary.take(1000),
                        "characterAppearance" to project.characterAppearance.take(500),
                        "characterClothing" to project.characterClothing.take(500),
                        "characterAgeRange" to project.characterAgeRange.take(80),
                        "cameraAngle" to project.cameraAngle.take(100),
                        "lighting" to project.lighting.take(100),
                        "atmosphere" to project.atmosphere.take(120),
                        "backgroundSetting" to project.backgroundSetting.take(300),
                        "visualStyle" to project.visualStyle.take(120),
                        "voiceStyle" to project.voiceStyle.take(120),
                        "musicStyle" to project.musicStyle.take(120),
                        "aspectRatio" to project.aspectRatio.take(20),
                        "resolution" to project.resolution.take(20),
                        "targetDurationSec" to project.targetDurationSec.coerceIn(5, 14400),
                        "estimatedCredits" to project.estimatedCredits.coerceAtLeast(0),
                        "status" to project.status,
                        "publicProject" to project.publicProject,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteProject(projectId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid/projects/$projectId"
        return try {
            userDoc(uid).collection("projects").document(projectId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun saveScene(projectId: String, scene: SceneItem): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val col = userDoc(uid).collection("projects").document(projectId).collection("scenes")
        val docRef = if (scene.id.isBlank()) col.document() else col.document(scene.id)
        return try {
            if (scene.id.isBlank()) {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "projectId" to projectId,
                        "sceneNumber" to scene.sceneNumber.coerceAtLeast(1),
                        "title" to scene.title.ifBlank { "Scene ${scene.sceneNumber}" }.take(200),
                        "location" to scene.location.take(200),
                        "description" to scene.description.take(3000),
                        "cameraInstruction" to scene.cameraInstruction.take(500),
                        "shotDescription" to scene.shotDescription.take(1000),
                        "narration" to scene.narration.take(2000),
                        "dialogue" to scene.dialogue.take(4000),
                        "soundEffects" to scene.soundEffects.take(500),
                        "durationSec" to scene.durationSec.coerceIn(1, 1800),
                        "storyboardData" to scene.storyboardData.take(340000),
                        "status" to scene.status,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "sceneNumber" to scene.sceneNumber.coerceAtLeast(1),
                        "title" to scene.title.ifBlank { "Scene ${scene.sceneNumber}" }.take(200),
                        "location" to scene.location.take(200),
                        "description" to scene.description.take(3000),
                        "cameraInstruction" to scene.cameraInstruction.take(500),
                        "shotDescription" to scene.shotDescription.take(1000),
                        "narration" to scene.narration.take(2000),
                        "dialogue" to scene.dialogue.take(4000),
                        "soundEffects" to scene.soundEffects.take(500),
                        "durationSec" to scene.durationSec.coerceIn(1, 1800),
                        "storyboardData" to scene.storyboardData.take(340000),
                        "status" to scene.status,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteScene(projectId: String, sceneId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val ref = userDoc(uid).collection("projects").document(projectId).collection("scenes").document(sceneId)
        return try {
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun saveClip(projectId: String, clip: ClipItem): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val col = userDoc(uid).collection("projects").document(projectId).collection("clips")
        val docRef = if (clip.id.isBlank()) col.document() else col.document(clip.id)
        return try {
            if (clip.id.isBlank()) {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "projectId" to projectId,
                        "sceneId" to clip.sceneId.ifBlank { "scene_1" },
                        "clipOrder" to clip.clipOrder.coerceAtLeast(0),
                        "title" to clip.title.take(200),
                        "prompt" to clip.prompt.take(2000),
                        "durationSec" to clip.durationSec.coerceIn(1, 600),
                        "trimStartMs" to clip.trimStartMs.coerceAtLeast(0),
                        "trimEndMs" to clip.trimEndMs.coerceAtLeast(0),
                        "transitionType" to clip.transitionType.take(60),
                        "subtitleText" to clip.subtitleText.take(1000),
                        "voiceoverText" to clip.voiceoverText.take(2000),
                        "musicTrackName" to clip.musicTrackName.take(150),
                        "videoUrl" to clip.videoUrl.take(2000),
                        "status" to clip.status,
                        "errorMessage" to clip.errorMessage.take(1000),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "clipOrder" to clip.clipOrder.coerceAtLeast(0),
                        "title" to clip.title.take(200),
                        "prompt" to clip.prompt.take(2000),
                        "durationSec" to clip.durationSec.coerceIn(1, 600),
                        "trimStartMs" to clip.trimStartMs.coerceAtLeast(0),
                        "trimEndMs" to clip.trimEndMs.coerceAtLeast(0),
                        "transitionType" to clip.transitionType.take(60),
                        "subtitleText" to clip.subtitleText.take(1000),
                        "voiceoverText" to clip.voiceoverText.take(2000),
                        "musicTrackName" to clip.musicTrackName.take(150),
                        "videoUrl" to clip.videoUrl.take(2000),
                        "status" to clip.status,
                        "errorMessage" to clip.errorMessage.take(1000),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteClip(projectId: String, clipId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val ref = userDoc(uid).collection("projects").document(projectId).collection("clips").document(clipId)
        return try {
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun saveScript(script: ScriptDoc): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val col = userDoc(uid).collection("scripts")
        val docRef = if (script.id.isBlank()) col.document() else col.document(script.id)
        return try {
            if (script.id.isBlank()) {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "projectId" to script.projectId.ifBlank { "standalone" },
                        "title" to script.title.ifBlank { "Untitled Script" }.take(200),
                        "synopsis" to script.synopsis.take(3000),
                        "charactersSummary" to script.charactersSummary.take(3000),
                        "content" to script.content.take(80000),
                        "aiSuggestion" to script.aiSuggestion.take(30000),
                        "version" to script.version.coerceAtLeast(1),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "title" to script.title.ifBlank { "Untitled Script" }.take(200),
                        "synopsis" to script.synopsis.take(3000),
                        "charactersSummary" to script.charactersSummary.take(3000),
                        "content" to script.content.take(80000),
                        "aiSuggestion" to script.aiSuggestion.take(30000),
                        "version" to script.version.coerceAtLeast(1),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun saveCharacter(character: CharacterProfile): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val col = userDoc(uid).collection("characters")
        val docRef = if (character.id.isBlank()) col.document() else col.document(character.id)
        return try {
            require(character.name.trim().isNotEmpty()) { "Character name is required." }
            if (character.id.isBlank()) {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "name" to character.name.trim().take(100),
                        "role" to character.role.take(100),
                        "ageRange" to character.ageRange.take(60),
                        "appearance" to character.appearance.take(800),
                        "clothing" to character.clothing.take(500),
                        "hairstyle" to character.hairstyle.take(200),
                        "facialExpression" to character.facialExpression.take(200),
                        "bodyPosture" to character.bodyPosture.take(200),
                        "voiceProfile" to character.voiceProfile.take(150),
                        "fictional" to character.fictional,
                        "likenessConsentConfirmed" to character.likenessConsentConfirmed,
                        "referenceImageData" to character.referenceImageData.take(340000),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "name" to character.name.trim().take(100),
                        "role" to character.role.take(100),
                        "ageRange" to character.ageRange.take(60),
                        "appearance" to character.appearance.take(800),
                        "clothing" to character.clothing.take(500),
                        "hairstyle" to character.hairstyle.take(200),
                        "facialExpression" to character.facialExpression.take(200),
                        "bodyPosture" to character.bodyPosture.take(200),
                        "voiceProfile" to character.voiceProfile.take(150),
                        "fictional" to character.fictional,
                        "likenessConsentConfirmed" to character.likenessConsentConfirmed,
                        "referenceImageData" to character.referenceImageData.take(340000),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteCharacter(characterId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val ref = userDoc(uid).collection("characters").document(characterId)
        return try {
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun saveMovie(movie: MovieItem): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val col = userDoc(uid).collection("movies")
        val docRef = if (movie.id.isBlank()) col.document() else col.document(movie.id)
        return try {
            if (movie.id.isBlank()) {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "projectId" to movie.projectId.ifBlank { "standalone" },
                        "title" to movie.title.ifBlank { "Untitled Movie" }.take(200),
                        "genre" to movie.genre.take(80),
                        "durationSec" to movie.durationSec.coerceIn(0, 14400),
                        "resolution" to movie.resolution.take(20),
                        "aspectRatio" to movie.aspectRatio.take(20),
                        "status" to movie.status,
                        "thumbnailData" to movie.thumbnailData.take(340000),
                        "videoUrl" to movie.videoUrl.take(2000),
                        "subtitlesText" to movie.subtitlesText.take(10000),
                        "favorite" to movie.favorite,
                        "downloaded" to movie.downloaded,
                        "publicMovie" to movie.publicMovie,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "title" to movie.title.ifBlank { "Untitled Movie" }.take(200),
                        "genre" to movie.genre.take(80),
                        "durationSec" to movie.durationSec.coerceIn(0, 14400),
                        "resolution" to movie.resolution.take(20),
                        "aspectRatio" to movie.aspectRatio.take(20),
                        "status" to movie.status,
                        "thumbnailData" to movie.thumbnailData.take(340000),
                        "videoUrl" to movie.videoUrl.take(2000),
                        "subtitlesText" to movie.subtitlesText.take(10000),
                        "favorite" to movie.favorite,
                        "downloaded" to movie.downloaded,
                        "publicMovie" to movie.publicMovie,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleMovieFavorite(movie: MovieItem): Result<Unit> {
        return saveMovie(movie.copy(favorite = !movie.favorite)).map { }
    }

    suspend fun markMovieViewed(movieId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val ref = userDoc(uid).collection("movies").document(movieId)
        return try {
            ref.update(
                mapOf(
                    "lastViewedAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
            Result.failure(e)
        }
    }

    fun observeRecentlyDeleted(uid: String): Flow<List<com.example.data.model.RecentlyDeletedItem>> =
        observeSubcollection(uid, "recentlyDeleted", "deletedAtMs")

    suspend fun softDeleteMovieToRecycleBin(movie: MovieItem): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val ref = userDoc(uid).collection("movies").document(movie.id)
        val trashRef = userDoc(uid).collection("recentlyDeleted").document("movie_${movie.id}")
        return try {
            val nowMs = System.currentTimeMillis()
            val expiresMs = nowMs + 30L * 24L * 60L * 60L * 1000L
            val json = org.json.JSONObject().apply {
                put("projectId", movie.projectId)
                put("title", movie.title)
                put("genre", movie.genre)
                put("durationSec", movie.durationSec)
                put("resolution", movie.resolution)
                put("aspectRatio", movie.aspectRatio)
                put("status", movie.status)
                put("videoUrl", movie.videoUrl)
                put("subtitlesText", movie.subtitlesText)
                put("favorite", movie.favorite)
                put("downloaded", movie.downloaded)
                put("publicMovie", movie.publicMovie)
            }.toString()
            trashRef.set(
                mapOf(
                    "userId" to uid,
                    "itemType" to "movie",
                    "originalId" to movie.id,
                    "title" to movie.title.ifBlank { "Deleted Movie" }.take(200),
                    "subtitle" to "${movie.genre} • ${movie.resolution} • ${movie.status}".take(160),
                    "creationType" to "movie",
                    "previewVideoUrl" to movie.videoUrl.take(2000),
                    "thumbnailData" to movie.thumbnailData.take(250000),
                    "payloadJson" to json.take(60000),
                    "deletedAtMs" to nowMs,
                    "expiresAtMs" to expiresMs,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun softDeleteProjectToRecycleBin(project: MovieProject): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val ref = userDoc(uid).collection("projects").document(project.id)
        val trashRef = userDoc(uid).collection("recentlyDeleted").document("project_${project.id}")
        return try {
            val nowMs = System.currentTimeMillis()
            val expiresMs = nowMs + 30L * 24L * 60L * 60L * 1000L
            val json = org.json.JSONObject().apply {
                put("title", project.title)
                put("description", project.description)
                put("genre", project.genre)
                put("countryLocation", project.countryLocation)
                put("language", project.language)
                put("mainCharacterSummary", project.mainCharacterSummary)
                put("characterAppearance", project.characterAppearance)
                put("characterClothing", project.characterClothing)
                put("characterAgeRange", project.characterAgeRange)
                put("cameraAngle", project.cameraAngle)
                put("lighting", project.lighting)
                put("atmosphere", project.atmosphere)
                put("backgroundSetting", project.backgroundSetting)
                put("visualStyle", project.visualStyle)
                put("voiceStyle", project.voiceStyle)
                put("musicStyle", project.musicStyle)
                put("aspectRatio", project.aspectRatio)
                put("resolution", project.resolution)
                put("targetDurationSec", project.targetDurationSec)
                put("estimatedCredits", project.estimatedCredits)
                put("status", project.status)
                put("publicProject", project.publicProject)
            }.toString()
            trashRef.set(
                mapOf(
                    "userId" to uid,
                    "itemType" to "project",
                    "originalId" to project.id,
                    "title" to project.title.ifBlank { "Deleted Project" }.take(200),
                    "subtitle" to "${project.genre} • ${project.resolution} • Project".take(160),
                    "creationType" to "project",
                    "previewVideoUrl" to "",
                    "thumbnailData" to "",
                    "payloadJson" to json.take(60000),
                    "deletedAtMs" to nowMs,
                    "expiresAtMs" to expiresMs,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun batchSoftDeleteMoviesToRecycleBin(movies: List<MovieItem>): Result<Int> {
        var count = 0
        for (m in movies) {
            val res = softDeleteMovieToRecycleBin(m)
            if (res.isSuccess) count++
        }
        return Result.success(count)
    }

    suspend fun restoreDeletedItem(item: com.example.data.model.RecentlyDeletedItem): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val trashRef = userDoc(uid).collection("recentlyDeleted").document(item.id)
        return try {
            val json = org.json.JSONObject(item.payloadJson.ifBlank { "{}" })
            when (item.itemType) {
                "post" -> {
                    val targetId = item.originalId.ifBlank { db.collection("posts").document().id }
                    db.collection("posts").document(targetId).set(
                        mapOf(
                            "creatorId" to uid,
                            "creatorUsername" to json.optString("creatorUsername", "spark_creator"),
                            "creatorAvatarUrl" to json.optString("creatorAvatarUrl", ""),
                            "creationType" to json.optString("creationType", item.creationType.ifBlank { "short_video" }),
                            "mediaType" to json.optString("mediaType", "video"),
                            "title" to json.optString("title", item.title),
                            "caption" to json.optString("caption", ""),
                            "hashtags" to json.optString("hashtags", ""),
                            "mentions" to json.optString("mentions", ""),
                            "category" to json.optString("category", "Movie scenes"),
                            "previewVideoUrl" to json.optString("previewVideoUrl", item.previewVideoUrl),
                            "thumbnailData" to item.thumbnailData,
                            "photoThumbnails" to emptyList<String>(),
                            "soundId" to json.optString("soundId", ""),
                            "soundTitle" to json.optString("soundTitle", ""),
                            "durationSec" to json.optInt("durationSec", 15),
                            "aspectRatio" to json.optString("aspectRatio", "9:16"),
                            "resolution" to json.optString("resolution", "1080p"),
                            "visibility" to json.optString("visibility", "public"),
                            "allowComments" to json.optBoolean("allowComments", true),
                            "allowReuse" to json.optBoolean("allowReuse", true),
                            "allowDownload" to json.optBoolean("allowDownload", true),
                            "viewsCount" to 0,
                            "uniqueViewersCount" to 0,
                            "likesCount" to 0,
                            "commentsCount" to 0,
                            "sharesCount" to 0,
                            "repostsCount" to 0,
                            "savesCount" to 0,
                            "moderationStatus" to "active",
                            "linkedProjectId" to json.optString("linkedProjectId", ""),
                            "linkedTemplateId" to json.optString("linkedTemplateId", ""),
                            "originalCreatorName" to json.optString("originalCreatorName", ""),
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                    val profRef = db.collection("creatorProfiles").document(uid)
                    if (profRef.get().await().exists()) {
                        profRef.update(
                            mapOf(
                                "publishedCount" to FieldValue.increment(1),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                }
                "project" -> {
                    val targetId = item.originalId.ifBlank { userDoc(uid).collection("projects").document().id }
                    userDoc(uid).collection("projects").document(targetId).set(
                        mapOf(
                            "userId" to uid,
                            "title" to json.optString("title", item.title),
                            "description" to json.optString("description", ""),
                            "genre" to json.optString("genre", "Action"),
                            "countryLocation" to json.optString("countryLocation", "Lagos, Nigeria"),
                            "language" to json.optString("language", "English"),
                            "mainCharacterSummary" to json.optString("mainCharacterSummary", ""),
                            "characterAppearance" to json.optString("characterAppearance", ""),
                            "characterClothing" to json.optString("characterClothing", ""),
                            "characterAgeRange" to json.optString("characterAgeRange", "25-35"),
                            "cameraAngle" to json.optString("cameraAngle", "Dynamic Tracking Shot"),
                            "lighting" to json.optString("lighting", "Cinematic Golden & Neon Rim"),
                            "atmosphere" to json.optString("atmosphere", "High-Stakes Suspense"),
                            "backgroundSetting" to json.optString("backgroundSetting", ""),
                            "visualStyle" to json.optString("visualStyle", "Photorealistic Cinema 4K"),
                            "voiceStyle" to json.optString("voiceStyle", "Deep Dramatic Narration"),
                            "musicStyle" to json.optString("musicStyle", "Afrobeats Orchestral Hybrid"),
                            "aspectRatio" to json.optString("aspectRatio", "16:9"),
                            "resolution" to json.optString("resolution", "1080p"),
                            "targetDurationSec" to json.optInt("targetDurationSec", 60),
                            "estimatedCredits" to json.optInt("estimatedCredits", 2),
                            "status" to json.optString("status", "draft"),
                            "publicProject" to json.optBoolean("publicProject", false),
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
                else -> {
                    val targetId = item.originalId.ifBlank { userDoc(uid).collection("movies").document().id }
                    userDoc(uid).collection("movies").document(targetId).set(
                        mapOf(
                            "userId" to uid,
                            "projectId" to json.optString("projectId", "standalone"),
                            "title" to json.optString("title", item.title),
                            "genre" to json.optString("genre", "Action"),
                            "durationSec" to json.optInt("durationSec", 30),
                            "resolution" to json.optString("resolution", "1080p"),
                            "aspectRatio" to json.optString("aspectRatio", "16:9"),
                            "status" to json.optString("status", "draft"),
                            "thumbnailData" to item.thumbnailData,
                            "videoUrl" to json.optString("videoUrl", item.previewVideoUrl),
                            "subtitlesText" to json.optString("subtitlesText", ""),
                            "favorite" to json.optBoolean("favorite", false),
                            "downloaded" to json.optBoolean("downloaded", false),
                            "publicMovie" to json.optBoolean("publicMovie", false),
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            trashRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, trashRef.path)
            Result.failure(e)
        }
    }

    suspend fun permanentlyDeleteFromRecycleBin(itemId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val ref = userDoc(uid).collection("recentlyDeleted").document(itemId)
        return try {
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun purgeExpiredRecycleBin(items: List<com.example.data.model.RecentlyDeletedItem>): Result<Int> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val now = System.currentTimeMillis()
        var purged = 0
        return try {
            for (item in items) {
                if (item.expiresAtMs in 1..now) {
                    userDoc(uid).collection("recentlyDeleted").document(item.id).delete().await()
                    purged++
                }
            }
            Result.success(purged)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$uid/recentlyDeleted")
            Result.failure(e)
        }
    }

    suspend fun deleteMovie(movieId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val ref = userDoc(uid).collection("movies").document(movieId)
        return try {
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun createGenerationJob(
        projectId: String,
        movieTitle: String,
        jobType: String,
        totalScenes: Int,
        totalClips: Int,
        estimatedCredits: Int,
        providerId: String
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val col = userDoc(uid).collection("generationJobs")
        val docRef = col.document()
        return try {
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "projectId" to projectId,
                    "movieTitle" to movieTitle.take(200),
                    "jobType" to jobType,
                    "status" to "queued",
                    "totalScenes" to totalScenes.coerceAtLeast(0),
                    "totalClips" to totalClips.coerceAtLeast(0),
                    "completedClips" to 0,
                    "failedClips" to 0,
                    "estimatedCredits" to estimatedCredits.coerceAtLeast(0),
                    "creditsCharged" to 0,
                    "idempotencyKey" to "idem_${UUID.randomUUID().toString().take(16)}",
                    "providerId" to providerId.take(100),
                    "errorMessage" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateGenerationJobProgress(
        jobId: String,
        status: String,
        completedClips: Int,
        failedClips: Int,
        creditsCharged: Int,
        errorMessage: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val ref = userDoc(uid).collection("generationJobs").document(jobId)
        return try {
            ref.update(
                mapOf(
                    "status" to status,
                    "completedClips" to completedClips.coerceAtLeast(0),
                    "failedClips" to failedClips.coerceAtLeast(0),
                    "creditsCharged" to creditsCharged.coerceAtLeast(0),
                    "errorMessage" to errorMessage.take(1000),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
            Result.failure(e)
        }
    }

    // Only invoked AFTER a movie/clip generation succeeds and is verified
    suspend fun deductCreditsOnSuccess(amount: Int, description: String): Result<Unit> {
        if (amount <= 0) return Result.success(Unit)
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val ref = userDoc(uid).collection("credits").document("balance")
        return try {
            val snap = ref.get().await()
            val currentBal = (snap.getLong("balance") ?: 0L).toInt()
            val spent = (snap.getLong("lifetimeSpent") ?: 0L).toInt()
            val newBal = (currentBal - amount).coerceAtLeast(0)
            ref.update(
                mapOf(
                    "balance" to newBal,
                    "lifetimeSpent" to (spent + (currentBal - newBal)),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            userDoc(uid).collection("transactions").document().set(
                mapOf(
                    "userId" to uid,
                    "type" to "generation_charge",
                    "amountCredits" to -amount,
                    "amountUsd" to 0.0,
                    "description" to description.take(400),
                    "status" to "completed",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun recordDownload(
        movieId: String,
        movieTitle: String,
        resolution: String,
        fileUri: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val dlRef = userDoc(uid).collection("downloads").document()
        return try {
            dlRef.set(
                mapOf(
                    "userId" to uid,
                    "movieId" to movieId,
                    "movieTitle" to movieTitle.take(200),
                    "resolution" to resolution.take(20),
                    "format" to "MP4",
                    "fileUri" to fileUri.take(1000),
                    "status" to "completed",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            if (movieId.isNotBlank()) {
                userDoc(uid).collection("movies").document(movieId).update(
                    mapOf("downloaded" to true, "updatedAt" to FieldValue.serverTimestamp())
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, dlRef.path)
            Result.failure(e)
        }
    }

    suspend fun submitSupportTicket(subject: String, message: String): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("Not signed in"))
        val uid = user.uid
        val ref = userDoc(uid).collection("supportTickets").document()
        return try {
            require(subject.trim().isNotEmpty()) { "Subject is required." }
            require(message.trim().isNotEmpty()) { "Message is required." }
            ref.set(
                mapOf(
                    "userId" to uid,
                    "userEmail" to user.email.orEmpty(),
                    "subject" to subject.trim().take(200),
                    "message" to message.trim().take(3000),
                    "status" to "open",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun cancelSubscription(): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val ref = userDoc(uid).collection("subscriptions").document("current")
        return try {
            ref.update(
                mapOf(
                    "status" to "cancelled",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun deleteAccountAndData(): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("Not signed in"))
        val uid = user.uid
        return try {
            db.collection("userProfiles").document(uid).delete().await()
            userDoc(uid).delete().await()
            user.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun markNotificationRead(notifId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val ref = userDoc(uid).collection("notifications").document(notifId)
        return try {
            ref.update("readFlag", true).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun markAllNotificationsRead(notifIds: List<String>): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            for (id in notifIds) {
                userDoc(uid).collection("notifications").document(id).update("readFlag", true).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid/notifications")
            Result.failure(e)
        }
    }
}
