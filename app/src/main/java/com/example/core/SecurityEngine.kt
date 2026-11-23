package com.example.core

import android.content.Context
import android.os.Build
import java.util.UUID

sealed class ValidationResult {
    data class Valid(val sanitized: String) : ValidationResult()
    data class Invalid(val reason: String, val riskCategory: String) : ValidationResult()
}

object SecurityValidator {
    private val dangerousHtmlOrScriptRegex = Regex(
        pattern = "(<\\s*/?\\s*script\\b|javascript\\s*:|on(error|load|click|mouseover)\\s*=|<\\s*iframe\\b|<\\s*object\\b)",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    private val nosqlOperatorRegex = Regex(
        pattern = "(\\\$where|\\\$ne|\\\$gt|\\\$lt|\\\$gte|\\\$lte|\\\$regex|\\\$in|\\\$nin|\\\$or|\\\$and|\\\$set)"
    )

    private val controlCharRegex = Regex("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F\\u007F]")

    fun sanitizePlainText(input: String, maxLength: Int = 2000): String {
        val strippedControls = controlCharRegex.replace(input, "")
        val strippedTags = strippedControls
            .replace(Regex("<\\s*script[^>]*>.*?<\\s*/\\s*script\\s*>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
            .replace(Regex("</?(script|iframe|object|embed|style)[^>]*>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("javascript\\s*:", RegexOption.IGNORE_CASE), "")
        return strippedTags.trim().take(maxLength)
    }

    fun validateUserInput(
        input: String,
        fieldName: String,
        maxLength: Int = 2000,
        allowEmpty: Boolean = false
    ): ValidationResult {
        val trimmed = input.trim()
        if (!allowEmpty && trimmed.isBlank()) {
            return ValidationResult.Invalid("$fieldName cannot be empty.", "empty_input")
        }
        if (dangerousHtmlOrScriptRegex.containsMatchIn(trimmed)) {
            return ValidationResult.Invalid(
                "$fieldName contains unsafe script or HTML event markup and was blocked.",
                "xss_attempt"
            )
        }
        if (nosqlOperatorRegex.containsMatchIn(trimmed)) {
            return ValidationResult.Invalid(
                "$fieldName contains restricted query operators and was blocked.",
                "injection_attempt"
            )
        }
        val clean = sanitizePlainText(trimmed, maxLength)
        return ValidationResult.Valid(clean)
    }

    fun isSafeMediaOrWebUrl(rawUrl: String, allowEmpty: Boolean = true): Boolean {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) return allowEmpty
        val lower = trimmed.lowercase()
        if (lower.startsWith("javascript:") ||
            lower.startsWith("vbscript:") ||
            lower.startsWith("data:text/html") ||
            lower.startsWith("file://")
        ) {
            return false
        }
        return lower.startsWith("https://") ||
            lower.startsWith("content://") ||
            lower.startsWith("android.resource://") ||
            lower.startsWith("data:image/") ||
            lower.startsWith("http://127.0.0.1") ||
            lower.startsWith("http://localhost")
    }

    fun validateMediaSizeBytes(sizeBytes: Long, maxMb: Int = 250): ValidationResult {
        if (sizeBytes < 0) return ValidationResult.Invalid("Invalid media size.", "invalid_media")
        val maxBytes = maxMb.toLong() * 1024L * 1024L
        if (sizeBytes > maxBytes) {
            return ValidationResult.Invalid(
                "Selected media exceeds the ${maxMb}MB client safety upload limit.",
                "oversized_upload"
            )
        }
        return ValidationResult.Valid(sizeBytes.toString())
    }
}

data class RateLimitDecision(
    val allowed: Boolean,
    val retryAfterSec: Int = 0,
    val reason: String = ""
)

class RateLimiter(
    private val nowMsProvider: () -> Long = { System.currentTimeMillis() }
) {
    private val timestampsByKey = mutableMapOf<String, MutableList<Long>>()

    @Synchronized
    fun checkAndRecord(
        actionKey: String,
        maxRequests: Int,
        windowMs: Long
    ): RateLimitDecision {
        val now = nowMsProvider()
        val list = timestampsByKey.getOrPut(actionKey) { mutableListOf() }
        list.removeAll { ts -> now - ts >= windowMs }
        if (list.size >= maxRequests) {
            val oldest = list.minOrNull() ?: now
            val remainingMs = (windowMs - (now - oldest)).coerceAtLeast(1000L)
            val retrySec = ((remainingMs + 999L) / 1000L).toInt()
            return RateLimitDecision(
                allowed = false,
                retryAfterSec = retrySec,
                reason = "Too many requests. Please wait ${retrySec}s before trying again."
            )
        }
        list.add(now)
        return RateLimitDecision(allowed = true)
    }

    @Synchronized
    fun reset(actionKey: String? = null) {
        if (actionKey == null) timestampsByKey.clear() else timestampsByKey.remove(actionKey)
    }

    companion object {
        val shared = RateLimiter()
    }
}

object DeviceSessionHelper {
    private const val PREFS_NAME = "spark_security_session_prefs"
    private const val KEY_DEVICE_SESSION_ID = "device_session_id"

    fun getOrCreateSessionId(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_DEVICE_SESSION_ID, null)
        if (!existing.isNullOrBlank()) return existing
        val created = "sess_" + UUID.randomUUID().toString().replace("-", "").take(18)
        prefs.edit().putString(KEY_DEVICE_SESSION_ID, created).apply()
        return created
    }

    fun getDeviceLabel(): String {
        val manufacturer = Build.MANUFACTURER?.trim().orEmpty().replaceFirstChar { it.uppercase() }
        val model = Build.MODEL?.trim().orEmpty()
        val sdk = Build.VERSION.SDK_INT
        val combined = "$manufacturer $model".trim().ifBlank { "Android Device" }
        return "$combined (Android API $sdk)"
    }
}

data class SecurityPostureCheckItem(
    val id: String,
    val title: String,
    val status: String, // "ACTIVE" or "BACKEND_REQUIRED"
    val detail: String
)

object SecurityPostureEvaluator {
    fun evaluateClientPosture(
        appCheckConfigured: Boolean,
        firestoreRulesActive: Boolean,
        sessionTrackingActive: Boolean
    ): List<SecurityPostureCheckItem> {
        return listOf(
            SecurityPostureCheckItem(
                id = "google_signin",
                title = "Google Credential Manager + Firebase Auth",
                status = "ACTIVE",
                detail = "Verified Google ID token authentication with automatic session restoration and revocation."
            ),
            SecurityPostureCheckItem(
                id = "app_check",
                title = "Firebase App Check Enforcement",
                status = if (appCheckConfigured) "ACTIVE" else "BACKEND_REQUIRED",
                detail = "Play Integrity in release builds and Debug provider in local development protect Gemini AI Logic & Firestore."
            ),
            SecurityPostureCheckItem(
                id = "firestore_rules",
                title = "Zero-Trust Firestore Ownership & Schema Rules",
                status = if (firestoreRulesActive) "ACTIVE" else "BACKEND_REQUIRED",
                detail = "User-scoped writes, founder-only admin collections, immutable audit logs, and private privacy enforcement."
            ),
            SecurityPostureCheckItem(
                id = "input_sanitization",
                title = "Input Sanitization & Rate Limiting",
                status = "ACTIVE",
                detail = "Client-side XSS/script filtering, safe URL scheme checks, and burst throttling on AI & publishing actions."
            ),
            SecurityPostureCheckItem(
                id = "session_revocation",
                title = "Multi-Device Session Tracking & Remote Logout",
                status = if (sessionTrackingActive) "ACTIVE" else "BACKEND_REQUIRED",
                detail = "Live Firestore session listeners force immediate client sign-out when a session is revoked from Security Center or Founder Admin."
            ),
            SecurityPostureCheckItem(
                id = "server_proxy_waf",
                title = "Server-Side Cloud Functions, WAF & External Webhook Signing",
                status = "BACKEND_REQUIRED",
                detail = "Server-enforced rate limits, Stripe/Paystack webhook HMAC verification, and long-video provider token custody require Cloud Functions outside the mobile client."
            )
        )
    }
}
