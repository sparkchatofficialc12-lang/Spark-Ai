package com.example

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.AuthManager
import com.example.core.MediaUtils
import com.example.core.configureAppCheck
import com.example.data.model.SocialPost
import com.example.ui.components.SparkGoldButton
import com.example.ui.components.SparkOutlinedButton
import com.example.ui.components.SparkTopBar
import com.example.ui.navigation.SparkRoute
import com.example.ui.navigation.SparkTab
import com.example.ui.screens.AdminApiConfigScreen
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AdminGuideArticlesScreen
import com.example.ui.screens.AdminSecurityScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CharacterStudioScreen
import com.example.ui.screens.ChatThreadScreen
import com.example.ui.screens.CommunityHubScreen
import com.example.ui.screens.CreateMovieScreen
import com.example.ui.screens.CreateStoryScreen
import com.example.ui.screens.CreatorAnalyticsScreen
import com.example.ui.screens.CreatorProfileScreen
import com.example.ui.screens.DiscoverScreen
import com.example.ui.screens.FloatingSparkGuideButton
import com.example.ui.screens.FollowersListScreen
import com.example.ui.screens.GenerationJobsScreen
import com.example.ui.screens.InboxScreen
import com.example.ui.screens.LiveStreamingScreen
import com.example.ui.screens.MovieEditorScreen
import com.example.ui.screens.MovieLibraryScreen
import com.example.ui.screens.MusicAndSoundsScreen
import com.example.ui.screens.MusicVideoStudioScreen
import com.example.ui.screens.MyCreationsScreen
import com.example.ui.screens.MyPostsScreen
import com.example.ui.screens.MyTemplatesScreen
import com.example.ui.screens.PostDetailScreen
import com.example.ui.screens.RecentlyDeletedScreen
import com.example.ui.screens.RemixWorkspaceScreen
import com.example.ui.screens.ScriptStoryboardScreen
import com.example.ui.screens.SettingsHubScreen
import com.example.ui.screens.SettingsSubScreen
import com.example.ui.screens.ShortVideoReelsScreen
import com.example.ui.screens.SingingPhotoScreen
import com.example.ui.screens.SoundsDiscoveryScreen
import com.example.ui.screens.SparkGuideScreen
import com.example.ui.screens.SparkSocialHomeScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.StoriesViewerScreen
import com.example.ui.screens.SubscriptionCreditsScreen
import com.example.ui.screens.TemplateDetailScreen
import com.example.ui.screens.UniversalPostScreen
import com.example.ui.screens.VideoPlayerScreen
import com.example.ui.screens.VoiceMusicStudioScreen
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.IceWhite
import com.example.ui.theme.MidnightBlack
import com.example.ui.theme.SecondaryIce
import com.example.ui.theme.SparkAiTheme
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.ProjectDetailViewModel
import com.example.ui.viewmodel.SocialViewModel
import com.example.ui.viewmodel.SparkGuideViewModel
import com.example.ui.viewmodel.StudioViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            SparkRootApp()
        }
    }
}

@Composable
private fun SparkRootApp() {
    val studioVm: StudioViewModel = viewModel()
    val isDarkMode by studioVm.isDarkMode.collectAsStateWithLifecycle(initialValue = true)
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    SparkAiTheme(darkTheme = isDarkMode) {
        val signedIn = currentUser
        if (signedIn == null) {
            SparkAuthGate(onAuthenticated = { currentUser = Firebase.auth.currentUser })
        } else {
            key(signedIn.uid) {
                SparkSignedInShell(signedIn, studioVm)
            }
        }
    }
}

@Composable
private fun SparkAuthGate(onAuthenticated: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var checkingSession by remember { mutableStateOf(true) }
    var isSigningIn by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        AuthManager.attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                checkingSession = false
                onAuthenticated()
            },
            onUnauthenticated = {
                checkingSession = false
            },
            scope = scope
        )
    }

    if (checkingSession) {
        SplashScreen(statusLabel = "Checking Spark AI session...")
    } else {
        AuthScreen(
            isSigningIn = isSigningIn,
            authError = authError,
            onGoogleSignInClick = {
                isSigningIn = true
                authError = null
                AuthManager.onGoogleSignInClicked(
                    context = context,
                    credentialManager = credentialManager,
                    onAuthSuccess = {
                        isSigningIn = false
                        onAuthenticated()
                    },
                    onAuthError = { msg ->
                        isSigningIn = false
                        authError = msg
                    },
                    scope = scope,
                    onAuthCancelled = {
                        isSigningIn = false
                    }
                )
            }
        )
    }
}

@Composable
private fun SparkSignedInShell(
    user: FirebaseUser,
    studioVm: StudioViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val detailVm: ProjectDetailViewModel = viewModel()
    val socialVm: SocialViewModel = viewModel()
    val adminVm: AdminViewModel = viewModel()
    val guideVm: SparkGuideViewModel = viewModel()

    val studioState by studioVm.uiState.collectAsStateWithLifecycle()
    val detailState by detailVm.state.collectAsStateWithLifecycle()
    val socialState by socialVm.state.collectAsStateWithLifecycle()
    val adminState by adminVm.state.collectAsStateWithLifecycle()
    val guideState by guideVm.state.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val appLockState by studioVm.appLockEnabled.collectAsStateWithLifecycle(initialValue = null)
    val appLockEnabled = appLockState == true
    var lockInitialized by rememberSaveable(user.uid) { mutableStateOf(false) }
    var isAppUnlocked by rememberSaveable(user.uid) { mutableStateOf(true) }
    var awaitingUnlockResult by remember { mutableStateOf(false) }
    var appLockNote by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(appLockState) {
        val enabled = appLockState ?: return@LaunchedEffect
        if (!lockInitialized) {
            isAppUnlocked = !enabled
            lockInitialized = true
        } else if (!enabled) {
            isAppUnlocked = true
        }
    }

    DisposableEffect(lifecycleOwner, appLockEnabled) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && appLockEnabled && !awaitingUnlockResult) {
                isAppUnlocked = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val keyguardLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        awaitingUnlockResult = false
        if (result.resultCode == Activity.RESULT_OK) {
            isAppUnlocked = true
            appLockNote = null
        } else {
            appLockNote = "Device verification was cancelled. Tap Unlock Spark AI to continue."
        }
    }

    LaunchedEffect(studioState.sessionRevoked) {
        if (studioState.sessionRevoked) {
            studioVm.acknowledgeSessionRevoked()
            AuthManager.signOut(credentialManager, scope)
        }
    }

    if (appLockState == null) {
        SplashScreen(statusLabel = "Verifying Spark AI security...")
        return
    }

    val isFounderAdmin = AuthManager.isFounderAdmin(user)
    val myUsername = studioState.account?.username?.ifBlank { user.displayName ?: "creator" }
        ?: user.displayName ?: "creator"
    val myDisplayName = studioState.account?.fullName?.ifBlank { myUsername } ?: myUsername
    val myAvatarUrl = studioState.account?.photoUrl.orEmpty()
    val isProfilePublic = (studioState.profile?.defaultPrivacy ?: "private") == "public"

    val backStack = remember { mutableStateListOf<SparkRoute>(SparkRoute.Main(SparkTab.HOME)) }
    val currentRoute = backStack.lastOrNull() ?: SparkRoute.Main(SparkTab.HOME)

    LaunchedEffect(currentRoute) {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }

    fun navigateTo(route: SparkRoute) {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        if (route is SparkRoute.Main) {
            backStack.clear()
            backStack.add(route)
        } else if (backStack.lastOrNull() != route) {
            backStack.add(route)
        }
    }

    fun popBack() {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        } else {
            backStack[0] = SparkRoute.Main(SparkTab.HOME)
        }
    }

    BackHandler(enabled = currentRoute is SparkRoute.Main && currentRoute.tab != SparkTab.HOME && isAppUnlocked) {
        navigateTo(SparkRoute.Main(SparkTab.HOME))
    }

    fun handleUseTemplateOnPost(post: SocialPost) {
        val linkedTpl = (socialState.publicTemplates + socialState.myTemplates)
            .firstOrNull { it.id == post.linkedTemplateId }
        if (linkedTpl != null) {
            socialVm.startTemplateRemix(linkedTpl) { navigateTo(SparkRoute.RemixWorkspace) }
        } else {
            navigateTo(
                SparkRoute.CreateMovie(
                    initialMode = "text_to_movie",
                    seedTitle = "${post.title} Remix",
                    seedPrompt = post.caption.ifBlank { post.title },
                    seedDurationSec = post.durationSec.coerceIn(10, 600)
                )
            )
        }
    }

    val unreadNotifications = studioState.notifications.count { !it.readFlag }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideMainLayout = maxWidth >= 600.dp

        when (currentRoute) {
            is SparkRoute.Main -> {
                BoxWithConstraints(modifier = Modifier.fillMaxSize().background(MidnightBlack)) {
                    val useRail = maxWidth >= 600.dp

                    val navItems = listOf(
                        Triple(SparkTab.HOME, "Home", Icons.Default.Home),
                        Triple(SparkTab.DISCOVER, "Discover", Icons.Default.Explore),
                        Triple(SparkTab.CREATE, "Create", Icons.Default.AddCircle),
                        Triple(SparkTab.INBOX, "Inbox", Icons.Default.Inbox),
                        Triple(SparkTab.PROFILE, "Profile", Icons.Default.Person)
                    )

                    val mainContent: @Composable (Modifier) -> Unit = { contentMod ->
                        BoxWithConstraints(modifier = contentMod) {
                            when (currentRoute.tab) {
                                SparkTab.HOME -> SparkSocialHomeScreen(
                                    studioState = studioState,
                                    socialState = socialState,
                                    currentUid = user.uid,
                                    isFounderAdmin = isFounderAdmin,
                                    onToggleLike = { socialVm.togglePostLike(it, myUsername, myAvatarUrl, isProfilePublic) },
                                    onToggleSave = { socialVm.toggleSavePost(it) },
                                    onRepost = { socialVm.repostPost(it, myUsername) },
                                    onRecordShare = { p, ch -> socialVm.recordPostShare(p, ch, myUsername) },
                                    onToggleFollow = { cid -> socialVm.toggleFollowCreator(cid, myUsername) },
                                    onUseTemplatePost = { p -> handleUseTemplateOnPost(p) },
                                    onEditPost = { pid, t, c, tags, vis, allowC, allowR, allowD ->
                                        socialVm.editPost(pid, t, c, tags, vis, allowC, allowR, allowD)
                                    },
                                    onReplaceVideo = { p, url, thumb, dur, res, keep, t, c ->
                                        socialVm.replacePostVideo(p, url, thumb, dur, res, keep, t, c)
                                    },
                                    onSoftDeletePost = { p -> socialVm.softDeletePost(p) },
                                    onChangePrivacy = { pid, vis -> socialVm.changePostPrivacy(pid, vis) },
                                    onToggleComments = { pid, allow -> socialVm.togglePostComments(pid, allow) },
                                    onUpdateTemplatePermission = { pid, allow -> socialVm.updatePostTemplatePermission(pid, allow) },
                                    onClearMessages = { socialVm.clearMessages() },
                                    onNavigate = { navigateTo(it) }
                                )
                                SparkTab.DISCOVER -> DiscoverScreen(
                                    currentUid = user.uid,
                                    myUsername = myUsername,
                                    socialState = socialState,
                                    onToggleFollow = { cid -> socialVm.toggleFollowCreator(cid, myUsername) },
                                    onStartChat = { peer ->
                                        socialVm.openOrStartConversation(peer.creatorId, myUsername, peer.username) { convId ->
                                            navigateTo(SparkRoute.ChatThread(convId, peer.creatorId, peer.username))
                                        }
                                    },
                                    onNavigate = { navigateTo(it) }
                                )
                                SparkTab.CREATE -> UniversalPostScreen(
                                    prefillTitle = "",
                                    prefillType = "short_video",
                                    prefillProjectId = "",
                                    prefillVideoUrl = "",
                                    prefillThumbnail = "",
                                    prefillDurationSec = 30,
                                    prefillSoundId = "",
                                    prefillSoundTitle = "",
                                    studioState = studioState,
                                    socialState = socialState,
                                    onPublish = { cType, mType, title, cap, tags, mentions, cat, vUrl, bmp, album, existingThumb, sId, sTitle, dur, ratio, res, vis, allowC, allowR, allowD, projId, baseP, alsoTpl ->
                                        socialVm.publishCreationAsPost(
                                            creatorUsername = myUsername,
                                            creatorDisplayName = myDisplayName,
                                            creatorAvatarUrl = myAvatarUrl,
                                            creationType = cType,
                                            mediaType = mType,
                                            title = title,
                                            caption = cap,
                                            hashtags = tags,
                                            mentions = mentions,
                                            category = cat,
                                            previewVideoUrl = vUrl,
                                            thumbnailBitmap = bmp,
                                            albumBitmaps = album,
                                            existingThumbnailData = existingThumb,
                                            soundId = sId,
                                            soundTitle = sTitle,
                                            durationSec = dur,
                                            aspectRatio = ratio,
                                            resolution = res,
                                            visibility = vis,
                                            allowComments = allowC,
                                            allowReuse = allowR,
                                            allowDownload = allowD,
                                            linkedProjectId = projId,
                                            reusableBasePrompt = baseP,
                                            alsoPublishReusableTemplate = alsoTpl
                                        ) { navigateTo(SparkRoute.Main(SparkTab.HOME)) }
                                    },
                                    onClearMessages = { socialVm.clearMessages() },
                                    onBack = null,
                                    onNavigate = { navigateTo(it) }
                                )
                                SparkTab.INBOX -> InboxScreen(
                                    currentUid = user.uid,
                                    myUsername = myUsername,
                                    studioState = studioState,
                                    socialState = socialState,
                                    onStartChat = { peer ->
                                        socialVm.openOrStartConversation(peer.creatorId, myUsername, peer.username) { convId ->
                                            navigateTo(SparkRoute.ChatThread(convId, peer.creatorId, peer.username))
                                        }
                                    },
                                    onMarkNotificationRead = { studioVm.markNotificationRead(it) },
                                    onMarkAllNotificationsRead = { studioVm.markAllNotificationsRead() },
                                    onNavigate = { navigateTo(it) }
                                )
                                SparkTab.PROFILE -> CreatorProfileScreen(
                                    targetCreatorId = user.uid,
                                    currentUid = user.uid,
                                    studioState = studioState,
                                    socialState = socialState,
                                    onOpenCreatorProfile = { socialVm.openCreatorProfile(it) },
                                    onToggleFollow = { socialVm.toggleFollowCreator(it, myUsername) },
                                    onStartChat = { cid, uname ->
                                        socialVm.openOrStartConversation(cid, myUsername, uname) { convId ->
                                            navigateTo(SparkRoute.ChatThread(convId, cid, uname))
                                        }
                                    },
                                    onUpdateBioAndCover = { newBio, bmp ->
                                        socialVm.updateCreatorCoverAndBio(myUsername, myDisplayName, myAvatarUrl, newBio, bmp)
                                        studioState.profile?.let { studioVm.updateProfileSettings(it.copy(bio = newBio)) }
                                    },
                                    onBack = null,
                                    onNavigate = { navigateTo(it) }
                                )
                            }
                        }
                    }

                    Scaffold(
                        topBar = {
                            SparkTopBar(
                                title = when (currentRoute.tab) {
                                    SparkTab.HOME -> "Spark AI"
                                    SparkTab.DISCOVER -> "Discover"
                                    SparkTab.CREATE -> "Create"
                                    SparkTab.INBOX -> "Inbox"
                                    SparkTab.PROFILE -> "@$myUsername"
                                },
                                subtitle = "Imagine It. Create It. Watch It.",
                                credits = studioState.credits?.balance,
                                unreadNotifications = unreadNotifications,
                                onCreditsClick = { navigateTo(SparkRoute.SubscriptionCredits) },
                                onNotificationsClick = { navigateTo(SparkRoute.Main(SparkTab.INBOX)) }
                            )
                        },
                        bottomBar = {
                            if (!useRail) {
                                NavigationBar(containerColor = MidnightBlack) {
                                    navItems.forEach { (tab, label, icon) ->
                                        val selected = currentRoute.tab == tab
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = { navigateTo(SparkRoute.Main(tab)) },
                                            icon = {
                                                if (tab == SparkTab.INBOX && unreadNotifications > 0) {
                                                    BadgedBox(badge = { Badge(containerColor = ChampagneGold) { Text("$unreadNotifications") } }) {
                                                        Icon(icon, contentDescription = label)
                                                    }
                                                } else {
                                                    Icon(icon, contentDescription = label)
                                                }
                                            },
                                            label = { Text(label) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = ChampagneGold,
                                                selectedTextColor = ChampagneGold,
                                                unselectedIconColor = SecondaryIce,
                                                unselectedTextColor = SecondaryIce
                                            ),
                                            modifier = Modifier.testTag("nav_${tab.name.lowercase()}")
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        if (useRail) {
                            Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                                NavigationRail(containerColor = MidnightBlack) {
                                    navItems.forEach { (tab, label, icon) ->
                                        NavigationRailItem(
                                            selected = currentRoute.tab == tab,
                                            onClick = { navigateTo(SparkRoute.Main(tab)) },
                                            icon = { Icon(icon, contentDescription = label) },
                                            label = { Text(label) },
                                            colors = NavigationRailItemDefaults.colors(
                                                selectedIconColor = ChampagneGold,
                                                selectedTextColor = ChampagneGold,
                                                unselectedIconColor = SecondaryIce,
                                                unselectedTextColor = SecondaryIce
                                            )
                                        )
                                    }
                                    if (isFounderAdmin) {
                                        IconButton(onClick = { navigateTo(SparkRoute.AdminDashboard) }) {
                                            Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin", tint = ChampagneGold)
                                        }
                                    }
                                }
                                mainContent(Modifier.weight(1f).fillMaxSize())
                            }
                        } else {
                            mainContent(Modifier.fillMaxSize().padding(innerPadding))
                        }
                    }
                }
            }

            is SparkRoute.CreateMovie -> CreateMovieScreen(
                route = currentRoute,
                uiState = studioState,
                studioViewModel = studioVm,
                onBack = { popBack() },
                onProjectCreated = { projectId -> navigateTo(SparkRoute.MovieEditor(projectId)) }
            )

            SparkRoute.SingingPhoto -> SingingPhotoScreen(
                uiState = studioState,
                studioViewModel = studioVm,
                onBack = { popBack() },
                onNavigateJobs = { navigateTo(SparkRoute.GenerationJobs) }
            )

            is SparkRoute.MusicLibrary -> MusicAndSoundsScreen(
                title = if (currentRoute.pickerMode) "Select Soundtrack" else "Music & Sounds",
                uiState = studioState,
                socialState = socialState,
                studioViewModel = studioVm,
                socialViewModel = socialVm,
                onBack = { popBack() },
                onNavigate = { navigateTo(it) }
            )

            is SparkRoute.MusicVideoStudio -> MusicVideoStudioScreen(
                preselectedTrackId = currentRoute.preselectedTrackId,
                uiState = studioState,
                studioViewModel = studioVm,
                onBack = { popBack() },
                onNavigateJobs = { navigateTo(SparkRoute.GenerationJobs) }
            )

            SparkRoute.CharacterStudio -> CharacterStudioScreen(
                uiState = studioState,
                studioViewModel = studioVm,
                onBack = { popBack() }
            )

            is SparkRoute.ScriptStoryboard -> ScriptStoryboardScreen(
                initialProjectId = currentRoute.projectId,
                uiState = studioState,
                studioViewModel = studioVm,
                projectDetailViewModel = detailVm,
                onBack = { popBack() }
            )

            SparkRoute.VoiceMusicStudio -> VoiceMusicStudioScreen(
                studioViewModel = studioVm,
                onBack = { popBack() }
            )

            is SparkRoute.MovieEditor -> MovieEditorScreen(
                initialProjectId = currentRoute.projectId,
                studioState = studioState,
                detailState = detailState,
                onSelectProject = { detailVm.observeProject(it) },
                onSaveClip = { pid, clip -> detailVm.saveClip(pid, clip) },
                onSplitClip = { pid, clip -> detailVm.splitClip(pid, clip) },
                onMoveClip = { pid, clip, up -> detailVm.moveClipOrder(pid, clip, up) },
                onDeleteClip = { pid, cid -> detailVm.deleteClip(pid, cid) },
                onStitchExport = { proj ->
                    val mv = studioState.movies.firstOrNull { it.projectId == proj.id }
                    detailVm.exportStitchedMovie(proj, studioState.adminSetting, mv)
                },
                onClearMessages = { detailVm.clearMessages() },
                onBack = { popBack() },
                onNavigate = { navigateTo(it) }
            )

            SparkRoute.GenerationJobs -> GenerationJobsScreen(
                studioState = studioState,
                isFounderAdmin = isFounderAdmin,
                onUpdateJobStatus = { job, status -> detailVm.updateJobStatus(job, status) },
                onBack = { popBack() },
                onNavigate = { navigateTo(it) }
            )

            is SparkRoute.UniversalPost -> UniversalPostScreen(
                prefillTitle = currentRoute.prefillTitle,
                prefillType = currentRoute.prefillType,
                prefillProjectId = currentRoute.prefillProjectId,
                prefillVideoUrl = currentRoute.prefillVideoUrl,
                prefillThumbnail = currentRoute.prefillThumbnail,
                prefillDurationSec = currentRoute.prefillDurationSec,
                prefillSoundId = currentRoute.prefillSoundId,
                prefillSoundTitle = currentRoute.prefillSoundTitle,
                studioState = studioState,
                socialState = socialState,
                onPublish = { cType, mType, title, cap, tags, mentions, cat, vUrl, bmp, album, existingThumb, sId, sTitle, dur, ratio, res, vis, allowC, allowR, allowD, projId, baseP, alsoTpl ->
                    socialVm.publishCreationAsPost(
                        creatorUsername = myUsername,
                        creatorDisplayName = myDisplayName,
                        creatorAvatarUrl = myAvatarUrl,
                        creationType = cType,
                        mediaType = mType,
                        title = title,
                        caption = cap,
                        hashtags = tags,
                        mentions = mentions,
                        category = cat,
                        previewVideoUrl = vUrl,
                        thumbnailBitmap = bmp,
                        albumBitmaps = album,
                        existingThumbnailData = existingThumb,
                        soundId = sId,
                        soundTitle = sTitle,
                        durationSec = dur,
                        aspectRatio = ratio,
                        resolution = res,
                        visibility = vis,
                        allowComments = allowC,
                        allowReuse = allowR,
                        allowDownload = allowD,
                        linkedProjectId = projId,
                        reusableBasePrompt = baseP,
                        alsoPublishReusableTemplate = alsoTpl
                    ) { popBack() }
                },
                onClearMessages = { socialVm.clearMessages() },
                onBack = { popBack() },
                onNavigate = { navigateTo(it) }
            )

            is SparkRoute.PostDetail -> PostDetailScreen(
                postId = currentRoute.postId,
                currentUid = user.uid,
                myUsername = myUsername,
                socialState = socialState,
                onOpenPost = { socialVm.openPostById(it, myUsername, myAvatarUrl, isProfilePublic) },
                onToggleLike = { socialVm.togglePostLike(it, myUsername, myAvatarUrl, isProfilePublic) },
                onToggleSave = { socialVm.toggleSavePost(it) },
                onRepost = { socialVm.repostPost(it, myUsername) },
                onAddComment = { p, parent, txt -> socialVm.addPostComment(p, parent, myUsername, txt) },
                onToggleCommentLike = { socialVm.toggleCommentLike(it) },
                onDeleteComment = { p, cid -> socialVm.deletePostComment(p, cid) },
                onRecordShare = { p, ch -> socialVm.recordPostShare(p, ch, myUsername) },
                onToggleFollow = { cid -> socialVm.toggleFollowCreator(cid, myUsername) },
                onUseTemplate = { handleUseTemplateOnPost(it) },
                onReport = { p, reason -> socialVm.reportContent(p.id, p.creatorId, reason, p.title) },
                onDeletePost = { socialVm.deleteMyPost(it) },
                myMovies = studioState.movies,
                onEditPost = { pid, t, c, tags, vis, allowC, allowR, allowD ->
                    socialVm.editPost(pid, t, c, tags, vis, allowC, allowR, allowD)
                },
                onReplaceVideo = { p, url, thumb, dur, res, keep, t, c ->
                    socialVm.replacePostVideo(p, url, thumb, dur, res, keep, t, c)
                },
                onSoftDeletePost = { p -> socialVm.softDeletePost(p) },
                onChangePrivacy = { pid, vis -> socialVm.changePostPrivacy(pid, vis) },
                onToggleComments = { pid, allow -> socialVm.togglePostComments(pid, allow) },
                onUpdateTemplatePermission = { pid, allow -> socialVm.updatePostTemplatePermission(pid, allow) },
                onClearMessages = { socialVm.clearMessages() },
                onBack = { popBack() },
                onNavigate = { navigateTo(it) }
            )

            is SparkRoute.ShortVideoReels -> ShortVideoReelsScreen(
                startPostId = currentRoute.startPostId,
                soundFilterId = currentRoute.soundFilterId,
                currentUid = user.uid,
                socialState = socialState,
                onToggleLike = { socialVm.togglePostLike(it, myUsername, myAvatarUrl, isProfilePublic) },
                onToggleSave = { socialVm.toggleSavePost(it) },
                onToggleFollow = { cid -> socialVm.toggleFollowCreator(cid, myUsername) },
                onRecordShare = { p, ch -> socialVm.recordPostShare(p, ch, myUsername) },
                onUseTemplate = { handleUseTemplateOnPost(it) },
                onBack = { popBack() },
                onNavigate = { navigateTo(it) }
            )

            is SparkRoute.StoriesViewer -> StoriesViewerScreen(
                startCreatorId = currentRoute.startCreatorId,
                currentUid = user.uid,
                socialState = socialState,
                onOpenStory = { socialVm.openStory(it, myUsername, myAvatarUrl, isProfilePublic) },
                onDeleteStory = { socialVm.deleteStory(it) },
                onBack = { popBack() }
            )

            SparkRoute.CreateStory -> CreateStoryScreen(
                username = myUsername,
                avatarUrl = myAvatarUrl,
                isBusy = socialState.isBusy,
                onCreateStory = { u, av, mType, mUrl, bmp, overlay, sticker, cap, trk, vis ->
                    socialVm.createStory(u, av, mType, mUrl, bmp, overlay, sticker, cap, trk, vis) {
                        popBack()
                    }
                },
                onBack = { popBack() }
            )

            is SparkRoute.ChatThread -> ChatThreadScreen(
                conversationId = currentRoute.conversationId,
                peerUserId = currentRoute.peerUserId,
                peerUsername = currentRoute.peerUsername,
                currentUid = user.uid,
                myUsername = myUsername,
                socialState = socialState,
                voiceAudio = studioVm.voiceAudio,
                onObserveMessages = { socialVm.observeConversationMessages(it) },
                onSendMessage = { cid, recId, sName, mType, txt, mUrl, bmp, aDur, rId, rPrev ->
                    socialVm.sendMessage(cid, recId, sName, mType, txt, mUrl, bmp, aDur, rId, rPrev)
                },
                onDeleteMessage = { cid, mid -> socialVm.deleteMessage(cid, mid) },
                onBlockCreator = { socialVm.blockCreator(it) },
                onReportCreator = { cid -> socialVm.reportContent(cid, cid, "Spam or abuse", "Reported from chat") },
                onBack = { popBack() },
                onNavigate = { navigateTo(it) }
            )

            SparkRoute.SoundsDiscovery -> SoundsDiscoveryScreen(
                allTracks = studioVm.allMusicTracks(),
                savedSoundIds = socialState.savedSoundIds,
                socialState = socialState,
                voiceAudio = studioVm.voiceAudio,
                onToggleSaveSound = { trk -> socialVm.toggleSaveSound(trk.id, trk.title) },
                onBack = { popBack() },
                onNavigate = { navigateTo(it) }
            )

            SparkRoute.LiveStreaming -> LiveStreamingScreen(
                studioState = studioState,
                socialState = socialState,
                isFounderAdmin = isFounderAdmin,
                onBack = { popBack() },
                onNavigate = { navigateTo(it) }
            )

            SparkRoute.SavedContent -> MyCreationsScreen(
                studioState = studioState,
                socialState = socialState,
                onAttachLocalVideo = { m, u -> studioVm.attachLocalVideoToMovie(m, u) },
                onToggleFavorite = { studioVm.toggleMovieFavorite(it) },
                onDownloadMovie = { studioVm.downloadMovieToDevice(it) },
                onDeleteMovie = { studioVm.deleteMovie(it) },
                onBatchDeleteMovies = { studioVm.batchSoftDeleteMovies(it) },
                onDeleteProject = { studioVm.deleteProject(it) },
                onClearBanners = { studioVm.clearBanners() },
                onBack = { popBack() },
                onNavigate = { navigateTo(it) }
            )

            SparkRoute.CommunityHub -> CommunityHubScreen(
                socialState = socialState,
                onToggleLike = { socialVm.toggleTemplateLike(it) },
                onToggleSave = { socialVm.toggleSavedTemplate(it) },
                onUseTemplate = { tpl ->
                    socialVm.startTemplateRemix(tpl) { navigateTo(SparkRoute.RemixWorkspace) }
                },
                onBack = { popBack() },
                onNavigate = { navigateTo(it) }
            )

            is SparkRoute.TemplateDetail -> TemplateDetailScreen(
                templateId = currentRoute.templateId,
                socialState = socialState,
                onOpenTemplate = { socialVm.openTemplateDetail(it) },
                onToggleLike = { socialVm.toggleTemplateLike(it) },
                onToggleSave = { socialVm.toggleSavedTemplate(it) },
                onAddComment = { tid, txt -> socialVm.addTemplateComment(tid, myUsername, txt) },
                onUseTemplate = { tpl ->
                    socialVm.startTemplateRemix(tpl) { navigateTo(SparkRoute.RemixWorkspace) }
                },
                onBack = { popBack() },
                onNavigate = { navigateTo(it) }
            )

            SparkRoute.RemixWorkspace -> RemixWorkspaceScreen(
                seed = socialState.remixSeed,
                studioState = studioState,
                onCreateRemixProject = { seed, title, prompt, ratio, dur, bmp ->
                    studioVm.generateAndCreateMovieProject(
                        title = title,
                        description = prompt,
                        genre = "Action",
                        countryLocation = studioState.account?.country ?: "Nigeria",
                        language = "English",
                        mainCharacterSummary = seed.prompt.characterTemplate,
                        characterAppearance = "",
                        characterClothing = "",
                        characterAgeRange = "25-35",
                        cameraAngle = seed.settings.cameraMovement,
                        lighting = seed.settings.lighting,
                        atmosphere = "Cinematic Remix",
                        backgroundSetting = seed.template.category,
                        visualStyle = seed.settings.videoStyle,
                        voiceStyle = seed.settings.voiceStyle,
                        musicStyle = seed.settings.musicStyle,
                        aspectRatio = ratio,
                        resolution = seed.settings.resolution,
                        targetDurationSec = dur,
                        publicProject = false,
                        jobType = "template_remix",
                        referenceBitmap = bmp
                    ) { newProjId ->
                        socialVm.recordRemixAttribution(seed, myUsername, newProjId, title, prompt)
                        navigateTo(SparkRoute.MovieEditor(newProjId))
                    }
                },
                onBack = { popBack() }
            )

            is SparkRoute.CreatorProfileRoute -> CreatorProfileScreen(
                targetCreatorId = currentRoute.creatorId,
                currentUid = user.uid,
                studioState = studioState,
                socialState = socialState,
                onOpenCreatorProfile = { socialVm.openCreatorProfile(it) },
                onToggleFollow = { socialVm.toggleFollowCreator(it, myUsername) },
                onStartChat = { cid, uname ->
                    socialVm.openOrStartConversation(cid, myUsername, uname) { convId ->
                        navigateTo(SparkRoute.ChatThread(convId, cid, uname))
                    }
                },
                onUpdateBioAndCover = { newBio, bmp ->
                    socialVm.updateCreatorCoverAndBio(myUsername, myDisplayName, myAvatarUrl, newBio, bmp)
                    studioState.profile?.let { studioVm.updateProfileSettings(it.copy(bio = newBio)) }
                },
                onBack = { popBack() },
                onNavigate = { navigateTo(it) }
            )

            is SparkRoute.FollowersList -> FollowersListScreen(
                creatorId = currentRoute.creatorId,
                showFollowing = currentRoute.showFollowing,
                socialState = socialState,
                onBack = { popBack() },
                onNavigate = { navigateTo(it) }
            )

            SparkRoute.CreatorAnalytics -> CreatorAnalyticsScreen(
                socialState = socialState,
                onBack = { popBack() },
                onNavigate = { navigateTo(it) }
            )

            SparkRoute.MyPosts -> MyPostsScreen(
                socialState = socialState,
                onDeletePost = { socialVm.deleteMyPost(it) },
                myMovies = studioState.movies,
                onToggleSave = { socialVm.toggleSavePost(it) },
                onShare = { p ->
                    MediaUtils.shareMovieTextOrLink(context, p.title, p.previewVideoUrl, p.caption)
                    socialVm.recordPostShare(p, "system_share", myUsername)
                },
                onEditPost = { pid, t, c, tags, vis, allowC, allowR, allowD ->
                    socialVm.editPost(pid, t, c, tags, vis, allowC, allowR, allowD)
                },
                onReplaceVideo = { p, url, thumb, dur, res, keep, t, c ->
                    socialVm.replacePostVideo(p, url, thumb, dur, res, keep, t, c)
                },
                onSoftDeletePost = { p -> socialVm.softDeletePost(p) },
                onChangePrivacy = { pid, vis -> socialVm.changePostPrivacy(pid, vis) },
                onToggleComments = { pid, allow -> socialVm.togglePostComments(pid, allow) },
                onUpdateTemplatePermission = { pid, allow -> socialVm.updatePostTemplatePermission(pid, allow) },
                onClearMessages = { socialVm.clearMessages() },
                onBack = { popBack() },
                onNavigate = { navigateTo(it) }
            )

            SparkRoute.MyTemplates -> MyTemplatesScreen(
                socialState = socialState,
                onUpdateTemplateMeta = { id, desc, vis, allowR -> socialVm.updateMyTemplateMeta(id, desc, vis, allowR) },
                onDeleteTemplate = { socialVm.deleteTemplate(it) },
                onBack = { popBack() },
                onNavigate = { navigateTo(it) }
            )

            SparkRoute.MyCreations -> MyCreationsScreen(
                studioState = studioState,
                socialState = socialState,
                onAttachLocalVideo = { m, u -> studioVm.attachLocalVideoToMovie(m, u) },
                onToggleFavorite = { studioVm.toggleMovieFavorite(it) },
                onDownloadMovie = { studioVm.downloadMovieToDevice(it) },
                onDeleteMovie = { studioVm.deleteMovie(it) },
                onBatchDeleteMovies = { studioVm.batchSoftDeleteMovies(it) },
                onDeleteProject = { studioVm.deleteProject(it) },
                onClearBanners = { studioVm.clearBanners() },
                onBack = { popBack() },
                onNavigate = { navigateTo(it) }
            )

            SparkRoute.MovieLibrary -> MovieLibraryScreen(
                studioState = studioState,
                socialState = socialState,
                onMarkMovieViewed = { studioVm.markMovieViewed(it) },
                onToggleFavorite = { studioVm.toggleMovieFavorite(it) },
                onDownloadMovie = { studioVm.downloadMovieToDevice(it) },
                onClearBanners = { studioVm.clearBanners() },
                onBack = { popBack() },
                onNavigate = { navigateTo(it) }
            )

            is SparkRoute.VideoPlayer -> VideoPlayerScreen(
                title = currentRoute.title,
                videoUrl = currentRoute.videoUrl,
                movieId = currentRoute.movieId,
                subtitlesText = currentRoute.subtitlesText,
                resolution = currentRoute.resolution,
                postId = currentRoute.postId,
                studioState = studioState,
                socialState = socialState,
                onMarkMovieViewed = { studioVm.markMovieViewed(it) },
                onDownloadMovie = { studioVm.downloadMovieToDevice(it) },
                onTogglePostLike = { socialVm.togglePostLike(it, myUsername, myAvatarUrl, isProfilePublic) },
                onToggleSavePost = { socialVm.toggleSavePost(it) },
                onRecordShare = { p, ch -> socialVm.recordPostShare(p, ch, myUsername) },
                onOpenPostDetail = { navigateTo(SparkRoute.PostDetail(it)) },
                onBack = { popBack() }
            )

            SparkRoute.Notifications -> InboxScreen(
                currentUid = user.uid,
                myUsername = myUsername,
                studioState = studioState,
                socialState = socialState,
                onStartChat = { peer ->
                    socialVm.openOrStartConversation(peer.creatorId, myUsername, peer.username) { convId ->
                        navigateTo(SparkRoute.ChatThread(convId, peer.creatorId, peer.username))
                    }
                },
                onMarkNotificationRead = { studioVm.markNotificationRead(it) },
                onMarkAllNotificationsRead = { studioVm.markAllNotificationsRead() },
                onNavigate = { navigateTo(it) }
            )

            SparkRoute.SubscriptionCredits -> SubscriptionCreditsScreen(
                studioState = studioState,
                onCancelSubscription = { studioVm.cancelSubscription() },
                onClearBanners = { studioVm.clearBanners() },
                onBack = { popBack() }
            )

            SparkRoute.SettingsHub -> SettingsHubScreen(
                studioState = studioState,
                onSignOut = { AuthManager.signOut(credentialManager, scope) },
                onBack = { popBack() },
                onNavigate = { navigateTo(it) }
            )

            is SparkRoute.SettingsSub -> SettingsSubScreen(
                section = currentRoute.section,
                studioState = studioState,
                socialState = socialState,
                appLockEnabled = appLockEnabled,
                onToggleAppLock = { studioVm.toggleAppLock(it) },
                onRevokeSession = { studioVm.revokeSession(it) },
                onLogoutAllOtherDevices = { studioVm.logoutAllOtherDevices() },
                onReportSecurityIssue = { title, desc -> studioVm.reportSecurityIssue(title, desc) },
                onUpdateAccount = { full, uname, ctry, ph -> studioVm.updateAccountInfo(full, uname, ctry, ph) },
                onUpdateProfile = { studioVm.updateProfileSettings(it) },
                onSubmitTicket = { subj, msg -> studioVm.submitSupportTicket(subj, msg) },
                onDeleteAccount = {
                    studioVm.deleteAccount {
                        AuthManager.signOut(credentialManager, scope)
                    }
                },
                onClearBanners = { studioVm.clearBanners() },
                onNavigate = { navigateTo(it) },
                onBack = { popBack() }
            )

            is SparkRoute.SparkGuide -> SparkGuideScreen(
                contextSection = currentRoute.contextSection,
                initialQuestion = currentRoute.initialQuestion,
                guideState = guideState,
                onAskQuestion = { guideVm.askSparkGuide(it) },
                onSetContextSection = { guideVm.setContextSection(it) },
                onSetLanguage = { guideVm.setLanguage(it) },
                onStartNewChat = { guideVm.startNewChat() },
                onOpenConversation = { guideVm.openConversation(it) },
                onDeleteConversation = { guideVm.deleteConversation(it) },
                onClearBanners = { guideVm.clearBanners() },
                onBack = { popBack() }
            )

            SparkRoute.RecentlyDeleted -> RecentlyDeletedScreen(
                studioState = studioState,
                onRestoreItem = { studioVm.restoreDeletedItem(it) },
                onPermanentDelete = { studioVm.permanentlyDeleteTrashItem(it) },
                onPurgeExpired = { studioVm.purgeExpiredTrash() },
                onClearBanners = { studioVm.clearBanners() },
                onBack = { popBack() },
                onNavigate = { navigateTo(it) }
            )

            SparkRoute.AdminDashboard -> AdminDashboardScreen(
                adminState = adminState,
                onToggleSuspend = { adminVm.toggleSuspendUser(it) },
                onAdjustCredits = { uid, delta, rsn -> adminVm.adjustCredits(uid, delta, rsn) },
                onRetryJob = { adminVm.retryJob(it) },
                onResolveTicket = { t, st -> adminVm.resolveTicket(t, st) },
                onModerateReport = { r, rem -> adminVm.moderateReport(r, rem) },
                onClearMessages = { adminVm.clearMessages() },
                onBack = { popBack() },
                onNavigate = { navigateTo(it) }
            )

            SparkRoute.AdminApiConfig -> AdminApiConfigScreen(
                adminState = adminState,
                onSaveSetting = { adminVm.saveAdminSetting(it) },
                onTestAll = { adminVm.testAllConnections() },
                onTestSingle = { adminVm.testSingleConnection(it) },
                onClearMessages = { adminVm.clearMessages() },
                onBack = { popBack() }
            )

            SparkRoute.AdminSecurity -> AdminSecurityScreen(
                adminState = adminState,
                onRunPostureAudit = { adminVm.runSecurityPostureAudit() },
                onRevokeSession = { adminVm.revokeAnySession(it) },
                onResolveAlert = { adminVm.resolveSecurityAlert(it) },
                onClearMessages = { adminVm.clearMessages() },
                onBack = { popBack() }
            )

            SparkRoute.AdminGuideArticles -> AdminGuideArticlesScreen(
                adminState = adminState,
                onUpsertArticle = { adminVm.upsertGuideArticle(it) },
                onDeleteArticle = { adminVm.deleteGuideArticle(it) },
                onClearMessages = { adminVm.clearMessages() },
                onBack = { popBack() }
            )
        }

        val showFloatingGuide = when (currentRoute) {
            is SparkRoute.Main -> currentRoute.tab in listOf(SparkTab.HOME, SparkTab.CREATE, SparkTab.PROFILE)
            is SparkRoute.CreateMovie,
            SparkRoute.SingingPhoto,
            is SparkRoute.MusicLibrary,
            is SparkRoute.MusicVideoStudio,
            SparkRoute.CommunityHub,
            SparkRoute.MyCreations,
            SparkRoute.MovieLibrary,
            SparkRoute.SettingsHub,
            is SparkRoute.SettingsSub,
            is SparkRoute.CreatorProfileRoute -> true
            else -> false
        }

        if (showFloatingGuide) {
            val guideContext = when (currentRoute) {
                is SparkRoute.SettingsSub -> currentRoute.section
                is SparkRoute.CreateMovie,
                SparkRoute.SingingPhoto,
                is SparkRoute.MusicVideoStudio -> "studio"
                is SparkRoute.Main -> if (currentRoute.tab == SparkTab.CREATE) "posts" else "general"
                SparkRoute.MyCreations,
                SparkRoute.MovieLibrary -> "creations"
                else -> "general"
            }
            FloatingSparkGuideButton(
                onClick = { navigateTo(SparkRoute.SparkGuide(contextSection = guideContext)) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(
                        end = 16.dp,
                        bottom = if (currentRoute is SparkRoute.Main && !isWideMainLayout) 92.dp else 20.dp
                    )
            )
        }

        if (appLockEnabled && !isAppUnlocked) {
            SparkAppLockGate(
                note = appLockNote,
                onUnlockClick = {
                    val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                    @Suppress("DEPRECATION")
                    val confirmIntent = km?.createConfirmDeviceCredentialIntent(
                        "Unlock Spark AI",
                        "Confirm your device PIN, pattern, or biometrics"
                    )
                    if (confirmIntent != null) {
                        awaitingUnlockResult = true
                        keyguardLauncher.launch(confirmIntent)
                    } else if (km?.isDeviceSecure == false) {
                        isAppUnlocked = true
                        appLockNote = null
                    } else {
                        appLockNote = "Unable to open device lock screen on this device."
                    }
                },
                onDisableLock = {
                    studioVm.toggleAppLock(false)
                    isAppUnlocked = true
                    appLockNote = null
                }
            )
        }
    }
}

@Composable
private fun SparkAppLockGate(
    note: String?,
    onUnlockClick: () -> Unit,
    onDisableLock: () -> Unit
) {
    BackHandler(enabled = true) {}
    val blockerInteraction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightBlack)
            .clickable(interactionSource = blockerInteraction, indication = null) {}
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Spark AI Locked",
                tint = ChampagneGold,
                modifier = Modifier.size(56.dp)
            )
            Text(
                text = "Spark AI Is Locked",
                style = MaterialTheme.typography.headlineSmall,
                color = IceWhite
            )
            Text(
                text = note ?: "Verify your device lock screen to access your studio and creations.",
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryIce,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            SparkGoldButton(
                text = "Unlock Spark AI",
                onClick = onUnlockClick,
                modifier = Modifier.testTag("unlock_app_lock_btn")
            )
            SparkOutlinedButton(
                text = "Disable App Lock",
                onClick = onDisableLock,
                modifier = Modifier.testTag("disable_app_lock_btn")
            )
        }
    }
}
