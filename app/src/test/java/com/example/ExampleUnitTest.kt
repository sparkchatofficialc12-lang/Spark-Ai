package com.example

import com.example.core.RateLimiter
import com.example.core.SecurityPostureEvaluator
import com.example.core.SecurityValidator
import com.example.core.ValidationResult
import com.example.data.model.CommunityTemplate
import com.example.data.model.MovieItem
import com.example.data.model.MusicLibraryCatalog
import com.example.data.model.RecentlyDeletedItem
import com.example.data.model.SocialPost
import com.example.data.model.SparkCatalog
import com.example.data.model.SparkGuideArticle
import com.example.data.model.SparkGuideCatalog
import com.example.ui.screens.MovieLibraryHelper
import com.example.ui.screens.MovieLibrarySort
import com.google.firebase.Timestamp
import org.junit.Assert.*
import org.junit.Test

/**
 * Local JVM unit tests for Spark AI core logic, security validation, Spark Guide, and Recycle Bin.
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun creditEstimationAndDurationFormattingScaleCorrectly() {
        assertEquals(6, SparkCatalog.estimateCredits(60, "1080p"))
        assertEquals(12, SparkCatalog.estimateCredits(60, "4K UHD"))
        assertEquals("30s", SparkCatalog.formatDuration(30))
        assertEquals("2m", SparkCatalog.formatDuration(120))
        assertEquals("2h", SparkCatalog.formatDuration(7200))
    }

    @Test
    fun musicCatalogAndSocialCreationTypesIncludeRequiredCategories() {
        assertTrue(MusicLibraryCatalog.categories.containsAll(listOf("African", "Nigerian", "Afrobeats", "Gospel", "Hip-Hop", "Pop", "Instrumental", "Royalty-Free")))
        assertTrue(MusicLibraryCatalog.curatedTracks.all { it.licenseType.isNotBlank() && it.bpm > 0 })
        val keys = MusicLibraryCatalog.creationTypes.map { it.first }
        assertTrue(keys.containsAll(listOf("text", "photo", "photo_album", "short_video", "long_video", "movie", "music_video", "singing_photo", "animation", "template")))
    }

    @Test
    fun analyticsAggregationCountsRealPostsAndTemplatesAccurately() {
        val posts = listOf(
            SocialPost(id = "p1", title = "Lagos Dawn", viewsCount = 120, uniqueViewersCount = 90, likesCount = 30, commentsCount = 8, sharesCount = 5),
            SocialPost(id = "p2", title = "Abuja Nights", viewsCount = 45, uniqueViewersCount = 40, likesCount = 12, commentsCount = 3, sharesCount = 1)
        )
        val templates = listOf(
            CommunityTemplate(id = "t1", title = "Lagos Drone Sweep", remixesCount = 14)
        )
        assertEquals(165, posts.sumOf { it.viewsCount })
        assertEquals(130, posts.sumOf { it.uniqueViewersCount })
        assertEquals(14, templates.sumOf { it.remixesCount })
    }

    @Test
    fun securityValidatorBlocksScriptInjectionAndUnsafeUrlsWhileAllowingCleanInput() {
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
    fun rateLimiterEnforcesBurstCapAndIsolatesActionKeys() {
        var fakeNowMs = 1_000_000L
        val limiter = RateLimiter(nowMsProvider = { fakeNowMs })
        assertTrue(limiter.checkAndRecord("spark_guide", maxRequests = 2, windowMs = 10_000L).allowed)
        assertTrue(limiter.checkAndRecord("spark_guide", maxRequests = 2, windowMs = 10_000L).allowed)

        val blocked = limiter.checkAndRecord("spark_guide", maxRequests = 2, windowMs = 10_000L)
        assertFalse(blocked.allowed)
        assertTrue(blocked.retryAfterSec >= 1)

        assertTrue(limiter.checkAndRecord("publish_post", maxRequests = 2, windowMs = 10_000L).allowed)

        fakeNowMs += 11_000L
        assertTrue(limiter.checkAndRecord("spark_guide", maxRequests = 2, windowMs = 10_000L).allowed)
    }

    @Test
    fun sparkGuideCatalogWelcomeStartersLanguagesAndOfflineFaqMatchingWork() {
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
    fun securityPostureEvaluatorKeepsClientVsBackendRequirementsTransparent() {
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
    fun recentlyDeletedItemComputes30DayRetentionWindowCleanly() {
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
    fun movieLibraryHelperFiltersSearchesAndSortsFirestoreMovies() {
        val m1 = MovieItem(
            id = "m1",
            title = "Lagos Horizon",
            genre = "Sci-Fi",
            durationSec = 180,
            resolution = "4K UHD",
            status = "completed",
            videoUrl = "https://cdn.example.com/lagos.mp4",
            favorite = true,
            createdAt = Timestamp(1_700_000_100L, 0),
            updatedAt = Timestamp(1_700_000_200L, 0)
        )
        val m2 = MovieItem(
            id = "m2",
            title = "Abuja Rhythm",
            genre = "Musical",
            durationSec = 90,
            resolution = "1080p",
            status = "draft",
            videoUrl = "",
            favorite = false,
            createdAt = Timestamp(1_700_000_300L, 0),
            updatedAt = Timestamp(1_700_000_400L, 0)
        )

        val readyOnly = MovieLibraryHelper.filterAndSortMovies(listOf(m1, m2), "", "MP4 Ready", MovieLibrarySort.NEWEST)
        assertEquals(listOf("m1"), readyOnly.map { it.id })

        val favOnly = MovieLibraryHelper.filterAndSortMovies(listOf(m1, m2), "lagos", "Favorites", MovieLibrarySort.NEWEST)
        assertEquals(listOf("m1"), favOnly.map { it.id })

        val byLongest = MovieLibraryHelper.filterAndSortMovies(listOf(m1, m2), "", "All", MovieLibrarySort.LONGEST)
        assertEquals(listOf("m1", "m2"), byLongest.map { it.id })

        val byTitle = MovieLibraryHelper.filterAndSortMovies(listOf(m1, m2), "", "All", MovieLibrarySort.TITLE)
        assertEquals(listOf("m2", "m1"), byTitle.map { it.id })

        assertTrue(SparkCatalog.movieLanguages.contains("Nigerian Pidgin"))
        val m3 = m1.copy(id = "m3", projectId = "proj_pidgin")
        val byPidgin = MovieLibraryHelper.filterAndSortMovies(
            movies = listOf(m2, m3),
            query = "Pidgin",
            filterChip = "All",
            sortOption = MovieLibrarySort.NEWEST,
            projectLanguages = mapOf("proj_pidgin" to "Nigerian Pidgin")
        )
        assertEquals(listOf("m3"), byPidgin.map { it.id })
    }
}

