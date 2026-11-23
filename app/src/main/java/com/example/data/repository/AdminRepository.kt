package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.ai.ConnectionCheckOutcome
import com.example.data.model.AdminSetting
import com.example.data.model.GenerationJob
import com.example.data.model.SupportTicket
import com.example.data.model.TemplateReport
import com.example.data.model.TransactionRecord
import com.example.data.model.UserAccount
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

class AdminRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireAdminUid(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Admin must be signed in.")
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

    fun observeAllUsers(): Flow<List<UserAccount>> = flow {
        requireAdminUid()
        emitAll(
            db.collection("users")
                .snapshots()
                .map { snap ->
                    snap.toObjects(UserAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, "users")
                    throw error
                }
        )
    }

    fun observeAllJobs(): Flow<List<GenerationJob>> = flow {
        requireAdminUid()
        emitAll(
            db.collectionGroup("generationJobs")
                .snapshots()
                .map { snap ->
                    snap.toObjects(GenerationJob::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, "collectionGroup/generationJobs")
                    throw error
                }
        )
    }

    fun observeAllTickets(): Flow<List<SupportTicket>> = flow {
        requireAdminUid()
        emitAll(
            db.collectionGroup("supportTickets")
                .snapshots()
                .map { snap ->
                    snap.toObjects(SupportTicket::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, "collectionGroup/supportTickets")
                    throw error
                }
        )
    }

    fun observeAllTransactions(): Flow<List<TransactionRecord>> = flow {
        requireAdminUid()
        emitAll(
            db.collectionGroup("transactions")
                .snapshots()
                .map { snap ->
                    snap.toObjects(TransactionRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, "collectionGroup/transactions")
                    throw error
                }
        )
    }

    fun observeTemplateReports(): Flow<List<TemplateReport>> = flow {
        requireAdminUid()
        emitAll(
            db.collection("templateReports")
                .snapshots()
                .map { snap ->
                    snap.toObjects(TemplateReport::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, "templateReports")
                    throw error
                }
        )
    }

    suspend fun testFirestoreConnection(): ConnectionCheckOutcome {
        return try {
            val uid = auth.currentUser?.uid
                ?: return ConnectionCheckOutcome.AuthError("No signed-in Firebase user session.")
            db.collection("users").document(uid).get().await()
            ConnectionCheckOutcome.Success("Cloud Firestore & Firebase Auth verified for UID ${uid.take(8)}...")
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseFirestoreException) {
            when (e.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED,
                FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                    ConnectionCheckOutcome.AuthError(e.localizedMessage ?: "Firestore authentication error.")
                FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED ->
                    ConnectionCheckOutcome.UsageLimit(e.localizedMessage ?: "Firestore quota limit reached.")
                else -> ConnectionCheckOutcome.Failed(e.localizedMessage ?: "Firestore error: ${e.code}")
            }
        } catch (e: Exception) {
            ConnectionCheckOutcome.Failed(e.localizedMessage ?: "Firestore connection failed.")
        }
    }

    suspend fun setAccountStatus(targetUserId: String, status: String): Result<Unit> {
        requireAdminUid()
        val ref = db.collection("users").document(targetUserId)
        return try {
            ref.update(
                mapOf(
                    "accountStatus" to status,
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

    suspend fun adjustUserCredits(
        targetUserId: String,
        deltaCredits: Int,
        reason: String
    ): Result<Unit> {
        requireAdminUid()
        val creditRef = db.collection("users").document(targetUserId).collection("credits").document("balance")
        return try {
            val snap = creditRef.get().await()
            val cur = (snap.getLong("balance") ?: 0L).toInt()
            val promo = (snap.getLong("promoCredits") ?: 0L).toInt()
            val earned = (snap.getLong("lifetimeEarned") ?: 0L).toInt()
            val spent = (snap.getLong("lifetimeSpent") ?: 0L).toInt()
            val newBal = (cur + deltaCredits).coerceAtLeast(0)
            val newPromo = if (deltaCredits > 0) promo + deltaCredits else promo
            val newEarned = if (deltaCredits > 0) earned + deltaCredits else earned
            val newSpent = if (deltaCredits < 0) spent + (-deltaCredits) else spent

            creditRef.set(
                mapOf(
                    "userId" to targetUserId,
                    "balance" to newBal,
                    "promoCredits" to newPromo,
                    "lifetimeEarned" to newEarned,
                    "lifetimeSpent" to newSpent,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            db.collection("users").document(targetUserId).collection("transactions").document().set(
                mapOf(
                    "userId" to targetUserId,
                    "type" to if (deltaCredits >= 0) "promo_grant" else "admin_adjustment",
                    "amountCredits" to deltaCredits,
                    "amountUsd" to 0.0,
                    "description" to reason.ifBlank { "Founder Admin credit adjustment" }.take(400),
                    "status" to "completed",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, creditRef.path)
            Result.failure(e)
        }
    }

    suspend fun saveAdminSettings(setting: AdminSetting): Result<Unit> {
        val adminUid = requireAdminUid()
        val ref = db.collection("adminSettings").document("providerConfig")
        return try {
            ref.set(
                mapOf(
                    "key" to "providerConfig",
                    "providerName" to setting.providerName.ifBlank { "Custom Video Proxy" }.take(120),
                    "providerBaseUrl" to setting.providerBaseUrl.trim().take(500),
                    "providerConfigured" to (setting.providerConfigured && setting.providerBaseUrl.isNotBlank()),
                    "maxFreeDurationSec" to setting.maxFreeDurationSec.coerceAtLeast(10),
                    "maxPaidDurationSec" to setting.maxPaidDurationSec.coerceAtLeast(60),
                    "promoOfferTitle" to setting.promoOfferTitle.take(200),
                    "promoBonusCredits" to setting.promoBonusCredits.coerceAtLeast(0),
                    "paymentProviderName" to setting.paymentProviderName.take(100),
                    "paymentConfigured" to setting.paymentConfigured,
                    "liveStreamingProviderName" to setting.liveStreamingProviderName.take(120),
                    "liveStreamingBaseUrl" to setting.liveStreamingBaseUrl.trim().take(500),
                    "liveStreamingConfigured" to (setting.liveStreamingConfigured && setting.liveStreamingBaseUrl.isNotBlank()),
                    "updatedBy" to adminUid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun retryFailedJobAsAdmin(job: GenerationJob): Result<Unit> {
        requireAdminUid()
        val ref = db.collection("users").document(job.userId).collection("generationJobs").document(job.id)
        return try {
            ref.update(
                mapOf(
                    "status" to "queued",
                    "errorMessage" to "",
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

    suspend fun resolveSupportTicket(userId: String, ticketId: String, status: String): Result<Unit> {
        requireAdminUid()
        val ref = db.collection("users").document(userId).collection("supportTickets").document(ticketId)
        return try {
            ref.update(
                mapOf(
                    "status" to status,
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

    suspend fun moderateTemplate(reportId: String, templateId: String, removeTemplate: Boolean): Result<Unit> {
        requireAdminUid()
        return try {
            if (removeTemplate) {
                db.collection("communityTemplates").document(templateId).update(
                    mapOf(
                        "moderationStatus" to "removed",
                        "visibility" to "private",
                        "allowReuse" to false,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            db.collection("templateReports").document(reportId).update(
                mapOf("status" to if (removeTemplate) "action_taken" else "reviewed")
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "templateReports/$reportId")
            Result.failure(e)
        }
    }
}
