package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.OperationType
import com.example.core.SecurityValidator
import com.example.core.ValidationResult
import com.example.core.handleFirestoreError
import com.example.data.model.ActiveSessionItem
import com.example.data.model.AuditLogItem
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import com.example.data.model.SecurityAlertItem
import com.example.data.model.SparkGuideArticle
import com.example.data.model.SparkGuideConversation
import com.example.data.model.SparkGuideMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class GuideAndSecurityRepository(
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
            ?: throw IllegalStateException("Must be signed in to access Spark AI.")
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

    // ---- Spark Guide Knowledge Base (Admin Managed, All Signed-In Users Read) ----

    fun observeGuideArticles(): Flow<List<SparkGuideArticle>> = flow {
        requireUserId()
        emitAll(
            db.collection("sparkGuideArticles")
                .snapshots()
                .map { snap ->
                    snap.toObjects(SparkGuideArticle::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, "sparkGuideArticles")
                    throw error
                }
        )
    }

    suspend fun upsertGuideArticle(article: SparkGuideArticle): Result<String> {
        val adminUid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val cleanTitle = when (val r = SecurityValidator.validateUserInput(article.title, "Article title", 200)) {
            is ValidationResult.Valid -> r.sanitized
            is ValidationResult.Invalid -> return Result.failure(IllegalArgumentException(r.reason))
        }
        val cleanSteps = when (val r = SecurityValidator.validateUserInput(article.stepsSummary, "Article guide steps", 4000)) {
            is ValidationResult.Valid -> r.sanitized
            is ValidationResult.Invalid -> return Result.failure(IllegalArgumentException(r.reason))
        }
        val col = db.collection("sparkGuideArticles")
        val ref = if (article.id.isBlank()) col.document() else col.document(article.id)
        return try {
            if (article.id.isBlank()) {
                ref.set(
                    mapOf(
                        "title" to cleanTitle,
                        "category" to SecurityValidator.sanitizePlainText(article.category, 80).ifBlank { "Getting Started" },
                        "stepsSummary" to cleanSteps,
                        "keywords" to SecurityValidator.sanitizePlainText(article.keywords, 400),
                        "published" to article.published,
                        "updatedBy" to adminUid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.update(
                    mapOf(
                        "title" to cleanTitle,
                        "category" to SecurityValidator.sanitizePlainText(article.category, 80).ifBlank { "Getting Started" },
                        "stepsSummary" to cleanSteps,
                        "keywords" to SecurityValidator.sanitizePlainText(article.keywords, 400),
                        "published" to article.published,
                        "updatedBy" to adminUid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            logAudit("upsert_guide_article", "sparkGuideArticles", ref.id, "Updated Spark Guide article: $cleanTitle")
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun deleteGuideArticle(articleId: String): Result<Unit> {
        auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val ref = db.collection("sparkGuideArticles").document(articleId)
        return try {
            ref.delete().await()
            logAudit("delete_guide_article", "sparkGuideArticles", articleId, "Deleted Spark Guide help article")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, ref.path)
            Result.failure(e)
        }
    }

    // ---- Spark Guide Conversations & History (User-Scoped) ----

    fun observeConversations(uid: String): Flow<List<SparkGuideConversation>> = flow {
        requireUserId()
        emitAll(
            db.collection("users").document(uid).collection("sparkGuideChats")
                .snapshots()
                .map { snap ->
                    snap.toObjects(SparkGuideConversation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, "users/$uid/sparkGuideChats")
                    throw error
                }
        )
    }

    fun observeMessages(uid: String, conversationId: String): Flow<List<SparkGuideMessage>> = flow {
        requireUserId()
        emitAll(
            db.collection("users").document(uid).collection("sparkGuideChats")
                .document(conversationId).collection("messages")
                .snapshots()
                .map { snap ->
                    snap.toObjects(SparkGuideMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, "users/$uid/sparkGuideChats/$conversationId/messages")
                    throw error
                }
        )
    }

    suspend fun saveGuideTurn(
        conversationId: String?,
        title: String,
        contextSection: String,
        language: String,
        userText: String,
        assistantText: String,
        offlineFallback: Boolean
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val chatsCol = db.collection("users").document(uid).collection("sparkGuideChats")
        val convRef = if (conversationId.isNullOrBlank()) chatsCol.document() else chatsCol.document(conversationId)
        return try {
            val cleanTitle = SecurityValidator.sanitizePlainText(title.ifBlank { userText }, 120)
                .ifBlank { "Spark Guide Chat" }
            val existing = if (!conversationId.isNullOrBlank()) convRef.get().await().exists() else false
            if (!existing) {
                convRef.set(
                    mapOf(
                        "userId" to uid,
                        "title" to cleanTitle,
                        "contextSection" to contextSection.take(60),
                        "language" to language.take(40),
                        "lastMessagePreview" to assistantText.take(180),
                        "messageCount" to 2,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                convRef.update(
                    mapOf(
                        "language" to language.take(40),
                        "lastMessagePreview" to assistantText.take(180),
                        "messageCount" to FieldValue.increment(2),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            val msgCol = convRef.collection("messages")
            msgCol.document().set(
                mapOf(
                    "conversationId" to convRef.id,
                    "userId" to uid,
                    "role" to "user",
                    "text" to SecurityValidator.sanitizePlainText(userText, 2000),
                    "offlineFallback" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            msgCol.document().set(
                mapOf(
                    "conversationId" to convRef.id,
                    "userId" to uid,
                    "role" to "assistant",
                    "text" to assistantText.take(8000),
                    "offlineFallback" to offlineFallback,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(convRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, convRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteGuideConversation(conversationId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val convRef = db.collection("users").document(uid).collection("sparkGuideChats").document(conversationId)
        return try {
            val msgs = convRef.collection("messages").get().await()
            for (m in msgs.documents) {
                m.reference.delete().await()
            }
            convRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, convRef.path)
            Result.failure(e)
        }
    }

    // ---- Cybersecurity: Active Device Sessions, Alerts & Audit Logs ----

    suspend fun registerOrHeartbeatDeviceSession(
        sessionId: String,
        deviceLabel: String,
        email: String,
        username: String
    ): Result<Boolean> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val sessRef = db.collection("users").document(uid).collection("activeSessions").document(sessionId)
        return try {
            val existing = sessRef.get().await()
            val isNewDevice = !existing.exists()
            val existingStatus = existing.getString("status") ?: "active"
            if (!isNewDevice && existingStatus == "revoked") {
                return Result.success(false)
            }
            if (isNewDevice) {
                sessRef.set(
                    mapOf(
                        "userId" to uid,
                        "userEmail" to email.take(120),
                        "username" to username.ifBlank { "creator" }.take(80),
                        "deviceLabel" to deviceLabel.take(160),
                        "platform" to "Android",
                        "status" to "active",
                        "currentSession" to true,
                        "lastActiveAt" to FieldValue.serverTimestamp(),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                logSecurityEvent(uid, "Signed in on $deviceLabel", deviceLabel, "verified")
                logSecurityAlert(
                    targetUserId = uid,
                    username = username,
                    alertType = "new_device",
                    severity = "low",
                    title = "New Device Login Detected",
                    description = "Your Spark AI account signed in from $deviceLabel.",
                    deviceLabel = deviceLabel
                )
            } else {
                sessRef.update(
                    mapOf(
                        "deviceLabel" to deviceLabel.take(160),
                        "username" to username.ifBlank { "creator" }.take(80),
                        "status" to "active",
                        "lastActiveAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, sessRef.path)
            Result.failure(e)
        }
    }

    fun observeUserActiveSessions(uid: String): Flow<List<ActiveSessionItem>> = flow {
        requireUserId()
        emitAll(
            db.collection("users").document(uid).collection("activeSessions")
                .snapshots()
                .map { snap ->
                    snap.toObjects(ActiveSessionItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.lastActiveAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, "users/$uid/activeSessions")
                    throw error
                }
        )
    }

    fun observeCurrentSessionStatus(uid: String, sessionId: String): Flow<String> = flow {
        requireUserId()
        emitAll(
            db.collection("users").document(uid).collection("activeSessions").document(sessionId)
                .snapshots()
                .map { snap ->
                    if (!snap.exists()) "active" else (snap.getString("status") ?: "active")
                }
                .catch { error ->
                    logStreamError(error, OperationType.GET, "users/$uid/activeSessions/$sessionId")
                    emit("active")
                }
        )
    }

    suspend fun revokeSession(targetUserId: String, sessionId: String, reason: String = "Session revoked by user"): Result<Unit> {
        auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val ref = db.collection("users").document(targetUserId).collection("activeSessions").document(sessionId)
        return try {
            ref.update(
                mapOf(
                    "status" to "revoked",
                    "lastActiveAt" to FieldValue.serverTimestamp()
                )
            ).await()
            logSecurityEvent(targetUserId, "Session revoked ($sessionId)", reason, "revoked")
            logAudit("revoke_session", "users/$targetUserId/activeSessions", sessionId, reason)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun revokeAllOtherSessions(uid: String, currentSessionId: String): Result<Int> {
        auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val snap = db.collection("users").document(uid).collection("activeSessions").get().await()
            var count = 0
            for (doc in snap.documents) {
                if (doc.id != currentSessionId && doc.getString("status") != "revoked") {
                    doc.reference.update(
                        mapOf("status" to "revoked", "lastActiveAt" to FieldValue.serverTimestamp())
                    ).await()
                    count++
                }
            }
            logSecurityEvent(uid, "Logged out $count other device sessions", "Security Center Remote Logout", "verified")
            Result.success(count)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid/activeSessions")
            Result.failure(e)
        }
    }

    fun observeUserSecurityAlerts(uid: String): Flow<List<SecurityAlertItem>> = flow {
        requireUserId()
        emitAll(
            db.collection("securityAlerts")
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(SecurityAlertItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, "securityAlerts")
                    throw error
                }
        )
    }

    fun observeAllSecurityAlerts(): Flow<List<SecurityAlertItem>> = flow {
        requireUserId()
        emitAll(
            db.collection("securityAlerts")
                .snapshots()
                .map { snap ->
                    snap.toObjects(SecurityAlertItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, "securityAlerts")
                    throw error
                }
        )
    }

    fun observeAllActiveSessions(): Flow<List<ActiveSessionItem>> = flow {
        requireUserId()
        emitAll(
            db.collectionGroup("activeSessions")
                .snapshots()
                .map { snap ->
                    snap.toObjects(ActiveSessionItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.lastActiveAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, "collectionGroup/activeSessions")
                    throw error
                }
        )
    }

    fun observeAuditLogs(): Flow<List<AuditLogItem>> = flow {
        requireUserId()
        emitAll(
            db.collection("auditLogs")
                .snapshots()
                .map { snap ->
                    snap.toObjects(AuditLogItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, "auditLogs")
                    throw error
                }
        )
    }

    suspend fun logSecurityAlert(
        targetUserId: String = auth.currentUser?.uid.orEmpty(),
        username: String = "",
        alertType: String,
        severity: String,
        title: String,
        description: String,
        deviceLabel: String = ""
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val ref = db.collection("securityAlerts").document()
        return try {
            ref.set(
                mapOf(
                    "userId" to targetUserId.ifBlank { uid },
                    "username" to SecurityValidator.sanitizePlainText(username, 80),
                    "alertType" to alertType.take(60),
                    "severity" to severity.take(20),
                    "title" to SecurityValidator.sanitizePlainText(title, 180),
                    "description" to SecurityValidator.sanitizePlainText(description, 1500),
                    "deviceLabel" to deviceLabel.take(160),
                    "resolvedFlag" to false,
                    "resolvedBy" to "",
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

    suspend fun resolveSecurityAlert(alertId: String): Result<Unit> {
        val adminUid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val ref = db.collection("securityAlerts").document(alertId)
        return try {
            ref.update(
                mapOf(
                    "resolvedFlag" to true,
                    "resolvedBy" to adminUid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            logAudit("resolve_security_alert", "securityAlerts", alertId, "Marked security alert resolved")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun logSecurityEvent(uid: String, event: String, deviceInfo: String, status: String) {
        try {
            db.collection("users").document(uid).collection("securityLogs").document().set(
                mapOf(
                    "userId" to uid,
                    "event" to event.take(200),
                    "deviceInfo" to deviceInfo.take(200),
                    "status" to status.take(40),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (_: Exception) {
        }
    }

    suspend fun logAudit(action: String, targetCollection: String, targetId: String, summary: String) {
        val user = auth.currentUser ?: return
        try {
            db.collection("auditLogs").document().set(
                mapOf(
                    "actorUserId" to user.uid,
                    "actorEmail" to user.email.orEmpty().take(120),
                    "action" to action.take(80),
                    "targetCollection" to targetCollection.take(120),
                    "targetId" to targetId.take(128),
                    "summary" to SecurityValidator.sanitizePlainText(summary, 500),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (_: Exception) {
        }
    }
}
