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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import com.example.data.model.ActiveSessionItem
import com.example.data.model.SparkGuideArticle
import com.example.ui.components.SparkBadge
import com.example.ui.components.SparkCard
import com.example.ui.components.SparkGoldButton
import com.example.ui.components.SparkOutlinedButton
import com.example.ui.components.SparkStatusBanner
import com.example.ui.components.SparkTopBar
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

@Composable
fun AdminSecurityScreen(
    adminState: AdminUiState,
    onRunPostureAudit: () -> Unit,
    onRevokeSession: (ActiveSessionItem) -> Unit,
    onResolveAlert: (String) -> Unit,
    onClearMessages: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    if (!adminState.isFounderAdmin) {
        Scaffold(topBar = { SparkTopBar(title = "Access Denied", onBack = onBack) }) { inner ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SparkBrushes.CosmicBackground)
                    .padding(inner)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                SparkCard(borderColor = ErrorRed) {
                    Text("Founder Admin Required", style = MaterialTheme.typography.titleLarge, color = ErrorRed)
                    Text("Cybersecurity telemetry is restricted to verified Founder Christian Okafor.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        return
    }

    var tab by rememberSaveable { mutableIntStateOf(0) } // 0 Alerts, 1 Sessions, 2 Posture, 3 Audit Logs
    val unresolvedCount = adminState.securityAlerts.count { !it.resolvedFlag }
    val activeSessionsCount = adminState.allActiveSessions.count { it.status != "revoked" }
    val rateLimitCount = adminState.securityAlerts.count { it.alertType == "rate_limit" }
    val suspendedCount = adminState.users.count { it.accountStatus != "active" }

    Scaffold(
        topBar = {
            SparkTopBar(
                title = "Cybersecurity Center",
                subtitle = "Zero-trust posture, sessions & audit trail",
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SecurityMetricCard("Open Alerts", unresolvedCount.toString(), Modifier.weight(1f), if (unresolvedCount > 0) AmberGold else SuccessGreen)
                SecurityMetricCard("Sessions", activeSessionsCount.toString(), Modifier.weight(1f), ChampagneGold)
                SecurityMetricCard("Rate Limits", rateLimitCount.toString(), Modifier.weight(1f), SoftGold)
                SecurityMetricCard("Suspended", suspendedCount.toString(), Modifier.weight(1f), if (suspendedCount > 0) ErrorRed else SecondaryIce)
            }

            TabRow(
                selectedTabIndex = tab,
                containerColor = MidnightBlack,
                contentColor = ChampagneGold,
                indicator = { pos ->
                    TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(pos[tab]), color = ChampagneGold)
                }
            ) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Alerts (${adminState.securityAlerts.size})") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Sessions (${adminState.allActiveSessions.size})") })
                Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Posture") })
                Tab(selected = tab == 3, onClick = { tab = 3 }, text = { Text("Audit (${adminState.auditLogs.size})") })
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                when (tab) {
                    0 -> {
                        if (adminState.securityAlerts.isEmpty()) {
                            item {
                                SparkCard {
                                    Text("No security alerts recorded", style = MaterialTheme.typography.titleSmall)
                                    Text("Suspicious input blocks, burst rate limits, and user security reports appear here.", style = MaterialTheme.typography.bodySmall, color = SecondaryIce)
                                }
                            }
                        } else {
                            items(adminState.securityAlerts, key = { it.id }) { alert ->
                                val badgeColor = when {
                                    alert.resolvedFlag -> SuccessGreen
                                    alert.severity == "high" -> ErrorRed
                                    alert.severity == "medium" -> AmberGold
                                    else -> SoftGold
                                }
                                SparkCard(borderColor = badgeColor.copy(alpha = 0.5f)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text(alert.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                        SparkBadge(label = if (alert.resolvedFlag) "RESOLVED" else alert.severity.uppercase(), color = badgeColor)
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(alert.description, style = MaterialTheme.typography.bodySmall, color = SecondaryIce)
                                    Text("${alert.alertType} • ${alert.deviceLabel} • User ${alert.userId.take(8)}", style = MaterialTheme.typography.labelSmall, color = SoftGold)
                                    if (!alert.resolvedFlag) {
                                        Spacer(Modifier.height(8.dp))
                                        SparkOutlinedButton(
                                            text = "Mark Resolved",
                                            icon = Icons.Default.CheckCircle,
                                            accent = SuccessGreen,
                                            onClick = { onResolveAlert(alert.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        if (adminState.allActiveSessions.isEmpty()) {
                            item {
                                SparkCard {
                                    Text("No active device sessions tracked yet", style = MaterialTheme.typography.titleSmall)
                                }
                            }
                        } else {
                            items(adminState.allActiveSessions, key = { "${it.userId}_${it.id}" }) { session ->
                                SparkCard {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Icon(Icons.Default.Devices, contentDescription = null, tint = ChampagneGold)
                                            Spacer(Modifier.width(8.dp))
                                            Column {
                                                Text(session.deviceLabel.ifBlank { "Android Device" }, style = MaterialTheme.typography.titleSmall)
                                                Text("@${session.username.ifBlank { session.userEmail }} • ${session.platform}", style = MaterialTheme.typography.bodySmall, color = SecondaryIce)
                                            }
                                        }
                                        val isRevoked = session.status == "revoked"
                                        SparkBadge(
                                            label = if (isRevoked) "REVOKED" else "ACTIVE",
                                            color = if (isRevoked) ErrorRed else SuccessGreen
                                        )
                                    }
                                    if (session.status != "revoked") {
                                        Spacer(Modifier.height(8.dp))
                                        SparkOutlinedButton(
                                            text = "Revoke Session",
                                            accent = ErrorRed,
                                            onClick = { onRevokeSession(session) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        item {
                            SparkCard(borderColor = ChampagneGold) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Shield, contentDescription = null, tint = ChampagneGold)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Security Posture Check", style = MaterialTheme.typography.titleMedium, color = ChampagneGold)
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Honest runtime self-audit distinguishing verified client & Firestore controls from backend-only controls.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SecondaryIce
                                )
                                Spacer(Modifier.height(10.dp))
                                SparkGoldButton(
                                    text = "Run Self-Audit",
                                    icon = Icons.Default.Refresh,
                                    onClick = onRunPostureAudit,
                                    modifier = Modifier.fillMaxWidth().testTag("admin_run_posture_audit_btn")
                                )
                            }
                        }
                        items(adminState.postureChecks, key = { it.id }) { check ->
                            val isActive = check.status == "ACTIVE"
                            SparkCard(borderColor = if (isActive) SuccessGreen.copy(alpha = 0.55f) else AmberGold.copy(alpha = 0.55f)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(check.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                    SparkBadge(label = check.status, color = if (isActive) SuccessGreen else AmberGold)
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(check.detail, style = MaterialTheme.typography.bodySmall, color = SecondaryIce)
                            }
                        }
                    }

                    else -> {
                        if (adminState.auditLogs.isEmpty()) {
                            item {
                                SparkCard {
                                    Text("No audit log entries recorded yet", style = MaterialTheme.typography.titleSmall)
                                }
                            }
                        } else {
                            items(adminState.auditLogs, key = { it.id }) { log ->
                                SparkCard {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(log.action, style = MaterialTheme.typography.titleSmall, color = ChampagneGold)
                                        SparkBadge(label = log.targetCollection.uppercase())
                                    }
                                    Text(log.summary, style = MaterialTheme.typography.bodySmall, color = SecondaryIce)
                                    Text("Actor: ${log.actorUserId.take(10)} • Target: ${log.targetId.take(12)}", style = MaterialTheme.typography.labelSmall, color = SoftGold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SecurityMetricCard(label: String, value: String, modifier: Modifier, color: androidx.compose.ui.graphics.Color) {
    SparkCard(modifier = modifier, borderColor = color.copy(alpha = 0.5f)) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = SecondaryIce)
    }
}

@Composable
fun AdminGuideArticlesScreen(
    adminState: AdminUiState,
    onUpsertArticle: (SparkGuideArticle) -> Unit,
    onDeleteArticle: (String) -> Unit,
    onClearMessages: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    var editingArticle by remember { mutableStateOf<SparkGuideArticle?>(null) }

    if (!adminState.isFounderAdmin) {
        Scaffold(topBar = { SparkTopBar(title = "Access Denied", onBack = onBack) }) { inner ->
            Column(modifier = Modifier.fillMaxSize().background(SparkBrushes.CosmicBackground).padding(inner).padding(24.dp)) {
                SparkCard(borderColor = ErrorRed) {
                    Text("Founder Admin Required", style = MaterialTheme.typography.titleLarge, color = ErrorRed)
                }
            }
        }
        return
    }

    Scaffold(
        topBar = {
            SparkTopBar(
                title = "Spark Guide Knowledge Base",
                subtitle = "${adminState.guideArticles.size} official help articles",
                onBack = onBack
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(SparkBrushes.CosmicBackground)
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { SparkStatusBanner(adminState.statusMessage, adminState.errorMessage, onClearMessages) }

            item {
                SparkGoldButton(
                    text = "Add Help Article",
                    icon = Icons.Default.Add,
                    onClick = { editingArticle = SparkGuideArticle() },
                    modifier = Modifier.fillMaxWidth().testTag("admin_add_guide_article_btn")
                )
            }

            if (adminState.guideArticles.isEmpty()) {
                item {
                    SparkCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ChampagneGold)
                            Spacer(Modifier.width(8.dp))
                            Text("Built-in FAQ catalog active; add custom Founder articles here.", style = MaterialTheme.typography.bodySmall, color = SecondaryIce)
                        }
                    }
                }
            } else {
                items(adminState.guideArticles, key = { it.id }) { art ->
                    SparkCard(onClick = { editingArticle = art }) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(art.title, style = MaterialTheme.typography.titleMedium)
                                Text("${art.category} • ${if (art.published) "Published" else "Draft"}", style = MaterialTheme.typography.labelSmall, color = SoftGold)
                            }
                            IconButton(onClick = { onDeleteArticle(art.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete article", tint = ErrorRed)
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(art.stepsSummary, style = MaterialTheme.typography.bodySmall, color = SecondaryIce)
                    }
                }
            }
        }
    }

    editingArticle?.let { art ->
        var title by remember(art.id) { mutableStateOf(art.title) }
        var category by remember(art.id) { mutableStateOf(art.category) }
        var keywords by remember(art.id) { mutableStateOf(art.keywords) }
        var steps by remember(art.id) { mutableStateOf(art.stepsSummary) }
        var published by remember(art.id) { mutableStateOf(art.published) }

        AlertDialog(
            onDismissRequest = { editingArticle = null },
            containerColor = SecondarySlate,
            title = { Text(if (art.id.isBlank()) "New Spark Guide Article" else "Edit Help Article") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Question / Title") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = keywords, onValueChange = { keywords = it }, label = { Text("Keywords (comma separated)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = steps, onValueChange = { steps = it }, label = { Text("Numbered Steps") }, minLines = 4, modifier = Modifier.fillMaxWidth())
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(8.dp)).clickable { published = !published }.padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Published to Spark Guide", style = MaterialTheme.typography.bodySmall)
                        Switch(checked = published, onCheckedChange = { published = it }, colors = SwitchDefaults.colors(checkedThumbColor = MidnightBlack, checkedTrackColor = ChampagneGold))
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = title.isNotBlank() && steps.isNotBlank(),
                    onClick = {
                        onUpsertArticle(
                            art.copy(
                                title = title.trim(),
                                category = category.trim(),
                                keywords = keywords.trim(),
                                stepsSummary = steps.trim(),
                                published = published
                            )
                        )
                        editingArticle = null
                    }
                ) { Text("Save", color = ChampagneGold) }
            },
            dismissButton = {
                TextButton(onClick = { editingArticle = null }) { Text("Cancel") }
            }
        )
    }
}
