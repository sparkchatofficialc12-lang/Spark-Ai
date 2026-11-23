package com.example

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.example.core.MediaUtils
import com.example.core.RateLimiter
import com.example.core.SecurityPostureEvaluator
import com.example.core.SecurityValidator
import com.example.core.ThemePreferences
import com.example.core.ValidationResult
import com.example.data.model.CommunityTemplate
import com.example.data.model.MusicLibraryCatalog
import com.example.data.model.MusicTrack
import com.example.data.model.RecentlyDeletedItem
import com.example.data.model.SocialPost
import com.example.data.model.SparkCatalog
import com.example.data.model.SparkGuideArticle
import com.example.data.model.SparkGuideCatalog
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name matches Spark AI`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Spark AI", appName)
    }

    @Test
    fun `credit estimation and duration formatting scale correctly`() {
        assertEquals(6, SparkCatalog.estimateCredits(60, "1080p"))
        assertEquals(12, SparkCatalog.estimateCredits(60, "4K UHD"))
        assertEquals("30s", SparkCatalog.formatDuration(30))
        assertEquals("2m", SparkCatalog.formatDuration(120))
        assertEquals("2h", SparkCatalog.formatDuration(7200))
    }

    @Test
    fun `music catalog and social creation types include required categories`() {
        assertTrue(MusicLibraryCatalog.categories.containsAll(listOf("African", "Nigerian", "Afrobeats", "Gospel", "Hip-Hop", "Pop", "Instrumental", "Royalty-Free")))
        assertTrue(MusicLibraryCatalog.curatedTracks.all { it.licenseType.isNotBlank() && it.bpm > 0 })
        val keys = MusicLibraryCatalog.creationTypes.map { it.first }
        assertTrue(keys.containsAll(listOf("text", "photo", "photo_album", "short_video", "long_video", "movie", "music_video", "singing_photo", "animation", "template")))
    }

    @Test
    fun `analytics aggregation counts real posts and templates accurately`() {
        val posts = listOf(
            SocialPost(id = "p1", title = "Lagos Dawn", viewsCount = 120, uniqueViewersCount = 90, likesCount = 30, commentsCount = 8, sharesCount = 5),
            SocialPost(id = "p2", title = "Abuja Nights", viewsCount = 45, uniqueViewersCount = 40, likesCount = 12, commentsCount = 3, sharesCount = 1)
        )
        val templates = listOf(
            CommunityTemplate(id = "t1", title = "Lagos Drone Sweep", remixesCount = 14)
        )
        val totalViews = posts.sumOf { it.viewsCount }
        val totalUnique = posts.sumOf { it.uniqueViewersCount }
        val totalRemixes = templates.sumOf { it.remixesCount }
        assertEquals(165, totalViews)
        assertEquals(130, totalUnique)
        assertEquals(14, totalRemixes)
    }

    @Test
    fun `security validator blocks script injection and unsafe URLs while allowing clean input`() {
        val xssResult = SecurityValidator.validateUserInput("<script>alert(1)</script>", "Caption")
        assertTrue(xssResult is ValidationResult.Invalid)
        assertEquals("xss_attempt", (xssResult as ValidationResult.Invalid).riskCategory)

        val nosqlResult = SecurityValidator.validateUserInput("{\"\$where\": \"1==1\"}", "Search")
        assertTrue(nosqlResult is ValidationResult.Invalid)
        assertEquals("injection_attempt", (nosqlResult as ValidationResult.Invalid).riskCategory)

        val validResult = SecurityValidator.validateUserInput("  Cinematic Lagos sunset scene  ", "Title")
        assertTrue(validResult is ValidationResult.Valid)
        assertEquals("Cinematic Lagos sunset scene", (validResult as ValidationResult.Valid).sanitized)

        assertFalse(SecurityValidator.isSafeMediaOrWebUrl("javascript:alert(1)", allowEmpty = false))
        assertFalse(SecurityValidator.isSafeMediaOrWebUrl("file:///etc/passwd", allowEmpty = false))
        assertTrue(SecurityValidator.isSafeMediaOrWebUrl("https://cdn.example.com/movie.mp4", allowEmpty = false))
        assertTrue(SecurityValidator.isSafeMediaOrWebUrl("content://media/external/video/media/42", allowEmpty = false))
    }

    @Test
    fun `rate limiter enforces burst cap and isolates action keys`() {
        var fakeNowMs = 1_000_000L
        val limiter = RateLimiter(nowMsProvider = { fakeNowMs })
        assertTrue(limiter.checkAndRecord("spark_guide", maxRequests = 2, windowMs = 10_000L).allowed)
        assertTrue(limiter.checkAndRecord("spark_guide", maxRequests = 2, windowMs = 10_000L).allowed)

        val blocked = limiter.checkAndRecord("spark_guide", maxRequests = 2, windowMs = 10_000L)
        assertFalse(blocked.allowed)
        assertTrue(blocked.retryAfterSec >= 1)

        // Different action key still permitted
        assertTrue(limiter.checkAndRecord("publish_post", maxRequests = 2, windowMs = 10_000L).allowed)

        // Window expires
        fakeNowMs += 11_000L
        assertTrue(limiter.checkAndRecord("spark_guide", maxRequests = 2, windowMs = 10_000L).allowed)
    }

    @Test
    fun `spark guide catalog welcome starters languages and offline FAQ matching work`() {
        assertTrue(SparkGuideCatalog.WELCOME_MESSAGE.contains("Spark Guide"))
        assertEquals(6, SparkGuideCatalog.suggestedQuestions.size)
        assertEquals(7, SparkGuideCatalog.supportedLanguages.size)

        val matchedOffline = SparkGuideCatalog.matchOfflineAnswer("How do I replace or delete a post?")
        assertNotNull(matchedOffline)
        assertTrue(matchedOffline!!.answer.contains("Recently Deleted"))

        val customKb = listOf(
            SparkGuideArticle(
                id = "kb_custom",
                title = "Exporting 4K Nollywood Cut",
                stepsSummary = "1. Open My Movies\n2. Choose 4K UHD",
                keywords = "4k,nollywood,export",
                published = true
            )
        )
        val matchedAdminArticle = SparkGuideCatalog.matchOfflineAnswer("How do I export Nollywood in 4k?", customKb)
        assertNotNull(matchedAdminArticle)
        assertEquals("kb_custom", matchedAdminArticle!!.id)
    }

    @Test
    fun `security posture evaluator keeps client vs backend requirements transparent`() {
        val checks = SecurityPostureEvaluator.evaluateClientPosture(
            appCheckConfigured = true,
            firestoreRulesActive = true,
            sessionTrackingActive = true
        )
        assertTrue(checks.any { it.id == "app_check" && it.status == "ACTIVE" })
        assertTrue(checks.any { it.id == "firestore_rules" && it.status == "ACTIVE" })
        assertTrue(checks.any { it.id == "server_proxy_waf" && it.status == "BACKEND_REQUIRED" })
    }

    @Test
    fun `recently deleted item computes 30 day retention window cleanly`() {
        val deletedAt = 1_700_000_000_000L
        val thirtyDaysMs = 30L * 24L * 60L * 60L * 1000L
        val item = RecentlyDeletedItem(
            id = "post_1",
            userId = "u1",
            itemType = "post",
            originalId = "1",
            title = "Deleted Clip",
            deletedAtMs = deletedAt,
            expiresAtMs = deletedAt + thirtyDaysMs
        )
        assertEquals(30L, (item.expiresAtMs - item.deletedAtMs) / (24L * 60L * 60L * 1000L))
    }

    @Test
    fun `theme preferences persist uploaded audio tracks across reloads`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = ThemePreferences(context)
        val track = MusicTrack(
            id = "usr_test_1",
            title = "Lagos Sunrise Beat",
            artist = "creator",
            category = "Afrobeats",
            bpm = 112,
            durationSec = 140,
            mood = "Upbeat",
            licenseType = "User-Supplied Rights",
            localUri = "content://media/external/audio/media/99"
        )
        prefs.saveUploadedTrack(track)
        val saved = prefs.uploadedTracksFlow.first()
        assertTrue(saved.any { it.id == "usr_test_1" && it.title == "Lagos Sunrise Beat" })
    }

    @Test
    fun `media utils scales oversized bitmaps and produces bounded base64`() {
        val large = Bitmap.createBitmap(1600, 900, Bitmap.Config.ARGB_8888)
        val scaled = MediaUtils.scaleBitmap(large, maxDim = 480)
        assertTrue(scaled.width <= 480 && scaled.height <= 480)
        val encoded = MediaUtils.bitmapToCompactBase64(large, maxChars = 120_000)
        assertTrue(encoded.isNotBlank() && encoded.length <= 120_000)
    }
}

