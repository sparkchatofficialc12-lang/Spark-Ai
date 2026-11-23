package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.AuthManager.authStateFlow
import com.example.core.MediaUtils
import com.example.core.RateLimiter
import com.example.core.SecurityValidator
import com.example.core.ValidationResult
import com.example.data.model.CommunityTemplate
import com.example.data.model.ConversationThread
import com.example.data.model.CreatorAnalyticsSummary
import com.example.data.model.CreatorFollower
import com.example.data.model.CreatorProfile
import com.example.data.model.DirectMessage
import com.example.data.model.LiveStreamSession
import com.example.data.model.PostComment
import com.example.data.model.PostLike
import com.example.data.model.PostShare
import com.example.data.model.PostView
import com.example.data.model.RemixWorkspaceSeed
import com.example.data.model.SocialPost
import com.example.data.model.StoryItem
import com.example.data.model.StoryView
import com.example.data.model.TemplateComment
import com.example.data.model.TemplatePrompt
import com.example.data.model.TemplateRemix
import com.example.data.model.TemplateSettingsDoc
import com.example.data.repository.CommunityRepository
import com.example.data.repository.GuideAndSecurityRepository
import com.example.data.repository.SocialRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class SocialUiState(
    val publicPosts: List<SocialPost> = emptyList(),
    val myPosts: List<SocialPost> = emptyList(),
    val myLikedPostIds: Set<String> = emptySet(),
    val myLikedCommentIds: Set<String> = emptySet(),
    val savedPostIds: Set<String> = emptySet(),
    val savedSoundIds: Set<String> = emptySet(),
    val allCreatorProfiles: List<CreatorProfile> = emptyList(),
    val activeStories: List<StoryItem> = emptyList(),
    val selectedStoryViews: List<StoryView> = emptyList(),
    val conversations: List<ConversationThread> = emptyList(),
    val activeConversationId: String = "",
    val activeMessages: List<DirectMessage> = emptyList(),
    val liveStreams: List<LiveStreamSession> = emptyList(),
    val publicTemplates: List<CommunityTemplate> = emptyList(),
    val myTemplates: List<CommunityTemplate> = emptyList(),
    val myTemplateLikeIds: Set<String> = emptySet(),
    val savedTemplateIds: Set<String> = emptySet(),
    val blockedCreatorIds: Set<String> = emptySet(),
    val followingCreatorIds: Set<String> = emptySet(),
    val selectedPost: SocialPost? = null,
    val selectedPostViews: List<PostView> = emptyList(),
    val selectedPostLikes: List<PostLike> = emptyList(),
    val selectedPostComments: List<PostComment> = emptyList(),
    val selectedPostShares: List<PostShare> = emptyList(),
    val selectedTemplate: CommunityTemplate? = null,
    val selectedTemplateComments: List<TemplateComment> = emptyList(),
    val selectedTemplateRemixes: List<TemplateRemix> = emptyList(),
    val activeCreatorProfile: CreatorProfile? = null,
    val activeCreatorPosts: List<SocialPost> = emptyList(),
    val activeCreatorFollowers: List<CreatorFollower> = emptyList(),
    val remixSeed: RemixWorkspaceSeed? = null,
    val analytics: CreatorAnalyticsSummary = CreatorAnalyticsSummary(),
    val isBusy: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

class SocialViewModel(application: Application) : AndroidViewModel(application) {
    private val socialRepo = SocialRepository(application.applicationContext)
    private val communityRepo = CommunityRepository(application.applicationContext)
    private val securityRepo = GuideAndSecurityRepository(application.applicationContext)

    private val _state = MutableStateFlow(SocialUiState())
    val state: StateFlow<SocialUiState> = _state.asStateFlow()

    private var mainObserverJob: Job? = null
    private var postDetailJob: Job? = null
    private var storyViewsJob: Job? = null
    private var chatMessagesJob: Job? = null
    private var templateDetailJob: Job? = null
    private var creatorProfileJob: Job? = null

    init {
        viewModelScope.launch {
            Firebase.auth.authStateFlow().collectLatest { user ->
                mainObserverJob?.cancel()
                if (user == null) {
                    _state.value = SocialUiState()
                } else {
                    startFeedObservers(user.uid)
                }
            }
        }
    }

    private fun startFeedObservers(uid: String) {
        mainObserverJob = viewModelScope.launch {
            launch { socialRepo.observePublicPosts().catch {}.collect { updatePostsAndAnalytics(publicPosts = it) } }
            launch { socialRepo.observeMyPosts(uid).catch {}.collect { updatePostsAndAnalytics(myPosts = it) } }
            launch { socialRepo.observeMyLikedPostIds(uid).catch {}.collect { _state.value = _state.value.copy(myLikedPostIds = it) } }
            launch { socialRepo.observeMyLikedCommentIds(uid).catch {}.collect { _state.value = _state.value.copy(myLikedCommentIds = it) } }
            launch { socialRepo.observeSavedPostIds(uid).catch {}.collect { _state.value = _state.value.copy(savedPostIds = it) } }
            launch { socialRepo.observeSavedSoundIds(uid).catch {}.collect { _state.value = _state.value.copy(savedSoundIds = it) } }
            launch { socialRepo.observeAllCreatorProfiles().catch {}.collect { _state.value = _state.value.copy(allCreatorProfiles = it) } }
            launch { socialRepo.observeActiveStories(uid).catch {}.collect { _state.value = _state.value.copy(activeStories = it) } }
            launch { socialRepo.observeConversations(uid).catch {}.collect { _state.value = _state.value.copy(conversations = it) } }
            launch { socialRepo.observeLiveStreams().catch {}.collect { _state.value = _state.value.copy(liveStreams = it) } }
            launch { communityRepo.observePublicTemplates().catch {}.collect { _state.value = _state.value.copy(publicTemplates = it) } }
            launch { communityRepo.observeMyTemplates(uid).catch {}.collect { updatePostsAndAnalytics(myTemplates = it) } }
            launch { communityRepo.observeMyLikes(uid).catch {}.collect { _state.value = _state.value.copy(myTemplateLikeIds = it) } }
            launch { communityRepo.observeSavedTemplateIds(uid).catch {}.collect { _state.value = _state.value.copy(savedTemplateIds = it) } }
            launch { communityRepo.observeBlockedCreatorIds(uid).catch {}.collect { _state.value = _state.value.copy(blockedCreatorIds = it) } }
            launch { communityRepo.observeFollowingCreatorIds(uid).catch {}.collect { _state.value = _state.value.copy(followingCreatorIds = it) } }
            launch {
                socialRepo.observeFollowersOf(uid).catch {}.collect { followers ->
                    val current = _state.value
                    _state.value = current.copy(
                        analytics = socialRepo.buildAnalyticsSummary(current.myPosts, current.myTemplates, followers.size)
                    )
                }
            }
        }
    }

    private fun updatePostsAndAnalytics(
        publicPosts: List<SocialPost>? = null,
        myPosts: List<SocialPost>? = null,
        myTemplates: List<CommunityTemplate>? = null
    ) {
        val cur = _state.value
        val nextPublic = publicPosts ?: cur.publicPosts
        val nextMyPosts = myPosts ?: cur.myPosts
        val nextMyTemplates = myTemplates ?: cur.myTemplates
        val refreshedSelected = cur.selectedPost?.let { sel ->
            nextPublic.firstOrNull { it.id == sel.id } ?: nextMyPosts.firstOrNull { it.id == sel.id } ?: sel
        }
        val refreshedTpl = cur.selectedTemplate?.let { tpl ->
            cur.publicTemplates.firstOrNull { it.id == tpl.id } ?: nextMyTemplates.firstOrNull { it.id == tpl.id } ?: tpl
        }
        _state.value = cur.copy(
            publicPosts = nextPublic,
            myPosts = nextMyPosts,
            myTemplates = nextMyTemplates,
            selectedPost = refreshedSelected,
            selectedTemplate = refreshedTpl,
            analytics = socialRepo.buildAnalyticsSummary(nextMyPosts, nextMyTemplates, cur.analytics.followersCount)
        )
    }

    fun clearMessages() {
        _state.value = _state.value.copy(statusMessage = null, errorMessage = null)
    }

    fun updateCreatorCoverAndBio(username: String, displayName: String, avatarUrl: String, bio: String, coverBitmap: Bitmap?) {
        viewModelScope.launch {
            val coverB64 = coverBitmap?.let { MediaUtils.bitmapToCompactBase64Async(it, 180_000) }
            val res = socialRepo.ensureCreatorProfile(username, displayName, avatarUrl, bio, coverB64)
            _state.value = _state.value.copy(
                statusMessage = if (res.isSuccess) "Creator profile updated." else null,
                errorMessage = res.exceptionOrNull()?.localizedMessage
            )
        }
    }

    fun openPostDetail(post: SocialPost, viewerUsername: String, viewerAvatar: String, viewerPublic: Boolean) {
        val uid = Firebase.auth.currentUser?.uid ?: return
        postDetailJob?.cancel()
        _state.value = _state.value.copy(
            selectedPost = post,
            selectedPostViews = emptyList(),
            selectedPostLikes = emptyList(),
            selectedPostComments = emptyList(),
            selectedPostShares = emptyList()
        )
        val isOwner = post.creatorId == uid
        postDetailJob = viewModelScope.launch {
            socialRepo.recordPostView(post, viewerUsername, viewerAvatar, viewerPublic)
            launch { socialRepo.observePostViews(post.id, isOwner, uid).catch {}.collect { _state.value = _state.value.copy(selectedPostViews = it) } }
            launch { socialRepo.observePostLikes(post.id, isOwner, uid).catch {}.collect { _state.value = _state.value.copy(selectedPostLikes = it) } }
            launch { socialRepo.observePostComments(post.id).catch {}.collect { _state.value = _state.value.copy(selectedPostComments = it) } }
            launch { socialRepo.observePostShares(post.id, post.creatorId).catch {}.collect { _state.value = _state.value.copy(selectedPostShares = it) } }
        }
    }

    fun openPostById(postId: String, viewerUsername: String, viewerAvatar: String, viewerPublic: Boolean) {
        val local = (_state.value.publicPosts + _state.value.myPosts + _state.value.activeCreatorPosts)
            .firstOrNull { it.id == postId }
        if (local != null) {
            openPostDetail(local, viewerUsername, viewerAvatar, viewerPublic)
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isBusy = true, errorMessage = null)
            try {
                val res = socialRepo.getPostById(postId)
                val fetched = res.getOrNull()
                if (fetched != null) {
                    openPostDetail(fetched, viewerUsername, viewerAvatar, viewerPublic)
                } else {
                    _state.value = _state.value.copy(
                        errorMessage = res.exceptionOrNull()?.localizedMessage ?: "Post not found or is private."
                    )
                }
            } finally {
                _state.value = _state.value.copy(isBusy = false)
            }
        }
    }

    fun openStory(story: StoryItem, viewerUsername: String, viewerAvatar: String, viewerPublic: Boolean) {
        val uid = Firebase.auth.currentUser?.uid ?: return
        storyViewsJob?.cancel()
        _state.value = _state.value.copy(selectedStoryViews = emptyList())
        storyViewsJob = viewModelScope.launch {
            socialRepo.recordStoryView(story, viewerUsername, viewerAvatar, viewerPublic)
            if (story.creatorId == uid) {
                socialRepo.observeStoryViews(story.id, uid).catch {}.collect {
                    _state.value = _state.value.copy(selectedStoryViews = it)
                }
            }
        }
    }

    fun createStory(
        creatorUsername: String,
        creatorAvatarUrl: String,
        mediaType: String,
        mediaUrl: String,
        bitmap: Bitmap?,
        overlayText: String,
        stickerTag: String,
        caption: String,
        musicTrackTitle: String,
        visibility: String,
        onCreated: () -> Unit
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isBusy = true, errorMessage = null)
            try {
                val thumb = bitmap?.let { MediaUtils.bitmapToCompactBase64Async(it, 180_000) }.orEmpty()
                val res = socialRepo.createStory(
                    StoryItem(
                        creatorUsername = creatorUsername,
                        creatorAvatarUrl = creatorAvatarUrl,
                        mediaType = mediaType,
                        mediaUrl = mediaUrl,
                        thumbnailData = thumb,
                        overlayText = overlayText,
                        stickerTag = stickerTag,
                        caption = caption,
                        musicTrackTitle = musicTrackTitle,
                        visibility = visibility
                    )
                )
                _state.value = _state.value.copy(
                    statusMessage = if (res.isSuccess) "24-hour Story posted!" else null,
                    errorMessage = res.exceptionOrNull()?.localizedMessage
                )
                if (res.isSuccess) onCreated()
            } finally {
                _state.value = _state.value.copy(isBusy = false)
            }
        }
    }

    fun deleteStory(storyId: String) {
        viewModelScope.launch {
            socialRepo.deleteStory(storyId)
            _state.value = _state.value.copy(statusMessage = "Story deleted.")
        }
    }

    fun openOrStartConversation(
        peerUserId: String,
        myUsername: String,
        peerUsername: String,
        onReady: (String) -> Unit
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isBusy = true, errorMessage = null)
            try {
                val res = socialRepo.getOrCreateConversation(peerUserId, myUsername, peerUsername)
                _state.value = _state.value.copy(
                    errorMessage = res.exceptionOrNull()?.localizedMessage
                )
                res.getOrNull()?.let { convId ->
                    observeConversationMessages(convId)
                    onReady(convId)
                }
            } finally {
                _state.value = _state.value.copy(isBusy = false)
            }
        }
    }

    fun observeConversationMessages(conversationId: String) {
        if (_state.value.activeConversationId == conversationId && chatMessagesJob?.isActive == true) return
        chatMessagesJob?.cancel()
        _state.value = _state.value.copy(activeConversationId = conversationId, activeMessages = emptyList())
        chatMessagesJob = viewModelScope.launch {
            socialRepo.observeMessages(conversationId).catch {}.collect { msgs ->
                _state.value = _state.value.copy(activeMessages = msgs)
                socialRepo.markMessagesRead(conversationId, msgs)
            }
        }
    }

    fun sendMessage(
        conversationId: String,
        receiverId: String,
        senderUsername: String,
        messageType: String,
        text: String,
        mediaUrl: String = "",
        mediaBitmap: Bitmap? = null,
        audioDurationSec: Int = 0,
        replyToMessageId: String = "",
        replyToPreview: String = ""
    ) {
        viewModelScope.launch {
            val thumb = mediaBitmap?.let { MediaUtils.bitmapToCompactBase64Async(it, 150_000) }.orEmpty()
            val res = socialRepo.sendDirectMessage(
                conversationId = conversationId,
                receiverId = receiverId,
                senderUsername = senderUsername,
                messageType = messageType,
                text = text,
                mediaUrl = mediaUrl,
                mediaThumbnailData = thumb,
                audioDurationSec = audioDurationSec,
                replyToMessageId = replyToMessageId,
                replyToPreview = replyToPreview
            )
            if (res.isFailure) {
                _state.value = _state.value.copy(errorMessage = res.exceptionOrNull()?.localizedMessage)
            }
        }
    }

    fun deleteMessage(conversationId: String, messageId: String) {
        viewModelScope.launch {
            socialRepo.deleteDirectMessage(conversationId, messageId)
        }
    }

    fun openCreatorProfile(creatorId: String) {
        creatorProfileJob?.cancel()
        _state.value = _state.value.copy(
            activeCreatorProfile = null,
            activeCreatorPosts = emptyList(),
            activeCreatorFollowers = emptyList()
        )
        creatorProfileJob = viewModelScope.launch {
            launch { communityRepo.observeCreatorProfile(creatorId).catch {}.collect { _state.value = _state.value.copy(activeCreatorProfile = it) } }
            launch { socialRepo.observeCreatorPublicPosts(creatorId).catch {}.collect { _state.value = _state.value.copy(activeCreatorPosts = it) } }
            launch { socialRepo.observeFollowersOf(creatorId).catch {}.collect { _state.value = _state.value.copy(activeCreatorFollowers = it) } }
        }
    }

    fun openTemplateDetail(template: CommunityTemplate) {
        templateDetailJob?.cancel()
        _state.value = _state.value.copy(selectedTemplate = template)
        templateDetailJob = viewModelScope.launch {
            launch { communityRepo.observeComments(template.id).catch {}.collect { _state.value = _state.value.copy(selectedTemplateComments = it) } }
            launch { communityRepo.observeRemixes(template.id).catch {}.collect { _state.value = _state.value.copy(selectedTemplateRemixes = it) } }
        }
    }

    fun publishCreationAsPost(
        creatorUsername: String,
        creatorDisplayName: String,
        creatorAvatarUrl: String,
        creationType: String,
        mediaType: String = "video",
        title: String,
        caption: String,
        hashtags: String,
        mentions: String = "",
        category: String,
        previewVideoUrl: String,
        thumbnailBitmap: Bitmap?,
        albumBitmaps: List<Bitmap> = emptyList(),
        existingThumbnailData: String,
        soundId: String = "",
        soundTitle: String = "",
        durationSec: Int,
        aspectRatio: String,
        resolution: String,
        visibility: String,
        allowComments: Boolean,
        allowReuse: Boolean,
        allowDownload: Boolean,
        linkedProjectId: String,
        reusableBasePrompt: String,
        alsoPublishReusableTemplate: Boolean,
        onPublished: () -> Unit
    ) {
        if (title.isBlank()) {
            _state.value = _state.value.copy(errorMessage = "Please add a title before publishing.")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isBusy = true, statusMessage = null, errorMessage = null)
            try {
                val thumb = thumbnailBitmap?.let { MediaUtils.bitmapToCompactBase64Async(it, 180_000) } ?: existingThumbnailData
                val albumThumbs = albumBitmaps.take(5).map { MediaUtils.bitmapToCompactBase64Async(it, 55_000) }
                var templateId = ""
                if (alsoPublishReusableTemplate || creationType == "template") {
                    val tplRes = communityRepo.publishTemplate(
                        creatorUsername = creatorUsername,
                        creatorDisplayName = creatorDisplayName,
                        creatorAvatarUrl = creatorAvatarUrl,
                        template = CommunityTemplate(
                            title = title.trim(),
                            description = caption.trim(),
                            category = category,
                            previewVideoUrl = previewVideoUrl,
                            thumbnailData = thumb,
                            durationSec = durationSec.coerceAtLeast(5),
                            aspectRatio = aspectRatio,
                            tags = hashtags,
                            visibility = visibility,
                            allowReuse = allowReuse
                        ),
                        prompt = TemplatePrompt(
                            basePrompt = reusableBasePrompt.ifBlank { caption.ifBlank { title } }
                        ),
                        settings = TemplateSettingsDoc(
                            aspectRatio = aspectRatio,
                            resolution = resolution,
                            durationSec = durationSec.coerceAtLeast(5)
                        )
                    )
                    templateId = tplRes.getOrNull().orEmpty()
                }

                val postRes = socialRepo.publishPost(
                    SocialPost(
                        creatorUsername = creatorUsername,
                        creatorAvatarUrl = creatorAvatarUrl,
                        creationType = creationType,
                        mediaType = mediaType,
                        title = title.trim(),
                        caption = caption.trim(),
                        hashtags = hashtags.trim(),
                        mentions = mentions.trim(),
                        category = category,
                        previewVideoUrl = previewVideoUrl,
                        thumbnailData = thumb.ifBlank { albumThumbs.firstOrNull().orEmpty() },
                        photoThumbnails = albumThumbs,
                        soundId = soundId,
                        soundTitle = soundTitle,
                        durationSec = durationSec.coerceAtLeast(0),
                        aspectRatio = aspectRatio,
                        resolution = resolution,
                        visibility = visibility,
                        allowComments = allowComments,
                        allowReuse = allowReuse,
                        allowDownload = allowDownload,
                        linkedProjectId = linkedProjectId,
                        linkedTemplateId = templateId
                    )
                )
                _state.value = _state.value.copy(
                    statusMessage = if (postRes.isSuccess) "Published \"$title\" to Spark Social ($visibility)!" else null,
                    errorMessage = postRes.exceptionOrNull()?.localizedMessage
                )
                if (postRes.isSuccess) onPublished()
            } finally {
                _state.value = _state.value.copy(isBusy = false)
            }
        }
    }

    fun togglePostLike(post: SocialPost, username: String, avatarUrl: String, userPublic: Boolean) {
        val liked = _state.value.myLikedPostIds.contains(post.id)
        viewModelScope.launch {
            socialRepo.togglePostLike(post, username, avatarUrl, userPublic, liked)
        }
    }

    fun toggleSavePost(post: SocialPost) {
        val saved = _state.value.savedPostIds.contains(post.id)
        viewModelScope.launch {
            val res = socialRepo.toggleSavePost(post, saved)
            if (res.isSuccess) {
                _state.value = _state.value.copy(
                    statusMessage = if (saved) "Removed from Saved Posts." else "Saved \"${post.title}\"."
                )
            }
        }
    }

    fun toggleSaveSound(soundId: String, title: String) {
        val saved = _state.value.savedSoundIds.contains(soundId)
        viewModelScope.launch {
            socialRepo.toggleSaveSound(soundId, title, saved)
        }
    }

    fun repostPost(post: SocialPost, username: String, note: String = "") {
        viewModelScope.launch {
            val res = socialRepo.repostPost(post, username, note)
            _state.value = _state.value.copy(
                statusMessage = if (res.isSuccess) "Reposted \"${post.title}\" to your audience!" else null,
                errorMessage = res.exceptionOrNull()?.localizedMessage
            )
        }
    }

    fun addPostComment(post: SocialPost, parentCommentId: String, username: String, text: String) {
        val limit = RateLimiter.shared.checkAndRecord("comment", maxRequests = 15, windowMs = 60_000L)
        if (!limit.allowed) {
            _state.value = _state.value.copy(errorMessage = limit.reason)
            return
        }
        val cleanText = when (val r = SecurityValidator.validateUserInput(text, "Comment", 1000)) {
            is ValidationResult.Valid -> r.sanitized
            is ValidationResult.Invalid -> {
                _state.value = _state.value.copy(errorMessage = r.reason)
                if (r.riskCategory != "empty_input") {
                    viewModelScope.launch {
                        securityRepo.logSecurityAlert(
                            username = username,
                            alertType = "suspicious_input",
                            severity = "medium",
                            title = "Unsafe Comment Blocked",
                            description = "Blocked comment (${r.riskCategory}) on post ${post.id}"
                        )
                    }
                }
                return
            }
        }
        viewModelScope.launch {
            val res = socialRepo.addPostComment(post, parentCommentId, username, cleanText)
            if (res.isFailure) {
                _state.value = _state.value.copy(errorMessage = res.exceptionOrNull()?.localizedMessage)
            }
        }
    }

    fun toggleCommentLike(comment: PostComment) {
        val liked = _state.value.myLikedCommentIds.contains(comment.id)
        viewModelScope.launch { socialRepo.toggleCommentLike(comment, liked) }
    }

    fun deletePostComment(post: SocialPost, commentId: String) {
        viewModelScope.launch { socialRepo.deletePostComment(post, commentId) }
    }

    fun recordPostShare(post: SocialPost, channel: String, username: String) {
        viewModelScope.launch { socialRepo.recordPostShare(post, channel, username) }
    }

    fun editPost(
        postId: String,
        title: String,
        caption: String,
        hashtags: String,
        visibility: String,
        allowComments: Boolean,
        allowReuse: Boolean,
        allowDownload: Boolean
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isBusy = true)
            try {
                val res = socialRepo.updatePost(
                    postId = postId,
                    title = title,
                    caption = caption,
                    hashtags = hashtags,
                    visibility = visibility,
                    allowComments = allowComments,
                    allowReuse = allowReuse,
                    allowDownload = allowDownload
                )
                _state.value = _state.value.copy(
                    statusMessage = if (res.isSuccess) "Post updated." else null,
                    errorMessage = res.exceptionOrNull()?.localizedMessage
                )
            } finally {
                _state.value = _state.value.copy(isBusy = false)
            }
        }
    }

    fun replacePostVideo(
        existingPost: SocialPost,
        newVideoUrl: String,
        newThumbnailData: String,
        newDurationSec: Int,
        newResolution: String,
        keepOriginalLink: Boolean,
        newTitle: String,
        newCaption: String,
        onDone: (String?) -> Unit = {}
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isBusy = true)
            try {
                val res = socialRepo.replacePostVideo(
                    existingPost = existingPost,
                    newVideoUrl = newVideoUrl,
                    newThumbnailData = newThumbnailData,
                    newDurationSec = newDurationSec,
                    newResolution = newResolution,
                    keepOriginalLink = keepOriginalLink,
                    newTitle = newTitle,
                    newCaption = newCaption
                )
                _state.value = _state.value.copy(
                    statusMessage = if (res.isSuccess) {
                        if (keepOriginalLink) "Replaced video while preserving original post link."
                        else "Published replacement video as a new post."
                    } else null,
                    errorMessage = res.exceptionOrNull()?.localizedMessage
                )
                onDone(res.getOrNull())
            } finally {
                _state.value = _state.value.copy(isBusy = false)
            }
        }
    }

    fun changePostPrivacy(postId: String, visibility: String) {
        viewModelScope.launch {
            val res = socialRepo.changePostVisibility(postId, visibility)
            _state.value = _state.value.copy(
                statusMessage = if (res.isSuccess) "Post visibility set to $visibility." else null,
                errorMessage = res.exceptionOrNull()?.localizedMessage
            )
        }
    }

    fun togglePostComments(postId: String, allowComments: Boolean) {
        viewModelScope.launch {
            val res = socialRepo.togglePostComments(postId, allowComments)
            _state.value = _state.value.copy(
                statusMessage = if (res.isSuccess) {
                    if (allowComments) "Comments enabled on post." else "Comments disabled on post."
                } else null,
                errorMessage = res.exceptionOrNull()?.localizedMessage
            )
        }
    }

    fun updatePostTemplatePermission(postId: String, allowReuse: Boolean) {
        viewModelScope.launch {
            val res = socialRepo.updatePostTemplatePermission(postId, allowReuse)
            _state.value = _state.value.copy(
                statusMessage = if (res.isSuccess) {
                    if (allowReuse) "Template remixing enabled." else "Template remixing disabled."
                } else null,
                errorMessage = res.exceptionOrNull()?.localizedMessage
            )
        }
    }

    fun softDeletePost(post: SocialPost, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            val res = socialRepo.softDeletePostToRecycleBin(post)
            _state.value = _state.value.copy(
                statusMessage = if (res.isSuccess) "Moved \"${post.title}\" to Recently Deleted (30 days)." else null,
                errorMessage = res.exceptionOrNull()?.localizedMessage
            )
            if (res.isSuccess) onDeleted()
        }
    }

    fun deleteMyPost(postId: String) {
        val post = _state.value.myPosts.firstOrNull { it.id == postId }
        if (post != null) {
            softDeletePost(post)
            return
        }
        viewModelScope.launch {
            val res = socialRepo.deletePost(postId)
            _state.value = _state.value.copy(
                statusMessage = if (res.isSuccess) "Post deleted." else null,
                errorMessage = res.exceptionOrNull()?.localizedMessage
            )
        }
    }

    fun toggleFollowCreator(creatorId: String, followerUsername: String) {
        val following = _state.value.followingCreatorIds.contains(creatorId)
        viewModelScope.launch {
            communityRepo.toggleFollowCreator(creatorId, following, followerUsername)
        }
    }

    fun toggleTemplateLike(template: CommunityTemplate) {
        val liked = _state.value.myTemplateLikeIds.contains(template.id)
        viewModelScope.launch { communityRepo.toggleLike(template, liked) }
    }

    fun toggleSavedTemplate(templateId: String) {
        val saved = _state.value.savedTemplateIds.contains(templateId)
        viewModelScope.launch { communityRepo.toggleSavedTemplate(templateId, saved) }
    }

    fun addTemplateComment(templateId: String, username: String, text: String) {
        viewModelScope.launch { communityRepo.addComment(templateId, username, text) }
    }

    fun startTemplateRemix(template: CommunityTemplate, onLoaded: () -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isBusy = true, errorMessage = null)
            try {
                val res = communityRepo.loadRemixSeed(template)
                if (res.isSuccess) {
                    _state.value = _state.value.copy(isBusy = false, remixSeed = res.getOrThrow())
                    onLoaded()
                } else {
                    _state.value = _state.value.copy(
                        isBusy = false,
                        errorMessage = res.exceptionOrNull()?.localizedMessage ?: "Cannot remix this template."
                    )
                }
            } finally {
                if (_state.value.isBusy) {
                    _state.value = _state.value.copy(isBusy = false)
                }
            }
        }
    }

    fun recordRemixAttribution(seed: RemixWorkspaceSeed, remixerUsername: String, newProjectId: String, newTitle: String, customPrompt: String) {
        viewModelScope.launch {
            communityRepo.recordRemixAttribution(seed, remixerUsername, newProjectId, newTitle, customPrompt)
        }
    }

    fun updateMyTemplateMeta(templateId: String, description: String, visibility: String, allowReuse: Boolean) {
        viewModelScope.launch {
            val res = communityRepo.updateMyTemplateMeta(templateId, description, visibility, allowReuse)
            _state.value = _state.value.copy(
                statusMessage = if (res.isSuccess) "Template updated." else null,
                errorMessage = res.exceptionOrNull()?.localizedMessage
            )
        }
    }

    fun deleteTemplate(templateId: String) {
        viewModelScope.launch {
            communityRepo.deleteTemplate(templateId)
        }
    }

    fun reportContent(targetId: String, creatorId: String, reason: String, details: String) {
        viewModelScope.launch {
            val res = communityRepo.reportTemplate(targetId, creatorId, reason, details)
            _state.value = _state.value.copy(
                statusMessage = if (res.isSuccess) "Safety report submitted to Spark AI moderation." else null,
                errorMessage = res.exceptionOrNull()?.localizedMessage
            )
        }
    }

    fun blockCreator(creatorId: String) {
        viewModelScope.launch {
            val res = communityRepo.blockCreator(creatorId)
            _state.value = _state.value.copy(
                statusMessage = if (res.isSuccess) "Creator blocked from your feeds and inbox." else null
            )
        }
    }
}
