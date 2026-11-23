package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.model.CommunityTemplate
import com.example.data.model.ConversationThread
import com.example.data.model.CreatorAnalyticsSummary
import com.example.data.model.CreatorFollower
import com.example.data.model.CreatorProfile
import com.example.data.model.DirectMessage
import com.example.data.model.LiveStreamSession
import com.example.data.model.PostComment
import com.example.data.model.PostCommentLike
import com.example.data.model.PostLike
import com.example.data.model.PostRepost
import com.example.data.model.PostShare
import com.example.data.model.PostView
import com.example.data.model.SavedPostRef
import com.example.data.model.SavedSoundRef
import com.example.data.model.SocialPost
import com.example.data.model.StoryItem
import com.example.data.model.StoryView
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class SocialRepository(
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
            ?: throw IllegalStateException("Must be signed in to access Spark Social.")
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

    fun observePublicPosts(): Flow<List<SocialPost>> = flow {
        requireUserId()
        emitAll(
            db.collection("posts")
                .whereEqualTo("visibility", "public")
                .whereEqualTo("moderationStatus", "active")
                .snapshots()
                .map { snap ->
                    snap.toObjects(SocialPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, "posts")
                    throw error
                }
        )
    }

    fun observeMyPosts(uid: String): Flow<List<SocialPost>> = flow {
        requireUserId()
        emitAll(
            db.collection("posts")
                .whereEqualTo("creatorId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(SocialPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, "posts")
                    throw error
                }
        )
    }

    fun observeCreatorPublicPosts(creatorId: String): Flow<List<SocialPost>> = flow {
        requireUserId()
        emitAll(
            db.collection("posts")
                .whereEqualTo("creatorId", creatorId)
                .whereEqualTo("visibility", "public")
                .whereEqualTo("moderationStatus", "active")
                .snapshots()
                .map { snap ->
                    snap.toObjects(SocialPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, "posts")
                    throw error
                }
        )
    }

    fun observeAllCreatorProfiles(): Flow<List<CreatorProfile>> = flow {
        requireUserId()
        emitAll(
            db.collection("creatorProfiles")
                .snapshots()
                .map { snap ->
                    snap.toObjects(CreatorProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.followersCount }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, "creatorProfiles")
                    throw error
                }
        )
    }

    fun observeSavedPostIds(uid: String): Flow<Set<String>> = flow {
        requireUserId()
        emitAll(
            db.collection("users").document(uid).collection("savedPosts")
                .snapshots()
                .map { snap ->
                    snap.toObjects(SavedPostRef::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .map { it.postId }.toSet()
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, "users/$uid/savedPosts")
                    throw error
                }
        )
    }

    fun observeSavedSoundIds(uid: String): Flow<Set<String>> = flow {
        requireUserId()
        emitAll(
            db.collection("users").document(uid).collection("savedSounds")
                .snapshots()
                .map { snap ->
                    snap.toObjects(SavedSoundRef::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .map { it.soundId }.toSet()
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, "users/$uid/savedSounds")
                    throw error
                }
        )
    }

    fun observeActiveStories(uid: String): Flow<List<StoryItem>> = flow {
        requireUserId()
        emitAll(
            db.collection("stories")
                .whereIn("visibility", listOf("public", "followers"))
                .snapshots()
                .map { snap ->
                    val now = System.currentTimeMillis()
                    snap.toObjects(StoryItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { it.expiresAtMs > now || it.creatorId == uid }
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, "stories")
                    throw error
                }
        )
    }

    fun observeStoryViews(storyId: String, uid: String): Flow<List<StoryView>> = flow {
        requireUserId()
        emitAll(
            db.collection("storyViews")
                .whereEqualTo("storyId", storyId)
                .whereEqualTo("storyCreatorId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(StoryView::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, "storyViews")
                    throw error
                }
        )
    }

    fun observeConversations(uid: String): Flow<List<ConversationThread>> = flow {
        requireUserId()
        emitAll(
            db.collection("conversations")
                .whereArrayContains("participantIds", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ConversationThread::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, "conversations")
                    throw error
                }
        )
    }

    fun observeMessages(conversationId: String): Flow<List<DirectMessage>> = flow {
        requireUserId()
        emitAll(
            db.collection("conversations").document(conversationId).collection("messages")
                .snapshots()
                .map { snap ->
                    snap.toObjects(DirectMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, "conversations/$conversationId/messages")
                    throw error
                }
        )
    }

    fun observeLiveStreams(): Flow<List<LiveStreamSession>> = flow {
        requireUserId()
        emitAll(
            db.collection("liveStreams")
                .snapshots()
                .map { snap ->
                    snap.toObjects(LiveStreamSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, "liveStreams")
                    throw error
                }
        )
    }

    fun observePostViews(postId: String, isOwnerOrAdmin: Boolean, uid: String): Flow<List<PostView>> = flow {
        requireUserId()
        val base = db.collection("postViews").whereEqualTo("postId", postId)
        val query = if (isOwnerOrAdmin) {
            base.whereEqualTo("postCreatorId", uid)
        } else {
            base.whereEqualTo("viewerPublic", true)
        }
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(PostView::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.lastViewedAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, "postViews")
                    throw error
                }
        )
    }

    fun observePostLikes(postId: String, isOwnerOrAdmin: Boolean, uid: String): Flow<List<PostLike>> = flow {
        requireUserId()
        val base = db.collection("postLikes").whereEqualTo("postId", postId)
        val query = if (isOwnerOrAdmin) {
            base.whereEqualTo("postCreatorId", uid)
        } else {
            base.whereEqualTo("userPublic", true)
        }
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(PostLike::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, "postLikes")
                    throw error
                }
        )
    }

    fun observeMyLikedPostIds(uid: String): Flow<Set<String>> = flow {
        requireUserId()
        emitAll(
            db.collection("postLikes")
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(PostLike::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .map { it.postId }.toSet()
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, "postLikes")
                    throw error
                }
        )
    }

    fun observePostComments(postId: String): Flow<List<PostComment>> = flow {
        requireUserId()
        emitAll(
            db.collection("postComments")
                .whereEqualTo("postId", postId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(PostComment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, "postComments")
                    throw error
                }
        )
    }

    fun observeMyLikedCommentIds(uid: String): Flow<Set<String>> = flow {
        requireUserId()
        emitAll(
            db.collection("postCommentLikes")
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(PostCommentLike::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .map { it.commentId }.toSet()
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, "postCommentLikes")
                    throw error
                }
        )
    }

    fun observePostShares(postId: String, postCreatorId: String): Flow<List<PostShare>> = flow {
        val uid = requireUserId()
        val query = if (uid == postCreatorId) {
            db.collection("postShares").whereEqualTo("postId", postId).whereEqualTo("postCreatorId", postCreatorId)
        } else {
            db.collection("postShares").whereEqualTo("postId", postId).whereEqualTo("sharerUserId", uid)
        }
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(PostShare::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, "postShares")
                    throw error
                }
        )
    }

    fun observeFollowersOf(creatorId: String): Flow<List<CreatorFollower>> = flow {
        requireUserId()
        emitAll(
            db.collection("creatorFollowers")
                .whereEqualTo("creatorId", creatorId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(CreatorFollower::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, "creatorFollowers")
                    throw error
                }
        )
    }

    suspend fun ensureCreatorProfile(
        username: String,
        displayName: String,
        avatarUrl: String,
        bio: String = "Spark AI & Spark Social Creator",
        coverImageData: String? = null
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val ref = db.collection("creatorProfiles").document(uid)
        return try {
            val snap = ref.get().await()
            if (!snap.exists()) {
                ref.set(
                    mapOf(
                        "creatorId" to uid,
                        "username" to username.ifBlank { "spark_creator" }.take(80),
                        "displayName" to displayName.ifBlank { username }.take(120),
                        "avatarUrl" to avatarUrl.take(2000),
                        "bio" to bio.take(500),
                        "coverImageData" to (coverImageData ?: "").take(220000),
                        "publishedCount" to 0,
                        "followersCount" to 0,
                        "followingCount" to 0,
                        "totalLikes" to 0,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val updates = mutableMapOf<String, Any>(
                    "username" to username.ifBlank { "spark_creator" }.take(80),
                    "displayName" to displayName.ifBlank { username }.take(120),
                    "bio" to bio.take(500),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (coverImageData != null) {
                    updates["coverImageData"] = coverImageData.take(220000)
                }
                ref.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun publishPost(post: SocialPost): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val cleanTitle = when (val r = com.example.core.SecurityValidator.validateUserInput(post.title, "Post title", 200)) {
            is com.example.core.ValidationResult.Valid -> r.sanitized
            is com.example.core.ValidationResult.Invalid -> return Result.failure(IllegalArgumentException(r.reason))
        }
        val cleanCaption = when (val r = com.example.core.SecurityValidator.validateUserInput(post.caption, "Post caption", 3000, allowEmpty = true)) {
            is com.example.core.ValidationResult.Valid -> r.sanitized
            is com.example.core.ValidationResult.Invalid -> return Result.failure(IllegalArgumentException(r.reason))
        }
        if (!com.example.core.SecurityValidator.isSafeMediaOrWebUrl(post.previewVideoUrl, allowEmpty = true)) {
            return Result.failure(IllegalArgumentException("Blocked unsafe video URL scheme."))
        }
        val ref = db.collection("posts").document()
        return try {
            ref.set(
                mapOf(
                    "creatorId" to uid,
                    "creatorUsername" to post.creatorUsername.ifBlank { "spark_creator" }.take(80),
                    "creatorAvatarUrl" to post.creatorAvatarUrl.take(2000),
                    "creationType" to post.creationType,
                    "mediaType" to post.mediaType,
                    "title" to cleanTitle,
                    "caption" to cleanCaption,
                    "hashtags" to com.example.core.SecurityValidator.sanitizePlainText(post.hashtags, 500),
                    "mentions" to com.example.core.SecurityValidator.sanitizePlainText(post.mentions, 500),
                    "category" to post.category.ifBlank { "Movie scenes" }.take(80),
                    "previewVideoUrl" to post.previewVideoUrl.take(2000),
                    "thumbnailData" to post.thumbnailData.take(340000),
                    "photoThumbnails" to post.photoThumbnails.take(6).map { it.take(60000) },
                    "soundId" to post.soundId.take(128),
                    "soundTitle" to post.soundTitle.take(200),
                    "durationSec" to post.durationSec.coerceIn(0, 14400),
                    "aspectRatio" to post.aspectRatio.take(20),
                    "resolution" to post.resolution.take(20),
                    "visibility" to post.visibility,
                    "allowComments" to post.allowComments,
                    "allowReuse" to post.allowReuse,
                    "allowDownload" to post.allowDownload,
                    "viewsCount" to 0,
                    "uniqueViewersCount" to 0,
                    "likesCount" to 0,
                    "commentsCount" to 0,
                    "sharesCount" to 0,
                    "repostsCount" to 0,
                    "savesCount" to 0,
                    "moderationStatus" to "active",
                    "linkedProjectId" to post.linkedProjectId.take(128),
                    "linkedTemplateId" to post.linkedTemplateId.take(128),
                    "originalCreatorName" to post.originalCreatorName.take(100),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            ensureCreatorProfile(post.creatorUsername, post.creatorUsername, post.creatorAvatarUrl)
            db.collection("creatorProfiles").document(uid).update(
                mapOf(
                    "publishedCount" to FieldValue.increment(1),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun updatePost(
        postId: String,
        title: String,
        caption: String,
        hashtags: String,
        visibility: String,
        allowComments: Boolean,
        allowReuse: Boolean,
        allowDownload: Boolean
    ): Result<Unit> {
        auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val cleanTitle = when (val r = com.example.core.SecurityValidator.validateUserInput(title, "Post title", 200)) {
            is com.example.core.ValidationResult.Valid -> r.sanitized
            is com.example.core.ValidationResult.Invalid -> return Result.failure(IllegalArgumentException(r.reason))
        }
        val cleanCaption = when (val r = com.example.core.SecurityValidator.validateUserInput(caption, "Post caption", 3000, allowEmpty = true)) {
            is com.example.core.ValidationResult.Valid -> r.sanitized
            is com.example.core.ValidationResult.Invalid -> return Result.failure(IllegalArgumentException(r.reason))
        }
        val ref = db.collection("posts").document(postId)
        return try {
            ref.update(
                mapOf(
                    "title" to cleanTitle,
                    "caption" to cleanCaption,
                    "hashtags" to com.example.core.SecurityValidator.sanitizePlainText(hashtags, 500),
                    "visibility" to visibility,
                    "allowComments" to allowComments,
                    "allowReuse" to allowReuse,
                    "allowDownload" to allowDownload,
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

    suspend fun replacePostVideo(
        existingPost: SocialPost,
        newVideoUrl: String,
        newThumbnailData: String,
        newDurationSec: Int,
        newResolution: String,
        keepOriginalLink: Boolean,
        newTitle: String = existingPost.title,
        newCaption: String = existingPost.caption
    ): Result<String> {
        auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        if (!com.example.core.SecurityValidator.isSafeMediaOrWebUrl(newVideoUrl, allowEmpty = false)) {
            return Result.failure(IllegalArgumentException("Please select a valid local or HTTPS video source."))
        }
        return if (keepOriginalLink) {
            val ref = db.collection("posts").document(existingPost.id)
            try {
                ref.update(
                    mapOf(
                        "title" to com.example.core.SecurityValidator.sanitizePlainText(newTitle.ifBlank { existingPost.title }, 200),
                        "caption" to com.example.core.SecurityValidator.sanitizePlainText(newCaption, 3000),
                        "previewVideoUrl" to newVideoUrl.take(2000),
                        "thumbnailData" to newThumbnailData.ifBlank { existingPost.thumbnailData }.take(340000),
                        "durationSec" to newDurationSec.coerceIn(1, 14400),
                        "resolution" to newResolution.ifBlank { existingPost.resolution }.take(20),
                        "mediaType" to "video",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                Result.success(existingPost.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, ref.path)
                Result.failure(e)
            }
        } else {
            val fresh = existingPost.copy(
                id = "",
                title = newTitle.ifBlank { existingPost.title },
                caption = newCaption,
                previewVideoUrl = newVideoUrl,
                thumbnailData = newThumbnailData.ifBlank { existingPost.thumbnailData },
                durationSec = newDurationSec.coerceIn(1, 14400),
                resolution = newResolution.ifBlank { existingPost.resolution },
                mediaType = "video"
            )
            publishPost(fresh)
        }
    }

    suspend fun changePostVisibility(postId: String, visibility: String): Result<Unit> {
        auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val ref = db.collection("posts").document(postId)
        return try {
            ref.update(mapOf("visibility" to visibility, "updatedAt" to FieldValue.serverTimestamp())).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun togglePostComments(postId: String, allowComments: Boolean): Result<Unit> {
        auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val ref = db.collection("posts").document(postId)
        return try {
            ref.update(mapOf("allowComments" to allowComments, "updatedAt" to FieldValue.serverTimestamp())).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun updatePostTemplatePermission(postId: String, allowReuse: Boolean): Result<Unit> {
        auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val ref = db.collection("posts").document(postId)
        return try {
            ref.update(mapOf("allowReuse" to allowReuse, "updatedAt" to FieldValue.serverTimestamp())).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun softDeletePostToRecycleBin(post: SocialPost): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val postRef = db.collection("posts").document(post.id)
        val trashRef = db.collection("users").document(uid).collection("recentlyDeleted").document("post_${post.id}")
        return try {
            val nowMs = System.currentTimeMillis()
            val expiresMs = nowMs + 30L * 24L * 60L * 60L * 1000L
            val json = org.json.JSONObject().apply {
                put("creatorUsername", post.creatorUsername)
                put("creatorAvatarUrl", post.creatorAvatarUrl)
                put("creationType", post.creationType)
                put("mediaType", post.mediaType)
                put("title", post.title)
                put("caption", post.caption)
                put("hashtags", post.hashtags)
                put("mentions", post.mentions)
                put("category", post.category)
                put("previewVideoUrl", post.previewVideoUrl)
                put("soundId", post.soundId)
                put("soundTitle", post.soundTitle)
                put("durationSec", post.durationSec)
                put("aspectRatio", post.aspectRatio)
                put("resolution", post.resolution)
                put("visibility", post.visibility)
                put("allowComments", post.allowComments)
                put("allowReuse", post.allowReuse)
                put("allowDownload", post.allowDownload)
                put("linkedProjectId", post.linkedProjectId)
                put("linkedTemplateId", post.linkedTemplateId)
                put("originalCreatorName", post.originalCreatorName)
            }.toString()

            trashRef.set(
                mapOf(
                    "userId" to uid,
                    "itemType" to "post",
                    "originalId" to post.id,
                    "title" to post.title.ifBlank { "Deleted Post" }.take(200),
                    "subtitle" to "${post.creationType} • ${post.visibility}".take(160),
                    "creationType" to post.creationType.take(40),
                    "previewVideoUrl" to post.previewVideoUrl.take(2000),
                    "thumbnailData" to post.thumbnailData.take(250000),
                    "payloadJson" to json.take(60000),
                    "deletedAtMs" to nowMs,
                    "expiresAtMs" to expiresMs,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            postRef.delete().await()

            val profileRef = db.collection("creatorProfiles").document(uid)
            val snap = profileRef.get().await()
            if (snap.exists()) {
                val curCount = (snap.getLong("publishedCount") ?: 0L).toInt()
                profileRef.update(
                    mapOf(
                        "publishedCount" to (curCount - 1).coerceAtLeast(0),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, postRef.path)
            Result.failure(e)
        }
    }

    suspend fun deletePost(postId: String): Result<Unit> {
        auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val ref = db.collection("posts").document(postId)
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

    suspend fun toggleSavePost(post: SocialPost, currentlySaved: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val saveRef = db.collection("users").document(uid).collection("savedPosts").document(post.id)
        val postRef = db.collection("posts").document(post.id)
        return try {
            if (currentlySaved) {
                saveRef.delete().await()
                postRef.update("savesCount", (post.savesCount - 1).coerceAtLeast(0), "updatedAt", FieldValue.serverTimestamp()).await()
            } else {
                saveRef.set(mapOf("userId" to uid, "postId" to post.id, "createdAt" to FieldValue.serverTimestamp())).await()
                postRef.update("savesCount", FieldValue.increment(1), "updatedAt", FieldValue.serverTimestamp()).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, saveRef.path)
            Result.failure(e)
        }
    }

    suspend fun repostPost(post: SocialPost, reposterUsername: String, note: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val ref = db.collection("postReposts").document("${post.id}_$uid")
        return try {
            ref.set(
                mapOf(
                    "postId" to post.id,
                    "originalCreatorId" to post.creatorId,
                    "reposterUserId" to uid,
                    "reposterUsername" to reposterUsername.ifBlank { "spark_creator" }.take(80),
                    "note" to note.take(400),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            db.collection("posts").document(post.id).update(
                mapOf("repostsCount" to FieldValue.increment(1), "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            if (post.creatorId != uid) {
                sendNotification(
                    recipientId = post.creatorId,
                    title = "Post Reposted",
                    message = "@${reposterUsername.ifBlank { "creator" }} reposted \"${post.title}\".",
                    category = "share",
                    targetPostId = post.id,
                    targetCreatorId = uid
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun toggleSaveSound(soundId: String, soundTitle: String, currentlySaved: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val ref = db.collection("users").document(uid).collection("savedSounds").document(soundId)
        return try {
            if (currentlySaved) {
                ref.delete().await()
            } else {
                ref.set(
                    mapOf(
                        "userId" to uid,
                        "soundId" to soundId,
                        "soundTitle" to soundTitle.take(200),
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

    suspend fun createStory(story: StoryItem): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val ref = db.collection("stories").document()
        return try {
            val expiresAt = System.currentTimeMillis() + 24L * 60L * 60L * 1000L
            ref.set(
                mapOf(
                    "creatorId" to uid,
                    "creatorUsername" to story.creatorUsername.ifBlank { "spark_creator" }.take(80),
                    "creatorAvatarUrl" to story.creatorAvatarUrl.take(2000),
                    "mediaType" to story.mediaType,
                    "mediaUrl" to story.mediaUrl.take(2000),
                    "thumbnailData" to story.thumbnailData.take(300000),
                    "overlayText" to story.overlayText.take(300),
                    "stickerTag" to story.stickerTag.take(80),
                    "caption" to story.caption.take(400),
                    "musicTrackTitle" to story.musicTrackTitle.take(200),
                    "visibility" to story.visibility,
                    "viewsCount" to 0,
                    "expiresAtMs" to expiresAt,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun recordStoryView(
        story: StoryItem,
        viewerUsername: String,
        viewerAvatarUrl: String,
        viewerPublic: Boolean
    ) {
        val uid = auth.currentUser?.uid ?: return
        val ref = db.collection("storyViews").document("${story.id}_$uid")
        try {
            val snap = ref.get().await()
            if (!snap.exists()) {
                ref.set(
                    mapOf(
                        "storyId" to story.id,
                        "storyCreatorId" to story.creatorId,
                        "viewerUserId" to uid,
                        "viewerUsername" to if (viewerPublic) viewerUsername.take(80) else "Private Viewer",
                        "viewerAvatarUrl" to if (viewerPublic) viewerAvatarUrl.take(2000) else "",
                        "viewerPublic" to viewerPublic,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                db.collection("stories").document(story.id)
                    .update("viewsCount", FieldValue.increment(1)).await()
            }
        } catch (_: Exception) {
        }
    }

    suspend fun deleteStory(storyId: String): Result<Unit> {
        auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val ref = db.collection("stories").document(storyId)
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

    suspend fun getOrCreateConversation(
        peerUserId: String,
        myUsername: String,
        peerUsername: String
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        if (peerUserId.isBlank() || peerUserId == uid) {
            return Result.failure(IllegalArgumentException("Select another creator to message."))
        }
        val sorted = listOf(uid, peerUserId).sorted()
        val convId = "${sorted[0]}_${sorted[1]}"
        val ref = db.collection("conversations").document(convId)
        return try {
            val snap = ref.get().await()
            if (!snap.exists()) {
                ref.set(
                    mapOf(
                        "participantIds" to sorted,
                        "participantNames" to mapOf(
                            uid to myUsername.ifBlank { "Me" },
                            peerUserId to peerUsername.ifBlank { "Creator" }
                        ),
                        "lastMessageText" to "Conversation started",
                        "lastSenderId" to uid,
                        "lastMessageType" to "text",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(convId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun sendDirectMessage(
        conversationId: String,
        receiverId: String,
        senderUsername: String,
        messageType: String,
        text: String,
        mediaUrl: String = "",
        mediaThumbnailData: String = "",
        audioDurationSec: Int = 0,
        replyToMessageId: String = "",
        replyToPreview: String = ""
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val msgRef = db.collection("conversations").document(conversationId).collection("messages").document()
        return try {
            msgRef.set(
                mapOf(
                    "conversationId" to conversationId,
                    "senderId" to uid,
                    "senderUsername" to senderUsername.ifBlank { "spark_creator" }.take(80),
                    "receiverId" to receiverId,
                    "messageType" to messageType,
                    "text" to text.trim().take(3000),
                    "mediaUrl" to mediaUrl.take(250000),
                    "mediaThumbnailData" to mediaThumbnailData.take(220000),
                    "audioDurationSec" to audioDurationSec.coerceAtLeast(0),
                    "replyToMessageId" to replyToMessageId.take(128),
                    "replyToPreview" to replyToPreview.take(120),
                    "deliveryStatus" to "sent",
                    "deletedForAll" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val preview = when (messageType) {
                "voice" -> "Voice message (${audioDurationSec}s)"
                "image" -> "Photo attachment"
                "video" -> "Video attachment"
                "audio" -> "Audio attachment"
                else -> text.trim().take(80)
            }
            db.collection("conversations").document(conversationId).update(
                mapOf(
                    "lastMessageText" to preview,
                    "lastSenderId" to uid,
                    "lastMessageType" to messageType,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            sendNotification(
                recipientId = receiverId,
                title = "New Direct Message",
                message = "@${senderUsername.ifBlank { "creator" }}: $preview",
                category = "message",
                targetCreatorId = uid
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, msgRef.path)
            Result.failure(e)
        }
    }

    suspend fun markMessagesRead(conversationId: String, messages: List<DirectMessage>) {
        val uid = auth.currentUser?.uid ?: return
        for (m in messages) {
            if (m.receiverId == uid && m.deliveryStatus != "read" && !m.deletedForAll) {
                try {
                    db.collection("conversations").document(conversationId)
                        .collection("messages").document(m.id)
                        .update("deliveryStatus", "read").await()
                } catch (_: Exception) {
                }
            }
        }
    }

    suspend fun deleteDirectMessage(conversationId: String, messageId: String): Result<Unit> {
        auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val ref = db.collection("conversations").document(conversationId).collection("messages").document(messageId)
        return try {
            ref.update(
                mapOf(
                    "deletedForAll" to true,
                    "text" to "Message deleted"
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

    suspend fun recordPostView(
        post: SocialPost,
        viewerUsername: String,
        viewerAvatarUrl: String,
        viewerPublic: Boolean
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val viewId = "${post.id}_$uid"
        val viewRef = db.collection("postViews").document(viewId)
        val postRef = db.collection("posts").document(post.id)
        return try {
            val existing = viewRef.get().await()
            if (!existing.exists()) {
                viewRef.set(
                    mapOf(
                        "postId" to post.id,
                        "postCreatorId" to post.creatorId,
                        "viewerUserId" to uid,
                        "viewerUsername" to if (viewerPublic) viewerUsername.take(80) else "Private Viewer",
                        "viewerAvatarUrl" to if (viewerPublic) viewerAvatarUrl.take(2000) else "",
                        "viewerPublic" to viewerPublic,
                        "viewCount" to 1,
                        "lastViewedAt" to FieldValue.serverTimestamp(),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                postRef.update(
                    mapOf(
                        "viewsCount" to FieldValue.increment(1),
                        "uniqueViewersCount" to FieldValue.increment(1),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                viewRef.update(
                    mapOf(
                        "viewCount" to FieldValue.increment(1),
                        "lastViewedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                postRef.update(
                    mapOf(
                        "viewsCount" to FieldValue.increment(1),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, viewRef.path)
            Result.failure(e)
        }
    }

    suspend fun togglePostLike(
        post: SocialPost,
        username: String,
        avatarUrl: String,
        userPublic: Boolean,
        currentlyLiked: Boolean
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val likeRef = db.collection("postLikes").document("${post.id}_$uid")
        val postRef = db.collection("posts").document(post.id)
        return try {
            if (currentlyLiked) {
                likeRef.delete().await()
                postRef.update(
                    mapOf(
                        "likesCount" to (post.likesCount - 1).coerceAtLeast(0),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                likeRef.set(
                    mapOf(
                        "postId" to post.id,
                        "postCreatorId" to post.creatorId,
                        "userId" to uid,
                        "username" to if (userPublic) username.ifBlank { "spark_creator" }.take(80) else "Spark Member",
                        "avatarUrl" to if (userPublic) avatarUrl.take(2000) else "",
                        "userPublic" to userPublic,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                postRef.update(
                    mapOf(
                        "likesCount" to FieldValue.increment(1),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                if (post.creatorId != uid) {
                    sendNotification(
                        recipientId = post.creatorId,
                        title = "New Like on ${post.title}",
                        message = "@${username.ifBlank { "creator" }} liked your post.",
                        category = "like",
                        targetPostId = post.id,
                        targetCreatorId = uid
                    )
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, likeRef.path)
            Result.failure(e)
        }
    }

    suspend fun addPostComment(
        post: SocialPost,
        parentCommentId: String,
        username: String,
        text: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val ref = db.collection("postComments").document()
        return try {
            require(post.allowComments) { "Comments are disabled on this post." }
            require(text.trim().isNotEmpty()) { "Comment cannot be empty." }
            ref.set(
                mapOf(
                    "postId" to post.id,
                    "postCreatorId" to post.creatorId,
                    "parentCommentId" to parentCommentId.take(128),
                    "userId" to uid,
                    "username" to username.ifBlank { "spark_creator" }.take(80),
                    "text" to text.trim().take(1000),
                    "likesCount" to 0,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            db.collection("posts").document(post.id).update(
                mapOf(
                    "commentsCount" to FieldValue.increment(1),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            if (post.creatorId != uid) {
                sendNotification(
                    recipientId = post.creatorId,
                    title = if (parentCommentId.isBlank()) "New Comment" else "New Reply",
                    message = "@${username.ifBlank { "creator" }} commented on \"${post.title}\": ${text.trim().take(80)}",
                    category = "comment",
                    targetPostId = post.id,
                    targetCreatorId = uid
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun toggleCommentLike(comment: PostComment, currentlyLiked: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val likeRef = db.collection("postCommentLikes").document("${comment.id}_$uid")
        val commentRef = db.collection("postComments").document(comment.id)
        return try {
            if (currentlyLiked) {
                likeRef.delete().await()
                commentRef.update("likesCount", (comment.likesCount - 1).coerceAtLeast(0)).await()
            } else {
                likeRef.set(
                    mapOf(
                        "commentId" to comment.id,
                        "userId" to uid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                commentRef.update("likesCount", FieldValue.increment(1)).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, likeRef.path)
            Result.failure(e)
        }
    }

    suspend fun deletePostComment(post: SocialPost, commentId: String): Result<Unit> {
        auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val ref = db.collection("postComments").document(commentId)
        return try {
            ref.delete().await()
            db.collection("posts").document(post.id).update(
                mapOf(
                    "commentsCount" to (post.commentsCount - 1).coerceAtLeast(0),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun recordPostShare(
        post: SocialPost,
        channel: String,
        sharerUsername: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val shareRef = db.collection("postShares").document()
        return try {
            shareRef.set(
                mapOf(
                    "postId" to post.id,
                    "postCreatorId" to post.creatorId,
                    "sharerUserId" to uid,
                    "channel" to channel.take(60),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            db.collection("posts").document(post.id).update(
                mapOf(
                    "sharesCount" to FieldValue.increment(1),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            if (post.creatorId != uid) {
                sendNotification(
                    recipientId = post.creatorId,
                    title = "Post Shared",
                    message = "@${sharerUsername.ifBlank { "creator" }} shared \"${post.title}\" ($channel).",
                    category = "share",
                    targetPostId = post.id,
                    targetCreatorId = uid
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, shareRef.path)
            Result.failure(e)
        }
    }

    suspend fun sendNotification(
        recipientId: String,
        title: String,
        message: String,
        category: String,
        targetPostId: String = "",
        targetCreatorId: String = ""
    ) {
        val actorUid = auth.currentUser?.uid ?: return
        if (recipientId.isBlank()) return
        try {
            db.collection("users").document(recipientId).collection("notifications").document().set(
                mapOf(
                    "userId" to recipientId,
                    "title" to title.take(150),
                    "message" to message.take(1000),
                    "category" to category.take(60),
                    "actorUserId" to actorUid,
                    "targetPostId" to targetPostId.take(128),
                    "targetCreatorId" to targetCreatorId.take(128),
                    "readFlag" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (_: Exception) {
        }
    }

    suspend fun getPostById(postId: String): Result<SocialPost?> {
        if (postId.isBlank()) return Result.success(null)
        val ref = db.collection("posts").document(postId)
        return try {
            val snap = ref.get().await()
            val post = snap.toObject(SocialPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?.let { if (it.id.isBlank()) it.copy(id = snap.id) else it }
            Result.success(post)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, ref.path)
            Result.failure(e)
        }
    }

    fun buildAnalyticsSummary(
        myPosts: List<SocialPost>,
        myTemplates: List<CommunityTemplate>,
        followersCount: Int
    ): CreatorAnalyticsSummary {
        return CreatorAnalyticsSummary(
            totalPosts = myPosts.size,
            totalViews = myPosts.sumOf { it.viewsCount },
            totalUniqueViewers = myPosts.sumOf { it.uniqueViewersCount },
            totalLikes = myPosts.sumOf { it.likesCount } + myTemplates.sumOf { it.likesCount },
            totalComments = myPosts.sumOf { it.commentsCount } + myTemplates.sumOf { it.commentsCount },
            totalShares = myPosts.sumOf { it.sharesCount },
            totalTemplates = myTemplates.size,
            totalRemixes = myTemplates.sumOf { it.remixesCount },
            followersCount = followersCount,
            topPerformingPost = myPosts.maxByOrNull { it.viewsCount + (it.likesCount * 3) + (it.sharesCount * 5) }
        )
    }
}
