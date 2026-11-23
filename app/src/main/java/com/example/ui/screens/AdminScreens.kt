package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AdminSetting
import com.example.data.model.GenerationJob
import com.example.data.model.SupportTicket
import com.example.data.model.TemplateReport
import com.example.data.model.UserAccount
import com.example.ui.components.SparkBadge
import com.example.ui.components.SparkCard
import com.example.ui.components.SparkGoldButton
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
import com.example.ui.viewmodel.AdminUiState
import com.example.ui.viewmodel.ApiHealthStatus

@Composable
fun AdminDashboardScreen(
    adminState: AdminUiState,
    onToggleSuspend: (UserAccount) -> Unit,
    onAdjustCredits: (String, Int, String) -> Unit,
    onRetryJob: (GenerationJob) -> Unit,
    onResolveTicket: (SupportTicket, String) -> Unit,
    onModerateReport: (TemplateReport, Boolean) -> Unit,
    onClearMessages: () -> Unit,
    onBack: () -> Unit,
    onNavigate: (SparkRoute) -> Unit
) {
    BackHandler { onBack() }

    if (!adminState.isFounderAdmin) {
        Scaffold(topBar = { SparkTopBar(title = "Admin Restricted", onBack = onBack) }) { inner ->
            Column(
                modifier = Modifier.fillMaxSize().background(SparkBrushes.CosmicBackground).padding(inner).padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                SparkCard(borderColor = ErrorRed) {
                    Text("Founder Admin Access Required", style = MaterialTheme.typography.titleLarge, color = ErrorRed)
                    Spacer(Modifier.height(6.dp))
                    Text("Only verified Founder Christian Okafor can access Spark AI admin tools.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        return
    }

    var tab by rememberSaveable { mutableIntStateOf(0) } // 0 Users, 1 Jobs, 2 Tickets, 3 Reports
    var creditTargetUser by remember { mutableStateOf<UserAccount?>(null) }
    var creditDeltaText by remember { mutableStateOf("20") }
    var creditReason by remember { mutableStateOf("Founder promo grant") }

    Scaffold(
        topBar = {
            SparkTopBar(
                title = "Founder Admin Console",
                subtitle = "Christian Okafor • Spark AI",
                onBack = onBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SparkBrushes.CosmicBackground)
                .padding(innerPadding)
        ) {
            SparkStatusBanner(adminState.statusMessage, adminState.errorMessage, onClearMessages)

            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SparkGoldButton(
                    text = "API Configuration & Live Health Checks",
                    icon = Icons.Default.Api,
                    onClick = { onNavigate(SparkRoute.AdminApiConfig) },
                    modifier = Modifier.fillMaxWidth().testTag("admin_open_api_config_btn")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    SparkOutlinedButton(
                        text = "Cybersecurity Center",
                        icon = Icons.Default.Security,
                        onClick = { onNavigate(SparkRoute.AdminSecurity) },
                        modifier = Modifier.weight(1f).testTag("admin_open_security_btn")
                    )
                    SparkOutlinedButton(
                        text = "Spark Guide KB",
                        icon = Icons.Default.AutoAwesome,
                        onClick = { onNavigate(SparkRoute.AdminGuideArticles) },
                        modifier = Modifier.weight(1f).testTag("admin_open_guide_kb_btn")
                    )
                }
            }

            TabRow(
                selectedTabIndex = tab,
                containerColor = MidnightBlack,
                contentColor = ChampagneGold,
                indicator = { pos ->
                    TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(pos[tab]), color = ChampagneGold)
                }
            ) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Users (${adminState.users.size})") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Jobs (${adminState.jobs.size})") })
                Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Tickets (${adminState.tickets.size})") })
                Tab(selected = tab == 3, onClick = { tab = 3 }, text = { Text("Reports (${adminState.reports.size})") })
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                when (tab) {
                    0 -> items(adminState.users, key = { it.userId }) { user ->
                        SparkCard {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(user.fullName.ifBlank { user.username }, style = MaterialTheme.typography.titleSmall)
                                    Text("${user.email} • ${user.country}", style = MaterialTheme.typography.bodySmall, color = SecondaryIce)
                                }
                                SparkBadge(
                                    label = user.accountStatus.uppercase(),
                                    color = if (user.accountStatus == "active") SuccessGreen else ErrorRed
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SparkOutlinedButton(
                                    text = "Adjust Credits",
                                    onClick = { creditTargetUser = user },
                                    modifier = Modifier.weight(1f)
                                )
                                SparkOutlinedButton(
                                    text = if (user.accountStatus == "active") "Suspend" else "Activate",
                                    accent = if (user.accountStatus == "active") ErrorRed else SuccessGreen,
                                    onClick = { onToggleSuspend(user) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    1 -> items(adminState.jobs, key = { it.id }) { job ->
                        SparkCard {
                            Text(job.movieTitle, style = MaterialTheme.typography.titleSmall)
                            Text("${job.status} • ${job.jobType} • ${job.providerId}", style = MaterialTheme.typography.bodySmall, color = SoftGold)
                            if (job.status == "failed" || job.status == "paused") {
                                Spacer(Modifier.height(6.dp))
                                SparkOutlinedButton(
                                    text = "Retry Job",
                                    icon = Icons.Default.Refresh,
                                    onClick = { onRetryJob(job) }
                                )
                            }
                        }
                    }
                    2 -> items(adminState.tickets, key = { it.id }) { t ->
                        SparkCard {
                            Text(t.subject, style = MaterialTheme.typography.titleSmall)
                            Text("${t.userEmail} • ${t.status}", style = MaterialTheme.typography.labelSmall, color = ChampagneGold)
                            Text(t.message, style = MaterialTheme.typography.bodySmall, color = SecondaryIce)
                            if (t.status == "open") {
                                Spacer(Modifier.height(8.dp))
                                SparkGoldButton(text = "Mark Resolved", onClick = { onResolveTicket(t, "resolved") })
                            }
                        }
                    }
                    else -> items(adminState.reports, key = { it.id }) { r ->
                        SparkCard {
                            Text("Reason: ${r.reason}", style = MaterialTheme.typography.titleSmall)
                            Text("Status: ${r.status} • ${r.details}", style = MaterialTheme.typography.bodySmall, color = SecondaryIce)
                            if (r.status == "open") {
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SparkOutlinedButton(
                                        text = "Remove Content",
                                        accent = ErrorRed,
                                        onClick = { onModerateReport(r, true) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    SparkOutlinedButton(
                                        text = "Dismiss",
                                        onClick = { onModerateReport(r, false) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    creditTargetUser?.let { target ->
        AlertDialog(
            onDismissRequest = { creditTargetUser = null },
            containerColor = SecondarySlate,
            title = { Text("Adjust Credits: ${target.fullName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = creditDeltaText,
                        onValueChange = { creditDeltaText = it },
                        label = { Text("Credits Delta (e.g. 50 or -10)") }
                    )
                    OutlinedTextField(
                        value = creditReason,
                        onValueChange = { creditReason = it },
                        label = { Text("Reason") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val delta = creditDeltaText.toIntOrNull() ?: 0
                    if (delta != 0) onAdjustCredits(target.userId, delta, creditReason)
                    creditTargetUser = null
                }) { Text("Apply", color = ChampagneGold) }
            },
            dismissButton = {
                TextButton(onClick = { creditTargetUser = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun AdminApiConfigScreen(
    adminState: AdminUiState,
    onSaveSetting: (AdminSetting) -> Unit,
    onTestAll: () -> Unit,
    onTestSingle: (String) -> Unit,
    onClearMessages: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val current = adminState.setting

    var providerName by remember(current) { mutableStateOf(current.providerName) }
    var providerUrl by remember(current) { mutableStateOf(current.providerBaseUrl) }
    var providerConfigured by remember(current) { mutableStateOf(current.providerConfigured) }
    var liveName by remember(current) { mutableStateOf(current.liveStreamingProviderName) }
    var liveUrl by remember(current) { mutableStateOf(current.liveStreamingBaseUrl) }
    var liveConfigured by remember(current) { mutableStateOf(current.liveStreamingConfigured) }
    var paymentName by remember(current) { mutableStateOf(current.paymentProviderName) }
    var paymentConfigured by remember(current) { mutableStateOf(current.paymentConfigured) }

    Scaffold(
        topBar = {
            SparkTopBar(
                title = "API Configuration & Health",
                subtitle = "Zero private keys stored in APK",
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
                SparkStatusBanner(adminState.statusMessage, adminState.errorMessage, onClearMessages)
            }

            item {
                SparkCard(borderColor = ChampagneGold) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = ChampagneGold)
                        Spacer(Modifier.width(8.dp))
                        Text("Zero-Secret Client Policy", style = MaterialTheme.typography.titleSmall, color = ChampagneGold)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Private API keys (VIDEO_PROVIDER_API_KEY, VIDEO_STITCHER_SECRET, STORAGE_BUCKET_SIGNING_KEY, LIVE_STREAM_API_SECRET) stay strictly on your backend proxy server — never inside the Android app or chat logs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryIce
                    )
                    Spacer(Modifier.height(10.dp))
                    SparkGoldButton(
                        text = if (adminState.isTestingConnections) "Testing Connections..." else "Test All 6 Connections Live",
                        icon = Icons.Default.NetworkCheck,
                        enabled = !adminState.isTestingConnections,
                        onClick = onTestAll,
                        modifier = Modifier.fillMaxWidth().testTag("admin_test_all_btn")
                    )
                }
            }

            items(adminState.integrations, key = { it.id }) { item ->
                val color = when (item.status) {
                    ApiHealthStatus.CONNECTION_SUCCESS, ApiHealthStatus.CONFIGURED -> SuccessGreen
                    ApiHealthStatus.NOT_CONFIGURED, ApiHealthStatus.USAGE_LIMIT -> AmberGold
                    ApiHealthStatus.CONNECTION_FAILED, ApiHealthStatus.AUTH_ERROR -> ErrorRed
                }
                SparkCard(borderColor = color.copy(alpha = 0.7f)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.name, style = MaterialTheme.typography.titleSmall)
                            Text(item.provider, style = MaterialTheme.typography.labelSmall, color = SoftGold)
                        }
                        SparkBadge(label = item.status.name, color = color)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(item.summary, style = MaterialTheme.typography.bodySmall, color = SecondaryIce)
                    if (!item.configured) {
                        Spacer(Modifier.height(4.dp))
                        Text("Setup: ${item.missingConfigInstructions}", style = MaterialTheme.typography.labelSmall, color = AmberGold)
                    }
                    Spacer(Modifier.height(8.dp))
                    SparkOutlinedButton(
                        text = "Test ${item.id}",
                        enabled = !adminState.isTestingConnections,
                        onClick = { onTestSingle(item.id) }
                    )
                }
            }

            item {
                SparkCard {
                    Text("Server Proxy Endpoints", style = MaterialTheme.typography.titleMedium, color = ChampagneGold)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = providerName, onValueChange = { providerName = it }, label = { Text("Video Provider Name") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(value = providerUrl, onValueChange = { providerUrl = it }, label = { Text("Video & Stitcher Proxy HTTPS URL") }, modifier = Modifier.fillMaxWidth())
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(8.dp)).clickable { providerConfigured = !providerConfigured }.padding(vertical = 2.dp)
                    ) {
                        Text("Video & Stitcher Active", style = MaterialTheme.typography.bodySmall)
                        Switch(checked = providerConfigured, onCheckedChange = { providerConfigured = it }, colors = SwitchDefaults.colors(checkedThumbColor = MidnightBlack, checkedTrackColor = ChampagneGold))
                    }

                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = liveName, onValueChange = { liveName = it }, label = { Text("Live Streaming Provider Name") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(value = liveUrl, onValueChange = { liveUrl = it }, label = { Text("Live Streaming Proxy HTTPS URL") }, modifier = Modifier.fillMaxWidth())
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(8.dp)).clickable { liveConfigured = !liveConfigured }.padding(vertical = 2.dp)
                    ) {
                        Text("Live Streaming Active", style = MaterialTheme.typography.bodySmall)
                        Switch(checked = liveConfigured, onCheckedChange = { liveConfigured = it }, colors = SwitchDefaults.colors(checkedThumbColor = MidnightBlack, checkedTrackColor = ChampagneGold))
                    }

                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = paymentName, onValueChange = { paymentName = it }, label = { Text("Payment Provider Name") }, modifier = Modifier.fillMaxWidth())
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(8.dp)).clickable { paymentConfigured = !paymentConfigured }.padding(vertical = 2.dp)
                    ) {
                        Text("Live Billing Configured", style = MaterialTheme.typography.bodySmall)
                        Switch(checked = paymentConfigured, onCheckedChange = { paymentConfigured = it }, colors = SwitchDefaults.colors(checkedThumbColor = MidnightBlack, checkedTrackColor = ChampagneGold))
                    }

                    Spacer(Modifier.height(10.dp))
                    SparkGoldButton(
                        text = "Save Proxy Configuration",
                        icon = Icons.Default.Save,
                        onClick = {
                            onSaveSetting(
                                current.copy(
                                    providerName = providerName,
                                    providerBaseUrl = providerUrl,
                                    providerConfigured = providerConfigured,
                                    liveStreamingProviderName = liveName,
                                    liveStreamingBaseUrl = liveUrl,
                                    liveStreamingConfigured = liveConfigured,
                                    paymentProviderName = paymentName,
                                    paymentConfigured = paymentConfigured
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth().testTag("admin_save_proxy_btn")
                    )
                }
            }
        }
    }
}
