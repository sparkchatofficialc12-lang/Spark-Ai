package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.MediaUtils
import com.example.data.model.CommunityCatalog
import com.example.data.model.MovieItem
import com.example.data.model.SocialPost
import com.example.ui.components.SparkOutlinedButton
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.IceWhite
import com.example.ui.theme.MidnightBlack
import com.example.ui.theme.SecondaryIce
import com.example.ui.theme.SecondarySlate
import com.example.ui.theme.SoftGold

@Composable
fun PostMoreMenuAndDialogs(
    post: SocialPost,
    isOwner: Boolean,
    isSaved: Boolean,
    myMovies: List<MovieItem>,
    onToggleSave: (SocialPost) -> Unit,
    onShare: (SocialPost) -> Unit,
    onEditPost: (postId: String, title: String, caption: String, hashtags: String, visibility: String, allowComments: Boolean, allowReuse: Boolean, allowDownload: Boolean) -> Unit,
    onReplaceVideo: (post: SocialPost, newVideoUrl: String, newThumb: String, durationSec: Int, res: String, keepLink: Boolean, title: String, caption: String) -> Unit,
    onSoftDeletePost: (SocialPost) -> Unit,
    onChangePrivacy: (String, String) -> Unit,
    onToggleComments: (String, Boolean) -> Unit,
    onUpdateTemplatePermission: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showReplaceDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var downloadNotice by remember { mutableStateOf<String?>(null) }

    Box {
        IconButton(
            onClick = { menuExpanded = true },
            modifier = Modifier.testTag("post_more_menu_${post.id}")
        ) {
            Icon(Icons.Default.MoreVert, contentDescription = "Post options", tint = IceWhite)
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            containerColor = SecondarySlate
        ) {
            if (isOwner) {
                DropdownMenuItem(
                    text = { Text("Edit Post") },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = ChampagneGold) },
                    onClick = {
                        menuExpanded = false
                        showEditDialog = true
                    }
                )
                DropdownMenuItem(
                    text = { Text("Replace Video") },
                    leadingIcon = { Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = ChampagneGold) },
                    onClick = {
                        menuExpanded = false
                        showReplaceDialog = true
                    }
                )
                DropdownMenuItem(
                    text = { Text("Change Privacy (${post.visibility})") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = SoftGold) },
                    onClick = {
                        menuExpanded = false
                        showPrivacyDialog = true
                    }
                )
                DropdownMenuItem(
                    text = { Text(if (post.allowComments) "Disable Comments" else "Enable Comments") },
                    leadingIcon = { Icon(Icons.Default.Comment, contentDescription = null, tint = SoftGold) },
                    onClick = {
                        menuExpanded = false
                        onToggleComments(post.id, !post.allowComments)
                    }
                )
                DropdownMenuItem(
                    text = { Text(if (post.allowReuse) "Disable Template Remix" else "Enable as Template") },
                    leadingIcon = { Icon(Icons.Default.Style, contentDescription = null, tint = SoftGold) },
                    onClick = {
                        menuExpanded = false
                        onUpdateTemplatePermission(post.id, !post.allowReuse)
                    }
                )
            }
            DropdownMenuItem(
                text = { Text(if (isSaved) "Remove from Saved" else "Save Post") },
                leadingIcon = {
                    Icon(
                        if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = null,
                        tint = ChampagneGold
                    )
                },
                onClick = {
                    menuExpanded = false
                    onToggleSave(post)
                }
            )
            if (post.allowDownload && post.previewVideoUrl.isNotBlank()) {
                DropdownMenuItem(
                    text = { Text("Download Video") },
                    leadingIcon = { Icon(Icons.Default.Download, contentDescription = null, tint = ChampagneGold) },
                    onClick = {
                        menuExpanded = false
                        MediaUtils.shareMovieTextOrLink(context, post.title, post.previewVideoUrl, post.caption)
                        downloadNotice = "Opened video export/download for \"${post.title}\"."
                    }
                )
            }
            DropdownMenuItem(
                text = { Text("Share") },
                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = ChampagneGold) },
                onClick = {
                    menuExpanded = false
                    onShare(post)
                }
            )
            if (isOwner) {
                DropdownMenuItem(
                    text = { Text("Delete Post (Move to Bin)", color = ErrorRed) },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorRed) },
                    onClick = {
                        menuExpanded = false
                        showDeleteConfirm = true
                    }
                )
            }
        }
    }

    if (showEditDialog) {
        var title by remember(post.id) { mutableStateOf(post.title) }
        var caption by remember(post.id) { mutableStateOf(post.caption) }
        var hashtags by remember(post.id) { mutableStateOf(post.hashtags) }
        var visibility by remember(post.id) { mutableStateOf(post.visibility) }
        var allowComments by remember(post.id) { mutableStateOf(post.allowComments) }
        var allowReuse by remember(post.id) { mutableStateOf(post.allowReuse) }
        var allowDownload by remember(post.id) { mutableStateOf(post.allowDownload) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = SecondarySlate,
            title = { Text("Edit Post") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = caption, onValueChange = { caption = it }, label = { Text("Caption") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = hashtags, onValueChange = { hashtags = it }, label = { Text("Hashtags") }, modifier = Modifier.fillMaxWidth())
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(CommunityCatalog.visibilities) { v ->
                            FilterChip(
                                selected = visibility == v,
                                onClick = { visibility = v },
                                label = { Text(v.replaceFirstChar { it.uppercase() }) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ChampagneGold, selectedLabelColor = MidnightBlack)
                            )
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(8.dp)).clickable { allowComments = !allowComments }.padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Allow Comments", style = MaterialTheme.typography.bodySmall)
                        Switch(checked = allowComments, onCheckedChange = { allowComments = it }, colors = SwitchDefaults.colors(checkedThumbColor = MidnightBlack, checkedTrackColor = ChampagneGold))
                    }
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(8.dp)).clickable { allowReuse = !allowReuse }.padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Allow Template Remix", style = MaterialTheme.typography.bodySmall)
                        Switch(checked = allowReuse, onCheckedChange = { allowReuse = it }, colors = SwitchDefaults.colors(checkedThumbColor = MidnightBlack, checkedTrackColor = ChampagneGold))
                    }
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(8.dp)).clickable { allowDownload = !allowDownload }.padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Allow Download", style = MaterialTheme.typography.bodySmall)
                        Switch(checked = allowDownload, onCheckedChange = { allowDownload = it }, colors = SwitchDefaults.colors(checkedThumbColor = MidnightBlack, checkedTrackColor = ChampagneGold))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onEditPost(post.id, title, caption, hashtags, visibility, allowComments, allowReuse, allowDownload)
                    showEditDialog = false
                }) { Text("Save Changes", color = ChampagneGold) }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showReplaceDialog) {
        var newVideoUrl by remember(post.id) { mutableStateOf(post.previewVideoUrl) }
        var newThumb by remember(post.id) { mutableStateOf(post.thumbnailData) }
        var newDuration by remember(post.id) { mutableIntStateOf(post.durationSec.coerceAtLeast(15)) }
        var newRes by remember(post.id) { mutableStateOf(post.resolution) }
        var newTitle by remember(post.id) { mutableStateOf(post.title) }
        var newCaption by remember(post.id) { mutableStateOf(post.caption) }
        var keepOriginalLink by remember { mutableStateOf(true) }

        val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            if (uri != null) {
                newVideoUrl = uri.toString()
            }
        }

        AlertDialog(
            onDismissRequest = { showReplaceDialog = false },
            containerColor = SecondarySlate,
            title = { Text("Replace Post Video") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Choose a replacement video from your phone or My Creations:", style = MaterialTheme.typography.bodySmall, color = SecondaryIce)
                    SparkOutlinedButton(
                        text = "Pick Video from Phone",
                        onClick = {
                            videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (myMovies.any { it.videoUrl.isNotBlank() }) {
                        Text("Or select from My Creations:", style = MaterialTheme.typography.labelSmall, color = SoftGold)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(myMovies.filter { it.videoUrl.isNotBlank() }, key = { it.id }) { m ->
                                FilterChip(
                                    selected = newVideoUrl == m.videoUrl,
                                    onClick = {
                                        newVideoUrl = m.videoUrl
                                        if (m.thumbnailData.isNotBlank()) newThumb = m.thumbnailData
                                        if (m.durationSec > 0) newDuration = m.durationSec
                                        if (m.resolution.isNotBlank()) newRes = m.resolution
                                    },
                                    label = { Text(m.title.take(18)) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ChampagneGold, selectedLabelColor = MidnightBlack)
                                )
                            }
                        }
                    }
                    OutlinedTextField(value = newTitle, onValueChange = { newTitle = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = newVideoUrl, onValueChange = { newVideoUrl = it }, label = { Text("Video Source URI / HTTPS URL") }, modifier = Modifier.fillMaxWidth())
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(8.dp)).clickable { keepOriginalLink = !keepOriginalLink }.padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f).padding(end = 8.dp)) {
                            Text("Keep Original Post Link", style = MaterialTheme.typography.bodySmall)
                            Text(
                                if (keepOriginalLink) "Updates this post in place & preserves views/likes/comments." else "Publishes replacement as a brand-new post.",
                                style = MaterialTheme.typography.labelSmall,
                                color = SecondaryIce
                            )
                        }
                        Switch(checked = keepOriginalLink, onCheckedChange = { keepOriginalLink = it }, colors = SwitchDefaults.colors(checkedThumbColor = MidnightBlack, checkedTrackColor = ChampagneGold))
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = newVideoUrl.isNotBlank(),
                    onClick = {
                        onReplaceVideo(post, newVideoUrl, newThumb, newDuration, newRes, keepOriginalLink, newTitle, newCaption)
                        showReplaceDialog = false
                    }
                ) { Text("Replace Video", color = ChampagneGold) }
            },
            dismissButton = {
                TextButton(onClick = { showReplaceDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showPrivacyDialog) {
        var vis by remember(post.id) { mutableStateOf(post.visibility) }
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            containerColor = SecondarySlate,
            title = { Text("Change Post Privacy") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CommunityCatalog.visibilities.forEach { v ->
                        FilterChip(
                            selected = vis == v,
                            onClick = { vis = v },
                            label = { Text(v.replaceFirstChar { it.uppercase() }) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ChampagneGold, selectedLabelColor = MidnightBlack)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onChangePrivacy(post.id, vis)
                    showPrivacyDialog = false
                }) { Text("Update Privacy", color = ChampagneGold) }
            },
            dismissButton = {
                TextButton(onClick = { showPrivacyDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = SecondarySlate,
            title = { Text("Delete \"${post.title}\"?", color = ErrorRed) },
            text = { Text("This post will move to Recently Deleted (Recycle Bin) where you can restore it within 30 days.") },
            confirmButton = {
                TextButton(onClick = {
                    onSoftDeletePost(post)
                    showDeleteConfirm = false
                }) { Text("Move to Recycle Bin", color = ErrorRed) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    downloadNotice?.let { msg ->
        AlertDialog(
            onDismissRequest = { downloadNotice = null },
            containerColor = SecondarySlate,
            title = { Text("Download Ready") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { downloadNotice = null }) { Text("OK", color = ChampagneGold) }
            }
        )
    }
}
