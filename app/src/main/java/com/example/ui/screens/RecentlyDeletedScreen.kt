package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.RecentlyDeletedItem
import com.example.ui.components.Base64Thumbnail
import com.example.ui.components.SparkBadge
import com.example.ui.components.SparkBusyOverlay
import com.example.ui.components.SparkCard
import com.example.ui.components.SparkGoldButton
import com.example.ui.components.SparkOutlinedButton
import com.example.ui.components.SparkStatusBanner
import com.example.ui.components.SparkTopBar
import com.example.ui.navigation.SparkRoute
import com.example.ui.theme.AmberGold
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SecondaryIce
import com.example.ui.theme.SecondarySlate
import com.example.ui.theme.SoftGold
import com.example.ui.theme.SparkBrushes
import com.example.ui.viewmodel.StudioUiState

@Composable
fun RecentlyDeletedScreen(
    studioState: StudioUiState,
    onRestoreItem: (RecentlyDeletedItem) -> Unit,
    onPermanentDelete: (String) -> Unit,
    onPurgeExpired: () -> Unit,
    onClearBanners: () -> Unit,
    onBack: () -> Unit,
    onNavigate: (SparkRoute) -> Unit
) {
    BackHandler { onBack() }
    var confirmForeverItem by remember { mutableStateOf<RecentlyDeletedItem?>(null) }
    val nowMs = remember(studioState.recentlyDeleted.size) { System.currentTimeMillis() }

    Scaffold(
        topBar = {
            SparkTopBar(
                title = "Recently Deleted",
                subtitle = "30-day Recycle Bin recovery window",
                onBack = onBack
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SparkBrushes.CosmicBackground)
                .padding(innerPadding)
        ) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    SparkStatusBanner(studioState.statusBanner, studioState.errorBanner, onClearBanners)
                }

                item {
                    SparkCard(borderColor = ChampagneGold.copy(alpha = 0.6f)) {
                        Text("Create Fresh After Cleanup", style = MaterialTheme.typography.titleMedium, color = ChampagneGold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Deleted posts, movies, and projects stay recoverable here for 30 days. You can restore anytime or start a new creation right away.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryIce
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            SparkGoldButton(
                                text = "Create Another Video",
                                icon = Icons.Default.MovieCreation,
                                onClick = { onNavigate(SparkRoute.CreateMovie("text_to_movie")) },
                                modifier = Modifier.weight(1f).testTag("recycle_create_another_btn")
                            )
                            SparkOutlinedButton(
                                text = "Post Again",
                                icon = Icons.Default.PostAdd,
                                onClick = { onNavigate(SparkRoute.UniversalPost()) },
                                modifier = Modifier.weight(1f).testTag("recycle_post_again_btn")
                            )
                        }
                        if (studioState.recentlyDeleted.any { it.expiresAtMs in 1..nowMs }) {
                            Spacer(Modifier.height(8.dp))
                            SparkOutlinedButton(
                                text = "Purge Expired Items (>30 Days)",
                                accent = AmberGold,
                                onClick = onPurgeExpired,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                if (studioState.recentlyDeleted.isEmpty()) {
                    item {
                        SparkCard {
                            Text("Recycle Bin is Empty", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "When you delete a post, movie, or project, it moves here safely for 30 days before permanent removal.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SecondaryIce
                            )
                        }
                    }
                } else {
                    items(studioState.recentlyDeleted, key = { it.id }) { item ->
                        val daysLeft = ((item.expiresAtMs - nowMs) / (24L * 60L * 60L * 1000L)).coerceAtLeast(0L)
                        SparkCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Base64Thumbnail(
                                    base64Data = item.thumbnailData,
                                    contentDescription = item.title,
                                    modifier = Modifier
                                        .width(84.dp)
                                        .height(62.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        SparkBadge(label = item.itemType.uppercase())
                                        SparkBadge(
                                            label = if (daysLeft > 0) "${daysLeft}d left" else "Expired",
                                            color = if (daysLeft > 3) ChampagneGold else AmberGold
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(item.title, style = MaterialTheme.typography.titleMedium)
                                    Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = SoftGold)
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                SparkGoldButton(
                                    text = "Restore",
                                    icon = Icons.Default.RestoreFromTrash,
                                    onClick = { onRestoreItem(item) },
                                    modifier = Modifier.weight(1f).testTag("restore_trash_${item.id}")
                                )
                                SparkOutlinedButton(
                                    text = "Delete Forever",
                                    icon = Icons.Default.DeleteForever,
                                    accent = ErrorRed,
                                    onClick = { confirmForeverItem = item },
                                    modifier = Modifier.weight(1f).testTag("forever_trash_${item.id}")
                                )
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }

            if (studioState.isBusy) {
                SparkBusyOverlay(studioState.busyLabel)
            }
        }
    }

    confirmForeverItem?.let { item ->
        AlertDialog(
            onDismissRequest = { confirmForeverItem = null },
            containerColor = SecondarySlate,
            title = { Text("Delete Permanently?", color = ErrorRed) },
            text = { Text("\"${item.title}\" will be permanently removed and cannot be recovered.") },
            confirmButton = {
                TextButton(onClick = {
                    onPermanentDelete(item.id)
                    confirmForeverItem = null
                }) { Text("Delete Forever", color = ErrorRed) }
            },
            dismissButton = {
                TextButton(onClick = { confirmForeverItem = null }) { Text("Cancel") }
            }
        )
    }
}
