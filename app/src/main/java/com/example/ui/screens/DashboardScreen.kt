package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.MusicVideo
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.WorkHistory
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.SparkCatalog
import com.example.ui.components.SparkBadge
import com.example.ui.components.SparkCard
import com.example.ui.components.SparkGoldButton
import com.example.ui.components.SparkLogoBadge
import com.example.ui.components.SparkOutlinedButton
import com.example.ui.navigation.SparkRoute
import com.example.ui.navigation.SparkTab
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.MidnightBlack
import com.example.ui.theme.SecondaryIce
import com.example.ui.theme.SoftGold
import com.example.ui.theme.SparkBrushes
import com.example.ui.viewmodel.StudioUiState

private data class StudioToolTile(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
fun DashboardScreen(
    uiState: StudioUiState,
    isFounderAdmin: Boolean,
    onNavigate: (SparkRoute) -> Unit
) {
    val account = uiState.account
    val credits = uiState.credits?.balance ?: 0
    val planName = uiState.subscription?.planName ?: "Free Creator"

    val studioTools = listOf(
        StudioToolTile("create_movie", "Create Movie", "Full cinema builder", Icons.Default.MovieCreation) {
            onNavigate(SparkRoute.CreateMovie("text_to_movie"))
        },
        StudioToolTile("text_to_movie", "Text to Movie", "Describe any scene", Icons.Default.AutoAwesome) {
            onNavigate(SparkRoute.CreateMovie("text_to_movie"))
        },
        StudioToolTile("image_to_movie", "Image to Movie", "Animate your photos", Icons.Default.Image) {
            onNavigate(SparkRoute.CreateMovie("image_to_movie"))
        },
        StudioToolTile("script_to_movie", "Script to Movie", "Turn scripts to film", Icons.Default.Description) {
            onNavigate(SparkRoute.CreateMovie("script_to_movie"))
        },
        StudioToolTile("singing_photo", "AI Singing Photo", "Portrait lip-sync", Icons.Default.RecordVoiceOver) {
            onNavigate(SparkRoute.SingingPhoto)
        },
        StudioToolTile("music_video", "Music Video", "Beat-synced visuals", Icons.Default.MusicVideo) {
            onNavigate(SparkRoute.MusicVideoStudio())
        },
        StudioToolTile("character_studio", "Character Studio", "Consistent cast", Icons.Default.Face) {
            onNavigate(SparkRoute.CharacterStudio)
        },
        StudioToolTile("storyboard", "Storyboard", "Scenes & shots", Icons.Default.ViewCarousel) {
            onNavigate(SparkRoute.ScriptStoryboard())
        },
        StudioToolTile("voice_studio", "AI Voice Studio", "Multi-accent TTS", Icons.Default.Mic) {
            onNavigate(SparkRoute.VoiceMusicStudio)
        },
        StudioToolTile("music_library", "Music Library", "Afrobeats & scores", Icons.Default.LibraryMusic) {
            onNavigate(SparkRoute.MusicLibrary())
        },
        StudioToolTile("my_movies", "My Movies", "Rendered & drafts", Icons.Default.Movie) {
            onNavigate(SparkRoute.MyCreations)
        },
        StudioToolTile("movie_library", "Movie Library", "Poster grid showcase", Icons.Default.VideoLibrary) {
            onNavigate(SparkRoute.MovieLibrary)
        },
        StudioToolTile("my_projects", "My Projects", "Open timeline editor", Icons.Default.Folder) {
            val latestProj = uiState.projects.firstOrNull()?.id.orEmpty()
            onNavigate(SparkRoute.MovieEditor(latestProj))
        },
        StudioToolTile("templates", "Templates", "Community remixes", Icons.Default.Style) {
            onNavigate(SparkRoute.CommunityHub)
        },
        StudioToolTile("analytics", "Creator Analytics", "Views, likes & shares", Icons.Default.Analytics) {
            onNavigate(SparkRoute.CreatorAnalytics)
        },
        StudioToolTile("jobs", "Render Jobs", "${uiState.jobs.size} total jobs", Icons.Default.WorkHistory) {
            onNavigate(SparkRoute.GenerationJobs)
        },
        StudioToolTile("credits", "Subscription", "$credits Credits • $planName", Icons.Default.Star) {
            onNavigate(SparkRoute.SubscriptionCredits)
        },
        StudioToolTile("settings", "Settings", "Studio & privacy", Icons.Default.Settings) {
            onNavigate(SparkRoute.SettingsHub)
        },
        StudioToolTile("help", "Help & Support", "Contact Spark AI", Icons.AutoMirrored.Filled.Help) {
            onNavigate(SparkRoute.SettingsSub("help"))
        }
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SparkBrushes.CosmicBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SparkLogoBadge(sizeDp = 56, showTagline = true)
        }

        item {
            SparkCard(borderColor = ChampagneGold.copy(alpha = 0.7f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Welcome back, ${account?.fullName?.ifBlank { "Creator" } ?: "Creator"}",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "$planName • $credits Credits available",
                            style = MaterialTheme.typography.bodySmall,
                            color = SoftGold
                        )
                    }
                    SparkBadge(label = account?.country?.ifBlank { "Global" } ?: "Nigeria")
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    SparkGoldButton(
                        text = "Create Movie",
                        icon = Icons.Default.MovieCreation,
                        onClick = { onNavigate(SparkRoute.CreateMovie("text_to_movie")) },
                        modifier = Modifier.weight(1f).testTag("home_create_movie_btn")
                    )
                    SparkOutlinedButton(
                        text = "POST",
                        icon = Icons.Default.PostAdd,
                        onClick = { onNavigate(SparkRoute.UniversalPost()) },
                        modifier = Modifier.testTag("home_post_btn")
                    )
                }
            }
        }

        if (isFounderAdmin) {
            item {
                SparkCard(
                    borderColor = ChampagneGold,
                    onClick = { onNavigate(SparkRoute.AdminDashboard) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = ChampagneGold)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Founder Admin Console", style = MaterialTheme.typography.titleMedium, color = ChampagneGold)
                                Text("API Config, Test Connections, Users, Credits & Moderation", style = MaterialTheme.typography.bodySmall, color = SecondaryIce)
                            }
                        }
                        SparkBadge(label = "ADMIN")
                    }
                }
            }
        }

        item {
            Text("Spark AI Studio Modules", style = MaterialTheme.typography.titleLarge)
        }

        items(studioTools.chunked(2)) { pair ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                for (tile in pair) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(onClick = tile.onClick)
                            .testTag("tile_${tile.id}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ChampagneGold.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(tile.icon, contentDescription = tile.title, tint = ChampagneGold, modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = tile.title,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = tile.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = SecondaryIce,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        if (uiState.projects.isNotEmpty()) {
            item {
                Text("Recent Projects", style = MaterialTheme.typography.titleLarge)
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(uiState.projects.take(6), key = { it.id }) { proj ->
                        SparkCard(
                            modifier = Modifier.width(240.dp),
                            onClick = { onNavigate(SparkRoute.MovieEditor(proj.id)) }
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                SparkBadge(label = proj.genre)
                                Text(
                                    text = SparkCatalog.formatDuration(proj.targetDurationSec),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SoftGold
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = proj.title,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "${proj.status} • ${proj.resolution}",
                                style = MaterialTheme.typography.bodySmall,
                                color = SecondaryIce
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SparkOutlinedButton(
                                    text = "Edit",
                                    icon = Icons.Default.Edit,
                                    onClick = { onNavigate(SparkRoute.MovieEditor(proj.id)) },
                                    modifier = Modifier.weight(1f)
                                )
                                SparkOutlinedButton(
                                    text = "Story",
                                    icon = Icons.Default.PlayArrow,
                                    onClick = { onNavigate(SparkRoute.ScriptStoryboard(proj.id)) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(24.dp))
        }
    }
}
