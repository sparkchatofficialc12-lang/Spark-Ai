package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.MediaUtils
import com.example.data.model.SparkCatalog
import com.example.data.model.UserProfile
import com.example.ui.components.SparkBadge
import com.example.ui.components.SparkCard
import com.example.ui.components.SparkGoldButton
import com.example.ui.components.SparkLogoBadge
import com.example.ui.components.SparkOutlinedButton
import com.example.ui.components.SparkStatusBanner
import com.example.ui.components.SparkTopBar
import com.example.ui.navigation.SparkRoute
import com.example.ui.theme.AmberGold
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.MidnightBlack
import com.example.ui.theme.SecondaryIce
import com.example.ui.theme.SecondarySlate
import com.example.ui.theme.SoftGold
import com.example.ui.theme.SparkBrushes
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.SocialUiState
import com.example.ui.viewmodel.StudioUiState

@Composable
fun SubscriptionCreditsScreen(
    studioState: StudioUiState,
    onCancelSubscription: () -> Unit,
    onClearBanners: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val plan = studioState.subscription
    val credits = studioState.credits
    val paymentConfigured = studioState.adminSetting?.paymentConfigured == true

    Scaffold(
        topBar = {
            SparkTopBar(
                title = "Subscription & Credits",
                subtitle = plan?.planName ?: "Free Creator",
                onBack = onBack
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(SparkBrushes.CosmicBackground).padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SparkStatusBanner(studioState.statusBanner, studioState.errorBanner, onClearBanners)
            }

            item {
                SparkCard(borderColor = ChampagneGold) {
                    Text("Credit Balance", style = MaterialTheme.typography.titleMedium, color = ChampagneGold)
                    Spacer(Modifier.height(4.dp))
                    Text("${credits?.balance ?: 0} Credits", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "Promo: ${credits?.promoCredits ?: 0} • Lifetime Earned: ${credits?.lifetimeEarned ?: 0} • Spent: ${credits?.lifetimeSpent ?: 0}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryIce
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Active Plan: ${plan?.planName ?: "Free Creator"} (${plan?.status ?: "active"})",
                        style = MaterialTheme.typography.labelLarge,
                        color = SoftGold
                    )
                    if (plan != null && plan.planId != "free" && plan.status == "active") {
                        Spacer(Modifier.height(8.dp))
                        SparkOutlinedButton(
                            text = "Cancel Paid Plan",
                            accent = ErrorRed,
                            onClick = onCancelSubscription
                        )
                    }
                }
            }

            item {
                SparkCard(borderColor = if (paymentConfigured) SuccessGreen else AmberGold) {
                    Text(
                        text = if (paymentConfigured) "Billing Gateway Connected (${studioState.adminSetting?.paymentProviderName})" else "Paid Upgrades Awaiting Payment Gateway",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (paymentConfigured) SuccessGreen else AmberGold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (paymentConfigured) {
                            "Your billing gateway is live. Choose a creator tier below."
                        } else {
                            "Spark AI never fakes card payments. Configure Google Play Billing / Paystack / Stripe on your backend to accept live subscriptions. Free tier + Admin promo credits remain active."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryIce
                    )
                }
            }

            items(SparkCatalog.planTiers, key = { it.planId }) { tier ->
                val isCurrent = (plan?.planId ?: "free") == tier.planId
                SparkCard(borderColor = if (isCurrent) ChampagneGold else SecondarySlate) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(tier.name, style = MaterialTheme.typography.titleMedium)
                            Text(tier.priceLabel, style = MaterialTheme.typography.bodySmall, color = ChampagneGold)
                        }
                        if (isCurrent) SparkBadge(label = "ACTIVE")
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("${tier.monthlyCredits} cr/mo • ${tier.maxDurationLabel} • ${tier.maxResolution}", style = MaterialTheme.typography.labelSmall, color = SoftGold)
                    Spacer(Modifier.height(6.dp))
                    tier.features.forEach { feat ->
                        Text("• $feat", style = MaterialTheme.typography.bodySmall, color = SecondaryIce)
                    }
                }
            }

            item {
                Text("Credit Ledger & Transactions", style = MaterialTheme.typography.titleMedium, color = SoftGold)
            }
            items(studioState.transactions, key = { it.id }) { tx ->
                SparkCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tx.description, style = MaterialTheme.typography.bodyMedium)
                            Text(tx.type, style = MaterialTheme.typography.labelSmall, color = SecondaryIce)
                        }
                        Text(
                            text = if (tx.amountCredits >= 0) "+${tx.amountCredits}" else "${tx.amountCredits}",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (tx.amountCredits >= 0) SuccessGreen else ErrorRed
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsHubScreen(
    studioState: StudioUiState,
    onSignOut: () -> Unit,
    onBack: () -> Unit,
    onNavigate: (SparkRoute) -> Unit
) {
    BackHandler { onBack() }
    val itemsList = listOf(
        Triple("account", "Account & Profile Info", Icons.Default.AccountCircle),
        Triple("studio", "AI Studio & Video Defaults", Icons.Default.Movie),
        Triple("notifications", "Social & Studio Notifications", Icons.Default.Notifications),
        Triple("privacy", "Privacy & Blocked Creators", Icons.Default.Lock),
        Triple("appearance", "Appearance, Data & Downloads", Icons.Default.DarkMode),
        Triple("storage", "Storage & Cache Manager", Icons.Default.Storage),
        Triple("security", "Security Center, Devices & Logs", Icons.Default.Security),
        Triple("help", "Help & Support Tickets", Icons.Default.Help),
        Triple("about", "About Spark AI & Christian Okafor", Icons.Default.Info)
    )

    Scaffold(
        topBar = {
            SparkTopBar(
                title = "Settings",
                subtitle = studioState.account?.email,
                onBack = onBack
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(SparkBrushes.CosmicBackground).padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                SparkCard(
                    borderColor = ChampagneGold.copy(alpha = 0.7f),
                    onClick = { onNavigate(SparkRoute.SparkGuide(contextSection = "general")) }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ChampagneGold)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Need help? Ask Spark Guide.", style = MaterialTheme.typography.titleSmall, color = ChampagneGold)
                            Text("Step-by-step instructions for studio, posts, privacy & security.", style = MaterialTheme.typography.bodySmall, color = SecondaryIce)
                        }
                    }
                }
            }

            item {
                SparkCard(onClick = { onNavigate(SparkRoute.RecentlyDeleted) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = ChampagneGold)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Recently Deleted (Recycle Bin)", style = MaterialTheme.typography.titleSmall)
                            Text("${studioState.recentlyDeleted.size} recoverable item(s) • 30-day window", style = MaterialTheme.typography.bodySmall, color = SoftGold)
                        }
                    }
                }
            }

            items(itemsList, key = { it.first }) { (key, title, icon) ->
                SparkCard(onClick = { onNavigate(SparkRoute.SettingsSub(key)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(icon, contentDescription = null, tint = ChampagneGold)
                        Spacer(Modifier.width(12.dp))
                        Text(title, style = MaterialTheme.typography.titleSmall)
                    }
                }
            }

            item {
                Spacer(Modifier.height(6.dp))
                SparkOutlinedButton(
                    text = "Sign Out",
                    icon = Icons.AutoMirrored.Filled.Logout,
                    onClick = onSignOut,
                    modifier = Modifier.fillMaxWidth().testTag("settings_signout_btn")
                )
            }
            item {
                SparkOutlinedButton(
                    text = "Delete Account Permanently",
                    icon = Icons.Default.DeleteForever,
                    accent = ErrorRed,
                    onClick = { onNavigate(SparkRoute.SettingsSub("delete")) },
                    modifier = Modifier.fillMaxWidth().testTag("settings_delete_account_nav")
                )
                Spacer(Modifier.height(92.dp))
            }
        }
    }
}

@Composable
fun SettingsSubScreen(
    section: String,
    studioState: StudioUiState,
    socialState: SocialUiState,
    appLockEnabled: Boolean = false,
    onToggleAppLock: (Boolean) -> Unit = {},
    onRevokeSession: (String) -> Unit = {},
    onLogoutAllOtherDevices: () -> Unit = {},
    onReportSecurityIssue: (String, String) -> Unit = { _, _ -> },
    onUpdateAccount: (String, String, String, String) -> Unit,
    onUpdateProfile: (UserProfile) -> Unit,
    onSubmitTicket: (String, String) -> Unit,
    onDeleteAccount: () -> Unit,
    onClearBanners: () -> Unit,
    onNavigate: (SparkRoute) -> Unit = {},
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val profile = studioState.profile ?: UserProfile()
    val account = studioState.account

    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            SparkTopBar(
                title = when (section) {
                    "account" -> "Account Info"
                    "studio" -> "Studio Defaults"
                    "notifications" -> "Notifications"
                    "privacy" -> "Privacy & Safety"
                    "appearance" -> "Appearance & Data"
                    "storage" -> "Storage & Cache"
                    "security" -> "Security Center & Devices"
                    "help" -> "Help & Support"
                    "delete" -> "Delete Account"
                    else -> "About Spark AI"
                },
                onBack = onBack
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(SparkBrushes.CosmicBackground).padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SparkStatusBanner(studioState.statusBanner, studioState.errorBanner, onClearBanners)
            }

            item {
                SparkCard(
                    borderColor = ChampagneGold.copy(alpha = 0.55f),
                    onClick = { onNavigate(SparkRoute.SparkGuide(contextSection = section)) }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ChampagneGold)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Need help? Ask Spark Guide.", style = MaterialTheme.typography.titleSmall, color = ChampagneGold)
                            Text("Get instant step-by-step help for $section settings.", style = MaterialTheme.typography.bodySmall, color = SecondaryIce)
                        }
                    }
                }
            }

            when (section) {
                "account" -> {
                    item {
                        var fullName by remember(account) { mutableStateOf(account?.fullName.orEmpty()) }
                        var username by remember(account) { mutableStateOf(account?.username.orEmpty()) }
                        var country by remember(account) { mutableStateOf(account?.country ?: "Nigeria") }
                        var phone by remember(account) { mutableStateOf(account?.phoneNumber.orEmpty()) }

                        SparkCard {
                            OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(value = country, onValueChange = { country = it }, label = { Text("Country") }, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(12.dp))
                            SparkGoldButton(
                                text = "Save Account Info",
                                onClick = { onUpdateAccount(fullName, username, country, phone) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                "studio" -> {
                    item {
                        var res by remember(profile) { mutableStateOf(profile.defaultResolution) }
                        var ratio by remember(profile) { mutableStateOf(profile.defaultAspectRatio) }
                        var defaultMovieLang by remember(profile) { mutableStateOf(profile.defaultMovieLanguage.ifBlank { "English" }) }
                        var autoSub by remember(profile) { mutableStateOf(profile.autoSubtitles) }
                        var autoSave by remember(profile) { mutableStateOf(profile.autoSaveProjects) }

                        SparkCard {
                            Text("Default Resolution", style = MaterialTheme.typography.titleSmall, color = SoftGold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SparkCatalog.resolutions.forEach { r ->
                                    FilterChip(
                                        selected = res == r,
                                        onClick = { res = r },
                                        label = { Text(r) },
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ChampagneGold, selectedLabelColor = MidnightBlack)
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("Default Aspect Ratio", style = MaterialTheme.typography.titleSmall, color = SoftGold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("16:9", "9:16", "1:1").forEach { ar ->
                                    FilterChip(
                                        selected = ratio == ar,
                                        onClick = { ratio = ar },
                                        label = { Text(ar) },
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ChampagneGold, selectedLabelColor = MidnightBlack)
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("Default Movie Language", style = MaterialTheme.typography.titleSmall, color = SoftGold)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(SparkCatalog.movieLanguages.take(7)) { langOpt ->
                                    FilterChip(
                                        selected = defaultMovieLang.equals(langOpt, ignoreCase = true),
                                        onClick = { defaultMovieLang = langOpt },
                                        label = { Text(langOpt) },
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ChampagneGold, selectedLabelColor = MidnightBlack)
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            OutlinedTextField(
                                value = defaultMovieLang,
                                onValueChange = { defaultMovieLang = it },
                                label = { Text("Custom Default Movie Language") },
                                placeholder = { Text("e.g., Nigerian Pidgin, Yoruba, French...") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            SettingToggle("Auto-generate subtitles", autoSub) { autoSub = it }
                            SettingToggle("Auto-save studio drafts", autoSave) { autoSave = it }
                            Spacer(Modifier.height(10.dp))
                            SparkGoldButton(
                                text = "Save Studio Defaults",
                                onClick = {
                                    onUpdateProfile(
                                        profile.copy(
                                            defaultResolution = res,
                                            defaultAspectRatio = ratio,
                                            defaultMovieLanguage = defaultMovieLang.trim().ifBlank { "English" },
                                            autoSubtitles = autoSub,
                                            autoSaveProjects = autoSave
                                        )
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                "notifications" -> {
                    item {
                        var master by remember(profile) { mutableStateOf(profile.notificationsEnabled) }
                        var likes by remember(profile) { mutableStateOf(profile.notifyLikes) }
                        var comments by remember(profile) { mutableStateOf(profile.notifyComments) }
                        var followers by remember(profile) { mutableStateOf(profile.notifyFollowers) }
                        var messages by remember(profile) { mutableStateOf(profile.notifyMessages) }
                        var remixes by remember(profile) { mutableStateOf(profile.notifyRemixes) }

                        SparkCard {
                            SettingToggle("All Notifications", master) { master = it }
                            SettingToggle("Post Likes", likes) { likes = it }
                            SettingToggle("Comments & Replies", comments) { comments = it }
                            SettingToggle("New Followers", followers) { followers = it }
                            SettingToggle("Direct Messages", messages) { messages = it }
                            SettingToggle("Template Remixes", remixes) { remixes = it }
                            Spacer(Modifier.height(10.dp))
                            SparkGoldButton(
                                text = "Save Notification Settings",
                                onClick = {
                                    onUpdateProfile(
                                        profile.copy(
                                            notificationsEnabled = master,
                                            notifyLikes = likes,
                                            notifyComments = comments,
                                            notifyFollowers = followers,
                                            notifyMessages = messages,
                                            notifyRemixes = remixes
                                        )
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                "privacy" -> {
                    item {
                        var privacy by remember(profile) { mutableStateOf(profile.defaultPrivacy) }
                        var allowDl by remember(profile) { mutableStateOf(profile.allowDownloads) }
                        SparkCard {
                            Text("Default Post Visibility", style = MaterialTheme.typography.titleSmall, color = SoftGold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("public", "private", "unlisted").forEach { v ->
                                    FilterChip(
                                        selected = privacy == v,
                                        onClick = { privacy = v },
                                        label = { Text(v.replaceFirstChar { it.uppercase() }) },
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ChampagneGold, selectedLabelColor = MidnightBlack)
                                    )
                                }
                            }
                            SettingToggle("Allow others to download my public videos", allowDl) { allowDl = it }
                            Spacer(Modifier.height(8.dp))
                            Text("Blocked Creators (${socialState.blockedCreatorIds.size})", style = MaterialTheme.typography.bodySmall, color = SecondaryIce)
                            Spacer(Modifier.height(10.dp))
                            SparkGoldButton(
                                text = "Save Privacy Settings",
                                onClick = { onUpdateProfile(profile.copy(defaultPrivacy = privacy, allowDownloads = allowDl)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                "appearance" -> {
                    item {
                        var dark by remember(profile) { mutableStateOf(profile.darkMode) }
                        var autoplay by remember(profile) { mutableStateOf(profile.videoAutoplay) }
                        var dataSaver by remember(profile) { mutableStateOf(profile.dataUsageSaver) }
                        var wifiDl by remember(profile) { mutableStateOf(profile.downloadWifiOnly) }
                        var lang by remember(profile) { mutableStateOf(profile.appLanguage) }

                        SparkCard {
                            SettingToggle("Dark Cinema Mode (Midnight Black & Gold)", dark) { dark = it }
                            SettingToggle("Autoplay Videos in Feeds", autoplay) { autoplay = it }
                            SettingToggle("Data Saver Mode", dataSaver) { dataSaver = it }
                            SettingToggle("Download on Wi-Fi Only", wifiDl) { wifiDl = it }
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(value = lang, onValueChange = { lang = it }, label = { Text("App Language") }, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(10.dp))
                            SparkGoldButton(
                                text = "Apply Appearance & Data Settings",
                                onClick = {
                                    onUpdateProfile(
                                        profile.copy(
                                            darkMode = dark,
                                            videoAutoplay = autoplay,
                                            dataUsageSaver = dataSaver,
                                            downloadWifiOnly = wifiDl,
                                            appLanguage = lang
                                        )
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                "storage" -> {
                    item {
                        var cacheBytes by remember { mutableStateOf(MediaUtils.calculateLocalCacheSizeBytes(context)) }
                        SparkCard {
                            Text("Local Cache Usage", style = MaterialTheme.typography.titleMedium)
                            Text("${cacheBytes / 1024} KB temporary cache", style = MaterialTheme.typography.bodySmall, color = SoftGold)
                            Spacer(Modifier.height(10.dp))
                            SparkGoldButton(
                                text = "Clear Temporary Cache",
                                icon = Icons.Default.CleaningServices,
                                onClick = {
                                    MediaUtils.clearTemporaryCache(context)
                                    cacheBytes = MediaUtils.calculateLocalCacheSizeBytes(context)
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                "security" -> {
                    item {
                        SparkCard(borderColor = ChampagneGold) {
                            Text("Google Account & 2FA Protection", style = MaterialTheme.typography.titleMedium, color = ChampagneGold)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Signed in via verified Google Credential Manager (${studioState.account?.email.orEmpty()}). Password, passkeys, and 2-step verification are managed directly by Google Security.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SecondaryIce
                            )
                            Spacer(Modifier.height(8.dp))
                            SettingToggle("Enable Spark AI Device App Lock", appLockEnabled, onToggleAppLock)
                        }
                    }

                    item {
                        SparkCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Devices, contentDescription = null, tint = ChampagneGold)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Active Sessions (${studioState.activeSessions.count { it.status == "active" }})", style = MaterialTheme.typography.titleMedium)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            SparkOutlinedButton(
                                text = "Log Out All Other Devices",
                                accent = ErrorRed,
                                onClick = onLogoutAllOtherDevices,
                                modifier = Modifier.fillMaxWidth().testTag("logout_all_devices_btn")
                            )
                        }
                    }

                    items(studioState.activeSessions, key = { it.id }) { sess ->
                        val isCurrent = sess.id == studioState.currentSessionId
                        SparkCard(borderColor = if (isCurrent) ChampagneGold else SecondarySlate) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(sess.deviceLabel.ifBlank { "Android Device" }, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "${sess.platform} • ${sess.status.uppercase()}${if (isCurrent) " • THIS DEVICE" else ""}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (sess.status == "active") SuccessGreen else ErrorRed
                                    )
                                }
                                if (!isCurrent && sess.status == "active") {
                                    SparkOutlinedButton(
                                        text = "Revoke",
                                        accent = ErrorRed,
                                        onClick = { onRevokeSession(sess.id) }
                                    )
                                }
                            }
                        }
                    }

                    if (studioState.securityAlerts.isNotEmpty()) {
                        item {
                            Text("Suspicious & Security Alerts", style = MaterialTheme.typography.titleMedium, color = AmberGold)
                        }
                        items(studioState.securityAlerts, key = { it.id }) { alert ->
                            SparkCard(borderColor = if (alert.resolvedFlag) SecondarySlate else AmberGold) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(alert.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                    SparkBadge(label = alert.severity.uppercase(), color = if (alert.resolvedFlag) SuccessGreen else AmberGold)
                                }
                                Text(alert.description, style = MaterialTheme.typography.bodySmall, color = SecondaryIce)
                            }
                        }
                    }

                    item {
                        var reportTitle by remember { mutableStateOf("") }
                        var reportDesc by remember { mutableStateOf("") }
                        SparkCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ReportProblem, contentDescription = null, tint = ChampagneGold)
                                Spacer(Modifier.width(8.dp))
                                Text("Report Security Issue", style = MaterialTheme.typography.titleMedium)
                            }
                            Spacer(Modifier.height(6.dp))
                            OutlinedTextField(value = reportTitle, onValueChange = { reportTitle = it }, label = { Text("Issue Title") }, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(6.dp))
                            OutlinedTextField(value = reportDesc, onValueChange = { reportDesc = it }, label = { Text("Describe suspicious login, impersonation, or bug") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(8.dp))
                            SparkGoldButton(
                                text = "Submit Security Alert",
                                onClick = {
                                    if (reportDesc.isNotBlank()) {
                                        onReportSecurityIssue(reportTitle, reportDesc)
                                        reportTitle = ""
                                        reportDesc = ""
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    item {
                        Text("Recent Login & Security Logs", style = MaterialTheme.typography.titleMedium, color = SoftGold)
                    }
                    items(studioState.securityLogs, key = { it.id }) { log ->
                        SparkCard {
                            Text(log.event, style = MaterialTheme.typography.titleSmall)
                            Text("${log.deviceInfo} • ${log.status}", style = MaterialTheme.typography.bodySmall, color = SecondaryIce)
                        }
                    }
                }
                "help" -> {
                    item {
                        var subject by remember { mutableStateOf("") }
                        var msg by remember { mutableStateOf("") }
                        SparkCard {
                            OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Subject") }, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(value = msg, onValueChange = { msg = it }, label = { Text("How can Spark AI help?") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(10.dp))
                            SparkGoldButton(
                                text = "Submit Support Ticket",
                                onClick = {
                                    if (subject.isNotBlank() && msg.isNotBlank()) {
                                        onSubmitTicket(subject, msg)
                                        subject = ""
                                        msg = ""
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    items(studioState.tickets, key = { it.id }) { t ->
                        SparkCard {
                            Text(t.subject, style = MaterialTheme.typography.titleSmall)
                            Text("Status: ${t.status}", style = MaterialTheme.typography.labelSmall, color = ChampagneGold)
                            Text(t.message, style = MaterialTheme.typography.bodySmall, color = SecondaryIce)
                        }
                    }
                }
                "delete" -> {
                    item {
                        SparkCard(borderColor = ErrorRed) {
                            Text("Permanent Account Deletion", style = MaterialTheme.typography.titleMedium, color = ErrorRed)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Deleting your Spark AI account permanently removes your profile, movie projects, scripts, credits, and signs you out.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SecondaryIce
                            )
                            Spacer(Modifier.height(12.dp))
                            SparkOutlinedButton(
                                text = "Delete My Account",
                                accent = ErrorRed,
                                onClick = { showDeleteConfirm = true },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                else -> {
                    item {
                        SparkCard(borderColor = ChampagneGold) {
                            SparkLogoBadge(sizeDp = 52, showTagline = true)
                            Spacer(Modifier.height(12.dp))
                            Text("Founder: Christian Okafor (Sparkling)", style = MaterialTheme.typography.titleSmall, color = ChampagneGold)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Spark AI & Spark Social unite cinematic AI screenplay generation, storyboard design, voice synthesis, music video production, timeline editing, and creator social sharing.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SecondaryIce
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(92.dp)) }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = SecondarySlate,
            title = { Text("Confirm Account Deletion", color = ErrorRed) },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDeleteAccount()
                }) { Text("Delete Forever", color = ErrorRed) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f).padding(end = 8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedThumbColor = MidnightBlack, checkedTrackColor = ChampagneGold)
        )
    }
}
