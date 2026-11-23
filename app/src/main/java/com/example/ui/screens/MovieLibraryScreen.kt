package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.MediaUtils
import com.example.data.model.MovieItem
import com.example.data.model.MovieProject
import com.example.data.model.SocialPost
import com.example.data.model.SparkCatalog
import com.example.ui.components.Base64Thumbnail
import com.example.ui.components.SparkBadge
import com.example.ui.components.SparkBusyOverlay
import com.example.ui.components.SparkCard
import com.example.ui.components.SparkGoldButton
import com.example.ui.components.SparkOutlinedButton
import com.example.ui.components.SparkStatusBanner
import com.example.ui.components.SparkTopBar
import com.example.ui.navigation.SparkRoute
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.IceWhite
import com.example.ui.theme.MidnightBlack
import com.example.ui.theme.SecondaryIce
import com.example.ui.theme.SecondarySlate
import com.example.ui.theme.SoftGold
import com.example.ui.theme.SparkBrushes
import com.example.ui.theme.SteelBorder
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.SocialUiState
import com.example.ui.viewmodel.StudioUiState
import java.text.SimpleDateFormat
import java.util.Locale

enum class LibrarySourceTab(val label: String) {
    MY_MOVIES("My Movies"),
    COMMUNITY("Community")
}

enum class MovieLibrarySort(val label: String) {
    NEWEST("Newest"),
    LONGEST("Longest"),
    TITLE("A–Z")
}

object MovieLibraryHelper {
    fun filterAndSortMovies(
        movies: List<MovieItem>,
        query: String,
        filterChip: String,
        sortOption: MovieLibrarySort,
        projectLanguages: Map<String, String> = emptyMap()
    ): List<MovieItem> {
        val cleanQuery = query.trim().lowercase(Locale.getDefault())
        val filtered = movies.filter { movie ->
            val movieLang = projectLanguages[movie.projectId].orEmpty().lowercase(Locale.getDefault())
            val matchesQuery = cleanQuery.isBlank() ||
                movie.title.lowercase(Locale.getDefault()).contains(cleanQuery) ||
                movie.genre.lowercase(Locale.getDefault()).contains(cleanQuery) ||
                movie.resolution.lowercase(Locale.getDefault()).contains(cleanQuery) ||
                movie.status.lowercase(Locale.getDefault()).contains(cleanQuery) ||
                movieLang.contains(cleanQuery)

            val matchesFilter = when (filterChip) {
                "All" -> true
                "MP4 Ready" -> movie.videoUrl.isNotBlank() || movie.status.equals("completed", ignoreCase = true)
                "Favorites" -> movie.favorite
                "Drafts" -> movie.status.equals("draft", ignoreCase = true)
                else -> movie.genre.equals(filterChip, ignoreCase = true)
            }
            matchesQuery && matchesFilter
        }

        return when (sortOption) {
            MovieLibrarySort.NEWEST -> filtered.sortedByDescending {
                it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L
            }
            MovieLibrarySort.LONGEST -> filtered.sortedByDescending { it.durationSec }
            MovieLibrarySort.TITLE -> filtered.sortedBy { it.title.lowercase(Locale.getDefault()) }
        }
    }

    fun filterCommunityMoviePosts(
        posts: List<SocialPost>,
        query: String,
        sortOption: MovieLibrarySort
    ): List<SocialPost> {
        val movieTypes = setOf("movie", "long_video", "music_video", "animation", "singing_photo", "short_video")
        val cleanQuery = query.trim().lowercase(Locale.getDefault())
        val filtered = posts.filter { post ->
            val isMovieOrVideo = post.mediaType == "video" || post.creationType in movieTypes
            val matchesQuery = cleanQuery.isBlank() ||
                post.title.lowercase(Locale.getDefault()).contains(cleanQuery) ||
                post.category.lowercase(Locale.getDefault()).contains(cleanQuery) ||
                post.creatorUsername.lowercase(Locale.getDefault()).contains(cleanQuery)
            isMovieOrVideo && matchesQuery
        }
        return when (sortOption) {
            MovieLibrarySort.NEWEST -> filtered.sortedByDescending { it.createdAt?.seconds ?: 0L }
            MovieLibrarySort.LONGEST -> filtered.sortedByDescending { it.durationSec }
            MovieLibrarySort.TITLE -> filtered.sortedBy { it.title.lowercase(Locale.getDefault()) }
        }
    }

    fun formatTimestampSeconds(seconds: Long?): String {
        if (seconds == null || seconds <= 0L) return "Synced"
        return try {
            val fmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            fmt.format(java.util.Date(seconds * 1000L))
        } catch (_: Exception) {
            "Synced"
        }
    }
}

@Composable
fun MovieLibraryScreen(
    studioState: StudioUiState,
    socialState: SocialUiState,
    onMarkMovieViewed: (String) -> Unit,
    onToggleFavorite: (MovieItem) -> Unit,
    onDownloadMovie: (MovieItem) -> Unit,
    onClearBanners: () -> Unit,
    onBack: () -> Unit,
    onNavigate: (SparkRoute) -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current

    var sourceTab by rememberSaveable { mutableStateOf(LibrarySourceTab.MY_MOVIES) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf("All") }
    var sortOption by rememberSaveable { mutableStateOf(MovieLibrarySort.NEWEST) }
    var inspectedMovie by remember { mutableStateOf<MovieItem?>(null) }

    val projectById = remember(studioState.projects) {
        studioState.projects.associateBy { it.id }
    }
    val projectLanguages = remember(studioState.projects) {
        studioState.projects.associate { it.id to it.language }
    }
    val postThumbByProject = remember(socialState.myPosts) {
        socialState.myPosts
            .filter { it.linkedProjectId.isNotBlank() && it.thumbnailData.isNotBlank() }
            .associate { it.linkedProjectId to it.thumbnailData }
    }

    val genreFilters = remember(studioState.movies) {
        val base = listOf("All", "MP4 Ready", "Favorites", "Drafts")
        val dynamicGenres = studioState.movies
            .map { it.genre.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .take(6)
        base + dynamicGenres
    }

    val filteredMyMovies = remember(studioState.movies, searchQuery, selectedFilter, sortOption, projectLanguages) {
        MovieLibraryHelper.filterAndSortMovies(studioState.movies, searchQuery, selectedFilter, sortOption, projectLanguages)
    }
    val filteredCommunityPosts = remember(socialState.publicPosts, searchQuery, sortOption) {
        MovieLibraryHelper.filterCommunityMoviePosts(socialState.publicPosts, searchQuery, sortOption)
    }

    val readyCount = remember(studioState.movies) {
        studioState.movies.count { it.videoUrl.isNotBlank() || it.status.equals("completed", ignoreCase = true) }
    }
    val favoriteCount = remember(studioState.movies) {
        studioState.movies.count { it.favorite }
    }
    val totalRuntimeSec = remember(studioState.movies) {
        studioState.movies.sumOf { it.durationSec }
    }

    Scaffold(
        topBar = {
            SparkTopBar(
                title = "Movie Library",
                subtitle = "${studioState.movies.size} created • Firestore synced",
                onBack = onBack,
                credits = studioState.credits?.balance,
                onCreditsClick = { onNavigate(SparkRoute.SubscriptionCredits) },
                actions = {
                    IconButton(
                        onClick = { onNavigate(SparkRoute.MyCreations) },
                        modifier = Modifier.testTag("movie_library_list_view_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "Manage list view",
                            tint = ChampagneGold
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SparkBrushes.CosmicBackground)
                .padding(innerPadding)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 164.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("movie_library_grid")
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SparkStatusBanner(
                        statusMessage = studioState.statusBanner,
                        errorMessage = studioState.errorBanner,
                        onDismiss = onClearBanners
                    )
                }

                // Cinematic summary card
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SparkCard(borderColor = ChampagneGold.copy(alpha = 0.6f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(ChampagneGold.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VideoLibrary,
                                        contentDescription = null,
                                        tint = ChampagneGold
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Cinema Poster Showcase",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = IceWhite
                                    )
                                    Text(
                                        text = "${studioState.movies.size} movies • $readyCount ready • $favoriteCount favs • ${SparkCatalog.formatDuration(totalRuntimeSec)} total",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SoftGold
                                    )
                                }
                            }
                            SparkGoldButton(
                                text = "New",
                                icon = Icons.Default.MovieCreation,
                                onClick = { onNavigate(SparkRoute.CreateMovie("text_to_movie")) },
                                modifier = Modifier.testTag("movie_library_create_btn")
                            )
                        }
                    }
                }

                // Source Switcher: My Movies vs Community
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LibrarySourceTab.entries.forEach { tab ->
                            val selected = sourceTab == tab
                            val count = if (tab == LibrarySourceTab.MY_MOVIES) {
                                studioState.movies.size
                            } else {
                                filteredCommunityPosts.size
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (selected) ChampagneGold else SecondarySlate,
                                border = BorderStroke(1.dp, if (selected) ChampagneGold else SteelBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(50))
                                    .clickable { sourceTab = tab }
                                    .testTag("library_source_${tab.name.lowercase()}")
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
                                    Text(
                                        text = "${tab.label} ($count)",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (selected) MidnightBlack else IceWhite,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // Search & Sort row
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search movies, genres, 4K...", color = SecondaryIce) },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = "Search library", tint = ChampagneGold)
                            },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear search", tint = SecondaryIce)
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ChampagneGold,
                                unfocusedBorderColor = SteelBorder,
                                focusedTextColor = IceWhite,
                                unfocusedTextColor = IceWhite,
                                focusedContainerColor = SecondarySlate,
                                unfocusedContainerColor = SecondarySlate
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("movie_library_search")
                        )
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SecondarySlate,
                            border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.55f)),
                            modifier = Modifier
                                .heightIn(min = 52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    val all = MovieLibrarySort.entries
                                    sortOption = all[(sortOption.ordinal + 1) % all.size]
                                }
                                .testTag("movie_library_sort_btn")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = "Cycle sort order",
                                    tint = ChampagneGold,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = sortOption.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ChampagneGold,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Filter chips for My Movies
                if (sourceTab == LibrarySourceTab.MY_MOVIES) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(genreFilters) { chip ->
                                FilterChip(
                                    selected = selectedFilter == chip,
                                    onClick = { selectedFilter = chip },
                                    label = { Text(chip) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ChampagneGold,
                                        selectedLabelColor = MidnightBlack
                                    )
                                )
                            }
                        }
                    }
                }

                // Main Content Grid
                if (sourceTab == LibrarySourceTab.MY_MOVIES) {
                    if (studioState.isBootstrapping && studioState.movies.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            SparkCard {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator(color = ChampagneGold)
                                    Text(
                                        text = "Syncing your created movies from Firestore...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = SecondaryIce
                                    )
                                }
                            }
                        }
                    } else if (filteredMyMovies.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            SparkCard {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 18.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Movie,
                                        contentDescription = null,
                                        tint = ChampagneGold,
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Text(
                                        text = if (studioState.movies.isEmpty()) {
                                            "Your Movie Library is empty"
                                        } else {
                                            "No movies match \"$selectedFilter\" ${if (searchQuery.isNotBlank()) "and \"$searchQuery\"" else ""}"
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        color = IceWhite
                                    )
                                    Text(
                                        text = "Generate your first AI movie with Gemini screenplays, storyboards, and scene previews — every creation syncs here automatically.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SecondaryIce
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        SparkGoldButton(
                                            text = "Create AI Movie",
                                            icon = Icons.Default.MovieCreation,
                                            onClick = { onNavigate(SparkRoute.CreateMovie("text_to_movie")) },
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (selectedFilter != "All" || searchQuery.isNotBlank()) {
                                            SparkOutlinedButton(
                                                text = "Reset Filters",
                                                onClick = {
                                                    selectedFilter = "All"
                                                    searchQuery = ""
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        items(filteredMyMovies, key = { it.id.ifBlank { it.projectId + it.title } }) { movie ->
                            val resolvedThumb = movie.thumbnailData.ifBlank {
                                postThumbByProject[movie.projectId].orEmpty()
                            }
                            val linkedProject = projectById[movie.projectId]
                            MoviePosterGridCard(
                                movie = movie,
                                thumbnailData = resolvedThumb,
                                visualStyle = linkedProject?.visualStyle.orEmpty(),
                                movieLanguage = linkedProject?.language.orEmpty(),
                                onCardClick = { inspectedMovie = movie },
                                onPlayClick = {
                                    if (movie.id.isNotBlank()) onMarkMovieViewed(movie.id)
                                    onNavigate(
                                        SparkRoute.VideoPlayer(
                                            title = movie.title,
                                            videoUrl = movie.videoUrl,
                                            movieId = movie.id,
                                            subtitlesText = movie.subtitlesText,
                                            resolution = movie.resolution
                                        )
                                    )
                                },
                                onToggleFavorite = { onToggleFavorite(movie) }
                            )
                        }
                    }
                } else {
                    // Community Published Movies Showcase from Firestore `posts`
                    if (filteredCommunityPosts.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            SparkCard {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 18.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Public,
                                        contentDescription = null,
                                        tint = ChampagneGold,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Text(
                                        text = "No public community movies found",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = IceWhite
                                    )
                                    Text(
                                        text = "Publish a movie from your library or switch back to My Movies.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SecondaryIce
                                    )
                                }
                            }
                        }
                    } else {
                        items(filteredCommunityPosts, key = { it.id }) { post ->
                            CommunityMovieGridCard(
                                post = post,
                                onOpenPost = { onNavigate(SparkRoute.PostDetail(post.id)) },
                                onPlay = {
                                    onNavigate(
                                        SparkRoute.VideoPlayer(
                                            title = post.title,
                                            videoUrl = post.previewVideoUrl,
                                            subtitlesText = post.caption,
                                            resolution = post.resolution,
                                            postId = post.id
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
            }

            if (studioState.isBusy) {
                SparkBusyOverlay(studioState.busyLabel)
            }
        }
    }

    inspectedMovie?.let { selected ->
        val currentMovie = studioState.movies.firstOrNull { it.id == selected.id } ?: selected
        val resolvedThumb = currentMovie.thumbnailData.ifBlank {
            postThumbByProject[currentMovie.projectId].orEmpty()
        }
        val linkedProject = projectById[currentMovie.projectId]
        MovieShowcaseDetailDialog(
            movie = currentMovie,
            linkedProject = linkedProject,
            thumbnailData = resolvedThumb,
            onDismiss = { inspectedMovie = null },
            onWatch = {
                inspectedMovie = null
                if (currentMovie.id.isNotBlank()) onMarkMovieViewed(currentMovie.id)
                onNavigate(
                    SparkRoute.VideoPlayer(
                        title = currentMovie.title,
                        videoUrl = currentMovie.videoUrl,
                        movieId = currentMovie.id,
                        subtitlesText = currentMovie.subtitlesText,
                        resolution = currentMovie.resolution
                    )
                )
            },
            onOpenEditor = {
                inspectedMovie = null
                onNavigate(SparkRoute.MovieEditor(currentMovie.projectId))
            },
            onPublishPost = {
                inspectedMovie = null
                onNavigate(
                    SparkRoute.UniversalPost(
                        prefillTitle = currentMovie.title,
                        prefillType = if (currentMovie.durationSec > 60) "movie" else "short_video",
                        prefillProjectId = currentMovie.projectId,
                        prefillVideoUrl = currentMovie.videoUrl,
                        prefillThumbnail = resolvedThumb,
                        prefillDurationSec = currentMovie.durationSec
                    )
                )
            },
            onDownload = { onDownloadMovie(currentMovie) },
            onToggleFavorite = { onToggleFavorite(currentMovie) },
            onShare = {
                MediaUtils.shareMovieTextOrLink(
                    context = context,
                    title = currentMovie.title,
                    videoUrl = currentMovie.videoUrl,
                    scriptSummary = currentMovie.subtitlesText.ifBlank { linkedProject?.description.orEmpty() }
                )
            }
        )
    }
}

@Composable
private fun MoviePosterGridCard(
    movie: MovieItem,
    thumbnailData: String,
    visualStyle: String,
    movieLanguage: String,
    onCardClick: () -> Unit,
    onPlayClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val isReady = movie.videoUrl.isNotBlank() || movie.status.equals("completed", ignoreCase = true)
    val dateLabel = remember(movie.updatedAt, movie.createdAt) {
        MovieLibraryHelper.formatTimestampSeconds(movie.updatedAt?.seconds ?: movie.createdAt?.seconds)
    }

    Card(
        onClick = onCardClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SecondarySlate),
        border = BorderStroke(
            width = 1.dp,
            color = if (movie.favorite || isReady) ChampagneGold.copy(alpha = 0.65f) else SteelBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("movie_library_card_${movie.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 5f)
                    .background(MidnightBlack)
            ) {
                Base64Thumbnail(
                    base64Data = thumbnailData,
                    contentDescription = "${movie.title} poster preview",
                    modifier = Modifier.fillMaxSize(),
                    placeholderIcon = Icons.Default.Movie
                )

                // Top & bottom cinematic gradient scrim
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MidnightBlack.copy(alpha = 0.45f),
                                    Color.Transparent,
                                    MidnightBlack.copy(alpha = 0.88f)
                                )
                            )
                        )
                )

                // Status badge
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MidnightBlack.copy(alpha = 0.78f),
                    border = BorderStroke(1.dp, if (isReady) SuccessGreen else ChampagneGold),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = if (isReady) "READY" else movie.status.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isReady) SuccessGreen else ChampagneGold,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                // Favorite button (48dp touch target)
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .testTag("movie_library_fav_${movie.id}")
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MidnightBlack.copy(alpha = 0.72f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (movie.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (movie.favorite) "Remove favorite" else "Favorite movie",
                                tint = if (movie.favorite) ChampagneGold else IceWhite,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Quick play badge at center
                Surface(
                    shape = CircleShape,
                    color = MidnightBlack.copy(alpha = 0.74f),
                    border = BorderStroke(1.5.dp, ChampagneGold),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onPlayClick)
                        .testTag("movie_library_play_${movie.id}")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play ${movie.title}",
                            tint = ChampagneGold,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                // Bottom metadata strip over thumbnail
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MidnightBlack.copy(alpha = 0.8f)
                    ) {
                        Text(
                            text = "${movie.resolution} • ${movie.aspectRatio}",
                            style = MaterialTheme.typography.labelSmall,
                            color = IceWhite,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = ChampagneGold.copy(alpha = 0.9f)
                    ) {
                        Text(
                            text = SparkCatalog.formatDuration(movie.durationSec),
                            style = MaterialTheme.typography.labelSmall,
                            color = MidnightBlack,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = movie.title.ifBlank { "Untitled Movie" },
                    style = MaterialTheme.typography.titleSmall,
                    color = IceWhite,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append(movie.genre.ifBlank { "Cinema" })
                        if (movieLanguage.isNotBlank()) {
                            append(" • ")
                            append(movieLanguage)
                        } else if (visualStyle.isNotBlank()) {
                            append(" • ")
                            append(visualStyle.substringBefore(" "))
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = SoftGold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$dateLabel${if (movie.downloaded) " • Saved MP4" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SecondaryIce,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CommunityMovieGridCard(
    post: SocialPost,
    onOpenPost: () -> Unit,
    onPlay: () -> Unit
) {
    val dateLabel = remember(post.createdAt) {
        MovieLibraryHelper.formatTimestampSeconds(post.createdAt?.seconds)
    }
    Card(
        onClick = onOpenPost,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SecondarySlate),
        border = BorderStroke(1.dp, SteelBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("community_library_card_${post.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 5f)
                    .background(MidnightBlack)
            ) {
                Base64Thumbnail(
                    base64Data = post.thumbnailData,
                    contentDescription = "${post.title} thumbnail",
                    modifier = Modifier.fillMaxSize(),
                    placeholderIcon = Icons.Default.Movie
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MidnightBlack.copy(alpha = 0.4f),
                                    Color.Transparent,
                                    MidnightBlack.copy(alpha = 0.85f)
                                )
                            )
                        )
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MidnightBlack.copy(alpha = 0.78f),
                    border = BorderStroke(1.dp, ChampagneGold),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = post.creationType.replace('_', ' ').uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = ChampagneGold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = MidnightBlack.copy(alpha = 0.74f),
                    border = BorderStroke(1.5.dp, ChampagneGold),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onPlay)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play community movie",
                            tint = ChampagneGold,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(shape = RoundedCornerShape(6.dp), color = MidnightBlack.copy(alpha = 0.8f)) {
                        Text(
                            text = "${post.viewsCount} views",
                            style = MaterialTheme.typography.labelSmall,
                            color = IceWhite,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (post.durationSec > 0) {
                        Surface(shape = RoundedCornerShape(6.dp), color = ChampagneGold.copy(alpha = 0.9f)) {
                            Text(
                                text = SparkCatalog.formatDuration(post.durationSec),
                                style = MaterialTheme.typography.labelSmall,
                                color = MidnightBlack,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = post.title.ifBlank { "Community Creation" },
                    style = MaterialTheme.typography.titleSmall,
                    color = IceWhite,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "@${post.creatorUsername.ifBlank { "creator" }} • ${post.category}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SoftGold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$dateLabel • ${post.likesCount} likes",
                    style = MaterialTheme.typography.labelSmall,
                    color = SecondaryIce,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MovieShowcaseDetailDialog(
    movie: MovieItem,
    linkedProject: MovieProject?,
    thumbnailData: String,
    onDismiss: () -> Unit,
    onWatch: () -> Unit,
    onOpenEditor: () -> Unit,
    onPublishPost: () -> Unit,
    onDownload: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit
) {
    val createdDate = MovieLibraryHelper.formatTimestampSeconds(movie.createdAt?.seconds)
    val synopsis = movie.subtitlesText.ifBlank { linkedProject?.description.orEmpty() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SecondarySlate,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = movie.title.ifBlank { "Untitled Movie" },
                    style = MaterialTheme.typography.titleLarge,
                    color = ChampagneGold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (movie.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Toggle favorite",
                        tint = if (movie.favorite) ChampagneGold else SecondaryIce
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Base64Thumbnail(
                    base64Data = thumbnailData,
                    contentDescription = "${movie.title} preview",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(12.dp)),
                    placeholderIcon = Icons.Default.Movie
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SparkBadge(label = movie.genre.ifBlank { "Cinema" })
                    if (!linkedProject?.language.isNullOrBlank()) {
                        SparkBadge(label = linkedProject!!.language, color = SoftGold)
                    }
                    SparkBadge(label = movie.resolution)
                    SparkBadge(label = movie.aspectRatio)
                    SparkBadge(label = SparkCatalog.formatDuration(movie.durationSec))
                    SparkBadge(
                        label = if (movie.videoUrl.isNotBlank()) "MP4 READY" else movie.status.uppercase(),
                        color = if (movie.videoUrl.isNotBlank()) SuccessGreen else ChampagneGold
                    )
                }

                Text(
                    text = "Created: $createdDate • Visibility: ${if (movie.publicMovie) "Public" else "Private"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SoftGold
                )

                if (linkedProject != null) {
                    Text(
                        text = "${linkedProject.language} • ${linkedProject.visualStyle} • ${linkedProject.countryLocation}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryIce
                    )
                }

                if (synopsis.isNotBlank()) {
                    Text(
                        text = synopsis.take(360),
                        style = MaterialTheme.typography.bodySmall,
                        color = IceWhite
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SparkGoldButton(
                        text = "Watch",
                        icon = Icons.Default.PlayArrow,
                        onClick = onWatch,
                        modifier = Modifier.weight(1f)
                    )
                    if (movie.projectId.isNotBlank() && movie.projectId != "standalone") {
                        SparkOutlinedButton(
                            text = "Editor",
                            icon = Icons.Default.Edit,
                            onClick = onOpenEditor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SparkOutlinedButton(
                        text = "Post",
                        icon = Icons.Default.Public,
                        onClick = onPublishPost,
                        modifier = Modifier.weight(1f)
                    )
                    SparkOutlinedButton(
                        text = "Save MP4",
                        icon = Icons.Default.Download,
                        onClick = onDownload,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onShare) {
                        Icon(Icons.Default.Share, contentDescription = "Share movie", tint = ChampagneGold)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = ChampagneGold)
            }
        }
    )
}
