package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.model.BlockedCreatorRef
import com.example.data.model.CommunityTemplate
import com.example.data.model.CreatorFollower
import com.example.data.model.CreatorProfile
import com.example.data.model.RemixWorkspaceSeed
import com.example.data.model.SavedTemplateRef
import com.example.data.model.TemplateComment
import com.example.data.model.TemplateLike
import com.example.data.model.TemplatePrompt
import com.example.data.model.TemplateRemix
import com.example.data.model.TemplateSettingsDoc
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
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

class CommunityRepository(
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
            ?: throw IllegalStateException("Must be signed in to access Spark AI Community.")
    }

    private fun logStreamError(error: Throwable, op: OperationType, path: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser != null) {
            handleFirestoreError(exceptionToLog, op, path)
        }
    }

    fun observePublicTemplates(): Flow<List<CommunityTemplate>> = flow {
        requireUserId()
        // Query filters MUST match firestore.rules `allow list` exactly
        emitAll(
            db.collection("communityTemplates")
                .whereEqualTo("visibility", "public")
                .whereEqualTo("moderationStatus", "active")
                .snapshots()
                .map { snap ->
                    snap.toObjects(CommunityTemplate::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.trendingScore }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, "communityTemplates")
                    throw error
                }
        )
    }

    fun observeMyTemplates(uid: String): Flow<List<CommunityTemplate>> = flow {
        requireUserId()
        emitAll(
            db.collection("communityTemplates")
                .whereEqualTo("creatorId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(CommunityTemplate::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, "communityTemplates")
                    throw error
                }
        )
    }

    fun observeMyLikes(uid: String): Flow<Set<String>> = flow {
        requireUserId()
        emitAll(
            db.collection("templateLikes")
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(TemplateLike::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .map { it.templateId }
                        .toSet()
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, "templateLikes")
                    throw error
                }
        )
    }

    fun observeSavedTemplateIds(uid: String): Flow<Set<String>> = flow {
        requireUserId()
        emitAll(
            db.collection("users").document(uid).collection("savedTemplates")
                .snapshots()
                .map { snap ->
                    snap.toObjects(SavedTemplateRef::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .map { it.templateId }
                        .toSet()
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, "users/$uid/savedTemplates")
                    throw error
                }
        )
    }

    fun observeBlockedCreatorIds(uid: String): Flow<Set<String>> = flow {
        requireUserId()
        emitAll(
            db.collection("users").document(uid).collection("blockedCreators")
                .snapshots()
                .map { snap ->
                    snap.toObjects(BlockedCreatorRef::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .map { it.creatorId }
                        .toSet()
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, "users/$uid/blockedCreators")
                    throw error
                }
        )
    }

    fun observeFollowingCreatorIds(uid: String): Flow<Set<String>> = flow {
        requireUserId()
        emitAll(
            db.collection("creatorFollowers")
                .whereEqualTo("followerId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(CreatorFollower::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .map { it.creatorId }
                        .toSet()
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, "creatorFollowers")
                    throw error
                }
        )
    }

    fun observeComments(templateId: String): Flow<List<TemplateComment>> = flow {
        requireUserId()
        emitAll(
            db.collection("templateComments")
                .whereEqualTo("templateId", templateId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(TemplateComment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, "templateComments")
                    throw error
                }
        )
    }

    fun observeRemixes(templateId: String): Flow<List<TemplateRemix>> = flow {
        requireUserId()
        emitAll(
            db.collection("templateRemixes")
                .whereEqualTo("originalTemplateId", templateId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(TemplateRemix::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, "templateRemixes")
                    throw error
                }
        )
    }

    fun observeCreatorProfile(creatorId: String): Flow<CreatorProfile?> =
        db.collection("creatorProfiles").document(creatorId).snapshots()
            .map { it.toObject(CreatorProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                logStreamError(error, OperationType.GET, "creatorProfiles/$creatorId")
                throw error
            }

    suspend fun publishTemplate(
        creatorUsername: String,
        creatorDisplayName: String,
        creatorAvatarUrl: String,
        template: CommunityTemplate,
        prompt: TemplatePrompt,
        settings: TemplateSettingsDoc
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val templateRef = db.collection("communityTemplates").document()
        val tid = templateRef.id
        return try {
            require(template.title.trim().isNotEmpty()) { "Template title is required." }
            require(prompt.basePrompt.trim().isNotEmpty()) { "Reusable prompt is required." }

            templateRef.set(
                mapOf(
                    "creatorId" to uid,
                    "creatorUsername" to creatorUsername.ifBlank { "spark_creator" }.take(80),
                    "creatorAvatarUrl" to creatorAvatarUrl.take(2000),
                    "title" to template.title.trim().take(160),
                    "description" to template.description.take(3000),
                    "category" to template.category.take(80),
                    "previewVideoUrl" to template.previewVideoUrl.take(2000),
                    "thumbnailData" to template.thumbnailData.take(340000),
                    "durationSec" to template.durationSec.coerceIn(5, 14400),
                    "aspectRatio" to template.aspectRatio.take(20),
                    "requiredInputs" to template.requiredInputs.take(500),
                    "effectsSummary" to template.effectsSummary.take(500),
                    "tags" to template.tags.take(400),
                    "visibility" to template.visibility,
                    "allowReuse" to template.allowReuse,
                    "likesCount" to 0,
                    "commentsCount" to 0,
                    "remixesCount" to 0,
                    "trendingScore" to 1,
                    "moderationStatus" to "active",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            db.collection("templatePrompts").document(tid).set(
                mapOf(
                    "templateId" to tid,
                    "creatorId" to uid,
                    "visibility" to template.visibility,
                    "allowReuse" to template.allowReuse,
                    "basePrompt" to prompt.basePrompt.trim().take(5000),
                    "sceneDescription" to prompt.sceneDescription.take(4000),
                    "characterTemplate" to prompt.characterTemplate.take(1000),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            db.collection("templateSettings").document(tid).set(
                mapOf(
                    "templateId" to tid,
                    "creatorId" to uid,
                    "visibility" to template.visibility,
                    "allowReuse" to template.allowReuse,
                    "cameraMovement" to settings.cameraMovement.take(120),
                    "lighting" to settings.lighting.take(120),
                    "videoStyle" to settings.videoStyle.take(120),
                    "voiceStyle" to settings.voiceStyle.take(120),
                    "musicStyle" to settings.musicStyle.take(120),
                    "aspectRatio" to settings.aspectRatio.take(20),
                    "resolution" to settings.resolution.take(20),
                    "durationSec" to settings.durationSec.coerceIn(5, 14400),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val creatorRef = db.collection("creatorProfiles").document(uid)
            val existingProfile = creatorRef.get().await()
            if (!existingProfile.exists()) {
                creatorRef.set(
                    mapOf(
                        "creatorId" to uid,
                        "username" to creatorUsername.ifBlank { "spark_creator" }.take(80),
                        "displayName" to creatorDisplayName.ifBlank { creatorUsername }.take(120),
                        "avatarUrl" to creatorAvatarUrl.take(2000),
                        "bio" to "Spark AI Creator",
                        "publishedCount" to 1,
                        "followersCount" to 0,
                        "totalLikes" to 0,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                creatorRef.update(
                    mapOf(
                        "publishedCount" to FieldValue.increment(1),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            Result.success(tid)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, templateRef.path)
            Result.failure(e)
        }
    }

    suspend fun loadRemixSeed(template: CommunityTemplate): Result<RemixWorkspaceSeed> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        if (template.visibility == "private" && template.creatorId != uid) {
            return Result.failure(IllegalStateException("This template is private and cannot be reused."))
        }
        if (!template.allowReuse && template.creatorId != uid) {
            return Result.failure(IllegalStateException("The creator has disabled reuse for this template."))
        }
        return try {
            val promptSnap = db.collection("templatePrompts").document(template.id).get().await()
            val settingsSnap = db.collection("templateSettings").document(template.id).get().await()
            val prompt = promptSnap.toObject(TemplatePrompt::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: TemplatePrompt(templateId = template.id, creatorId = template.creatorId, basePrompt = template.description)
            val settings = settingsSnap.toObject(TemplateSettingsDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: TemplateSettingsDoc(templateId = template.id, creatorId = template.creatorId, aspectRatio = template.aspectRatio, durationSec = template.durationSec)
            Result.success(RemixWorkspaceSeed(template, prompt, settings))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "templatePrompts/${template.id}")
            Result.failure(e)
        }
    }

    suspend fun recordRemixAttribution(
        seed: RemixWorkspaceSeed,
        remixerUsername: String,
        newProjectId: String,
        newMovieTitle: String,
        customPrompt: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val ref = db.collection("templateRemixes").document()
        return try {
            ref.set(
                mapOf(
                    "originalTemplateId" to seed.template.id,
                    "originalTemplateTitle" to seed.template.title.take(160),
                    "originalCreatorId" to seed.template.creatorId,
                    "originalCreatorName" to seed.template.creatorUsername.take(80),
                    "remixerUserId" to uid,
                    "remixerUsername" to remixerUsername.ifBlank { "spark_creator" }.take(80),
                    "newProjectId" to newProjectId,
                    "newMovieTitle" to newMovieTitle.take(200),
                    "customPrompt" to customPrompt.take(5000),
                    "previewVideoUrl" to seed.template.previewVideoUrl.take(2000),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            db.collection("communityTemplates").document(seed.template.id).update(
                mapOf(
                    "remixesCount" to FieldValue.increment(1),
                    "trendingScore" to FieldValue.increment(3),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            if (seed.template.creatorId != uid) {
                try {
                    db.collection("users").document(seed.template.creatorId).collection("notifications").document().set(
                        mapOf(
                            "userId" to seed.template.creatorId,
                            "title" to "Template Remixed",
                            "message" to "@${remixerUsername.ifBlank { "creator" }} used your template \"${seed.template.title}\" to create \"$newMovieTitle\".",
                            "category" to "remix",
                            "actorUserId" to uid,
                            "targetPostId" to seed.template.id,
                            "targetCreatorId" to uid,
                            "readFlag" to false,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                } catch (_: Exception) {}
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun toggleLike(template: CommunityTemplate, currentlyLiked: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val likeId = "${template.id}_$uid"
        val likeRef = db.collection("templateLikes").document(likeId)
        val tplRef = db.collection("communityTemplates").document(template.id)
        return try {
            if (currentlyLiked) {
                likeRef.delete().await()
                tplRef.update(
                    mapOf(
                        "likesCount" to (template.likesCount - 1).coerceAtLeast(0),
                        "trendingScore" to (template.trendingScore - 1).coerceAtLeast(0),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                likeRef.set(
                    mapOf(
                        "templateId" to template.id,
                        "userId" to uid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                tplRef.update(
                    mapOf(
                        "likesCount" to FieldValue.increment(1),
                        "trendingScore" to FieldValue.increment(2),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, likeRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleSavedTemplate(templateId: String, currentlySaved: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val ref = db.collection("users").document(uid).collection("savedTemplates").document(templateId)
        return try {
            if (currentlySaved) {
                ref.delete().await()
            } else {
                ref.set(
                    mapOf(
                        "userId" to uid,
                        "templateId" to templateId,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun addComment(templateId: String, username: String, text: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val ref = db.collection("templateComments").document()
        return try {
            require(text.trim().isNotEmpty()) { "Comment cannot be empty." }
            ref.set(
                mapOf(
                    "templateId" to templateId,
                    "userId" to uid,
                    "username" to username.ifBlank { "spark_creator" }.take(80),
                    "text" to text.trim().take(1000),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            db.collection("communityTemplates").document(templateId).update(
                mapOf(
                    "commentsCount" to FieldValue.increment(1),
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

    suspend fun toggleFollowCreator(creatorId: String, currentlyFollowing: Boolean, followerUsername: String = ""): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        if (creatorId == uid) return Result.success(Unit)
        val followId = "${creatorId}_$uid"
        val followRef = db.collection("creatorFollowers").document(followId)
        val creatorRef = db.collection("creatorProfiles").document(creatorId)
        return try {
            if (currentlyFollowing) {
                followRef.delete().await()
                val snap = creatorRef.get().await()
                if (snap.exists()) {
                    val count = (snap.getLong("followersCount") ?: 1L).toInt()
                    creatorRef.update(
                        mapOf(
                            "followersCount" to (count - 1).coerceAtLeast(0),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            } else {
                followRef.set(
                    mapOf(
                        "creatorId" to creatorId,
                        "followerId" to uid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                val snap = creatorRef.get().await()
                if (snap.exists()) {
                    creatorRef.update(
                        mapOf(
                            "followersCount" to FieldValue.increment(1),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
                try {
                    val label = followerUsername.ifBlank { "A Spark creator" }
                    db.collection("users").document(creatorId).collection("notifications").document().set(
                        mapOf(
                            "userId" to creatorId,
                            "title" to "New Follower",
                            "message" to "@$label started following your Spark AI studio.",
                            "category" to "follow",
                            "actorUserId" to uid,
                            "targetPostId" to "",
                            "targetCreatorId" to uid,
                            "readFlag" to false,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                } catch (_: Exception) {}
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, followRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateMyTemplateMeta(
        templateId: String,
        description: String,
        visibility: String,
        allowReuse: Boolean
    ): Result<Unit> {
        auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            db.collection("communityTemplates").document(templateId).update(
                mapOf(
                    "description" to description.take(3000),
                    "visibility" to visibility,
                    "allowReuse" to allowReuse,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            db.collection("templatePrompts").document(templateId).update(
                mapOf(
                    "visibility" to visibility,
                    "allowReuse" to allowReuse,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            db.collection("templateSettings").document(templateId).update(
                mapOf(
                    "visibility" to visibility,
                    "allowReuse" to allowReuse,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "communityTemplates/$templateId")
            Result.failure(e)
        }
    }

    suspend fun deleteTemplate(templateId: String): Result<Unit> {
        auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            db.collection("communityTemplates").document(templateId).delete().await()
            try { db.collection("templatePrompts").document(templateId).delete().await() } catch (_: Exception) {}
            try { db.collection("templateSettings").document(templateId).delete().await() } catch (_: Exception) {}
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "communityTemplates/$templateId")
            Result.failure(e)
        }
    }

    suspend fun reportTemplate(
        templateId: String,
        creatorId: String,
        reason: String,
        details: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val ref = db.collection("templateReports").document()
        return try {
            ref.set(
                mapOf(
                    "templateId" to templateId,
                    "creatorId" to creatorId,
                    "reporterId" to uid,
                    "reason" to reason.take(120),
                    "details" to details.take(1500),
                    "status" to "open",
                    "createdAt" to FieldValue.serverTimestamp()
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

    suspend fun blockCreator(creatorId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val ref = db.collection("users").document(uid).collection("blockedCreators").document(creatorId)
        return try {
            ref.set(
                mapOf(
                    "userId" to uid,
                    "creatorId" to creatorId,
                    "createdAt" to FieldValue.serverTimestamp()
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
}
