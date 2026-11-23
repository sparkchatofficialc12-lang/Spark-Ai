package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.MidnightBlack
import com.example.ui.theme.SecondaryIce
import com.example.ui.theme.SecondarySlate
import com.example.ui.theme.SoftGold
import com.example.ui.theme.SteelBorder
import com.example.ui.theme.SuccessGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private object SparkBitmapCache {
    val lru = object : LruCache<String, Bitmap>(24) {}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SparkTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    credits: Int? = null,
    unreadNotifications: Int = 0,
    onCreditsClick: (() -> Unit)? = null,
    onNotificationsClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.96f),
            titleContentColor = MaterialTheme.colorScheme.onBackground
        ),
        navigationIcon = {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("top_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = ChampagneGold
                    )
                }
            }
        },
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = ChampagneGold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        actions = {
            if (credits != null && onCreditsClick != null) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = ChampagneGold.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .heightIn(min = 38.dp)
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onCreditsClick)
                        .testTag("credits_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Credits",
                            tint = ChampagneGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "$credits cr",
                            style = MaterialTheme.typography.labelMedium,
                            color = ChampagneGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            if (onNotificationsClick != null) {
                IconButton(
                    onClick = onNotificationsClick,
                    modifier = Modifier.testTag("notifications_bell_button")
                ) {
                    BadgedBox(
                        badge = {
                            if (unreadNotifications > 0) {
                                Badge(containerColor = ChampagneGold, contentColor = MidnightBlack) {
                                    Text(unreadNotifications.coerceAtMost(99).toString())
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
            actions()
        }
    )
}

@Composable
fun SparkGoldButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = ChampagneGold,
            contentColor = MidnightBlack,
            disabledContainerColor = ChampagneGold.copy(alpha = 0.35f),
            disabledContentColor = MidnightBlack.copy(alpha = 0.6f)
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
        modifier = modifier
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SparkOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accent: Color = ChampagneGold,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = if (enabled) 0.75f else 0.3f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = accent),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 11.dp),
        modifier = modifier
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SparkCard(
    modifier: Modifier = Modifier,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, borderColor.copy(alpha = 0.65f))
        ) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, borderColor.copy(alpha = 0.65f))
        ) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    }
}

@Composable
fun SparkBadge(
    label: String,
    color: Color = ChampagneGold,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.55f)),
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun SparkLogoBadge(
    sizeDp: Int = 64,
    showTagline: Boolean = true
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(sizeDp.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MidnightBlack)
                .border(1.5.dp, ChampagneGold, RoundedCornerShape(16.dp))
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_spark_logo),
                contentDescription = "Spark AI Logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                text = "SPARK AI",
                style = MaterialTheme.typography.headlineMedium,
                color = ChampagneGold,
                fontWeight = FontWeight.Bold
            )
            if (showTagline) {
                Text(
                    text = "Imagine It. Create It. Watch It.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SoftGold
                )
            }
        }
    }
}

@Composable
fun Base64Thumbnail(
    base64Data: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    placeholderIcon: ImageVector = Icons.Default.Movie
) {
    val cacheKey = "${base64Data.length}_${base64Data.hashCode()}"
    val bitmap by produceState<Bitmap?>(
        initialValue = if (base64Data.isBlank()) null else SparkBitmapCache.lru.get(cacheKey),
        key1 = base64Data
    ) {
        if (base64Data.isBlank()) {
            value = null
        } else {
            val cached = SparkBitmapCache.lru.get(cacheKey)
            if (cached != null) {
                value = cached
            } else {
                val decoded = withContext(Dispatchers.Default) {
                    try {
                        val clean = base64Data.substringAfter("base64,", base64Data)
                        val bytes = android.util.Base64.decode(clean, android.util.Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    } catch (_: Exception) {
                        null
                    }
                }
                if (decoded != null) {
                    SparkBitmapCache.lru.put(cacheKey, decoded)
                }
                value = decoded
            }
        }
    }
    val decodedBitmap = bitmap
    if (decodedBitmap != null) {
        Image(
            bitmap = decodedBitmap.asImageBitmap(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier.background(SecondarySlate),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = placeholderIcon,
                contentDescription = contentDescription,
                tint = ChampagneGold.copy(alpha = 0.7f),
                modifier = Modifier.size(34.dp)
            )
        }
    }
}

@Composable
fun SparkStatusBanner(
    statusMessage: String?,
    errorMessage: String?,
    onDismiss: () -> Unit
) {
    if (errorMessage != null) {
        Surface(
            color = ErrorRed.copy(alpha = 0.16f),
            border = BorderStroke(1.dp, ErrorRed),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clickable(onClick = onDismiss)
        ) {
            Text(
                text = "$errorMessage  (Tap to dismiss)",
                style = MaterialTheme.typography.bodySmall,
                color = ErrorRed,
                modifier = Modifier.padding(12.dp)
            )
        }
    } else if (statusMessage != null) {
        Surface(
            color = SuccessGreen.copy(alpha = 0.14f),
            border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.7f)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clickable(onClick = onDismiss)
        ) {
            Text(
                text = "$statusMessage  (Tap to dismiss)",
                style = MaterialTheme.typography.bodySmall,
                color = SuccessGreen,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
fun SparkBusyOverlay(label: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightBlack.copy(alpha = 0.82f))
            .clickable(enabled = true) {},
        contentAlignment = Alignment.Center
    ) {
        SparkCard(
            borderColor = ChampagneGold,
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(0.88f)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    color = ChampagneGold,
                    trackColor = SteelBorder,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Spark AI Studio",
                        style = MaterialTheme.typography.titleMedium,
                        color = ChampagneGold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = label.ifBlank { "Working..." },
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryIce
                    )
                }
            }
        }
    }
}

@Composable
fun CreatorAvatarCircle(name: String, modifier: Modifier = Modifier) {
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "S"
    Box(
        modifier = modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(ChampagneGold.copy(alpha = 0.2f))
            .border(1.dp, ChampagneGold, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.titleSmall,
            color = ChampagneGold,
            fontWeight = FontWeight.Bold
        )
    }
}
