package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.AuthManager.authStateFlow
import com.example.core.RateLimiter
import com.example.core.SecurityValidator
import com.example.core.ValidationResult
import com.example.data.ai.SparkAiService
import com.example.data.model.SparkGuideArticle
import com.example.data.model.SparkGuideCatalog
import com.example.data.model.SparkGuideConversation
import com.example.data.model.SparkGuideMessage
import com.example.data.repository.GuideAndSecurityRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class LocalGuideBubble(
    val id: String,
    val role: String, // user, assistant
    val text: String,
    val offlineFallback: Boolean = false
)

data class SparkGuideUiState(
    val conversations: List<SparkGuideConversation> = emptyList(),
    val activeConversationId: String? = null,
    val messages: List<LocalGuideBubble> = listOf(
        LocalGuideBubble(
            id = "welcome",
            role = "assistant",
            text = SparkGuideCatalog.WELCOME_MESSAGE
        )
    ),
    val streamingText: String = "",
    val isGenerating: Boolean = false,
    val selectedLanguage: String = "English",
    val contextSection: String = "general",
    val articles: List<SparkGuideArticle> = emptyList(),
    val errorBanner: String? = null,
    val statusBanner: String? = null
)

class SparkGuideViewModel(application: Application) : AndroidViewModel(application) {
    private val guideRepo = GuideAndSecurityRepository(application.applicationContext)
    private val aiService = SparkAiService()

    private val _state = MutableStateFlow(SparkGuideUiState())
    val state: StateFlow<SparkGuideUiState> = _state.asStateFlow()

    private var observerJob: Job? = null
    private var messagesJob: Job? = null
    private var sendJob: Job? = null

    init {
        viewModelScope.launch {
            Firebase.auth.authStateFlow().collectLatest { user ->
                observerJob?.cancel()
                messagesJob?.cancel()
                if (user == null) {
                    _state.value = SparkGuideUiState()
                } else {
                    observerJob = viewModelScope.launch {
                        launch {
                            guideRepo.observeConversations(user.uid).catch {}.collect { convs ->
                                _state.value = _state.value.copy(conversations = convs)
                            }
                        }
                        launch {
                            guideRepo.observeGuideArticles().catch {}.collect { arts ->
                                _state.value = _state.value.copy(articles = arts)
                            }
                        }
                    }
                }
            }
        }
    }

    fun setContextSection(section: String) {
        _state.value = _state.value.copy(contextSection = section.ifBlank { "general" })
    }

    fun setLanguage(language: String) {
        _state.value = _state.value.copy(selectedLanguage = language)
    }

    fun clearBanners() {
        _state.value = _state.value.copy(errorBanner = null, statusBanner = null)
    }

    fun startNewChat() {
        sendJob?.cancel()
        messagesJob?.cancel()
        _state.value = _state.value.copy(
            activeConversationId = null,
            isGenerating = false,
            streamingText = "",
            errorBanner = null,
            messages = listOf(
                LocalGuideBubble(
                    id = "welcome_${System.currentTimeMillis()}",
                    role = "assistant",
                    text = SparkGuideCatalog.WELCOME_MESSAGE
                )
            )
        )
    }

    fun openConversation(conversation: SparkGuideConversation) {
        val uid = Firebase.auth.currentUser?.uid ?: return
        sendJob?.cancel()
        messagesJob?.cancel()
        _state.value = _state.value.copy(
            activeConversationId = conversation.id,
            selectedLanguage = conversation.language.ifBlank { "English" },
            contextSection = conversation.contextSection.ifBlank { "general" },
            streamingText = "",
            isGenerating = false
        )
        messagesJob = viewModelScope.launch {
            guideRepo.observeMessages(uid, conversation.id).catch {}.collect { dbMsgs ->
                val bubbles = if (dbMsgs.isEmpty()) {
                    listOf(LocalGuideBubble("welcome", "assistant", SparkGuideCatalog.WELCOME_MESSAGE))
                } else {
                    dbMsgs.map { m ->
                        LocalGuideBubble(
                            id = m.id,
                            role = m.role,
                            text = m.text,
                            offlineFallback = m.offlineFallback
                        )
                    }
                }
                _state.value = _state.value.copy(messages = bubbles)
            }
        }
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            val res = guideRepo.deleteGuideConversation(conversationId)
            if (_state.value.activeConversationId == conversationId) {
                startNewChat()
            }
            if (res.isFailure) {
                _state.value = _state.value.copy(errorBanner = res.exceptionOrNull()?.localizedMessage)
            } else {
                _state.value = _state.value.copy(statusBanner = "Chat history deleted.")
            }
        }
    }

    fun askSparkGuide(rawQuestion: String) {
        val currentUser = Firebase.auth.currentUser
        val validation = SecurityValidator.validateUserInput(rawQuestion, "Question", maxLength = 2000)
        val cleanQuestion = when (validation) {
            is ValidationResult.Valid -> validation.sanitized
            is ValidationResult.Invalid -> {
                _state.value = _state.value.copy(errorBanner = validation.reason)
                if (validation.riskCategory != "empty_input" && currentUser != null) {
                    viewModelScope.launch {
                        guideRepo.logSecurityAlert(
                            targetUserId = currentUser.uid,
                            alertType = "suspicious_input",
                            severity = "medium",
                            title = "Blocked Unsafe Spark Guide Input",
                            description = "Suspicious input blocked in Spark Guide: ${validation.riskCategory}"
                        )
                    }
                }
                return
            }
        }

        val rateDecision = RateLimiter.shared.checkAndRecord("spark_guide", maxRequests = 12, windowMs = 60_000L)
        if (!rateDecision.allowed) {
            _state.value = _state.value.copy(errorBanner = rateDecision.reason)
            if (currentUser != null) {
                viewModelScope.launch {
                    guideRepo.logSecurityAlert(
                        targetUserId = currentUser.uid,
                        alertType = "rate_limit",
                        severity = "low",
                        title = "Spark Guide Rate Limit Triggered",
                        description = rateDecision.reason
                    )
                }
            }
            return
        }

        val prevBubbles = _state.value.messages
        val userBubble = LocalGuideBubble(
            id = "u_${System.currentTimeMillis()}",
            role = "user",
            text = cleanQuestion
        )
        _state.value = _state.value.copy(
            messages = prevBubbles + userBubble,
            isGenerating = true,
            streamingText = "",
            errorBanner = null
        )

        val historyTurns = prevBubbles
            .filter { it.id != "welcome" && !it.id.startsWith("welcome_") }
            .map { it.role to it.text }

        val kbContext = _state.value.articles
            .filter { it.published }
            .take(8)
            .joinToString("\n\n") { "${it.title} (${it.category}):\n${it.stepsSummary}" }

        sendJob?.cancel()
        sendJob = viewModelScope.launch {
            val acc = StringBuilder()
            var usedOfflineFallback = false
            var streamError: String? = null
            try {
                aiService.streamSparkGuideReply(
                    userQuestion = cleanQuestion,
                    historyTurns = historyTurns,
                    language = _state.value.selectedLanguage,
                    contextSection = _state.value.contextSection,
                    kbArticlesContext = kbContext
                ).collect { chunk ->
                    acc.append(chunk)
                    _state.value = _state.value.copy(streamingText = acc.toString())
                }
            } catch (e: Exception) {
                streamError = e.localizedMessage
            }

            val finalReply = if (acc.isNotBlank()) {
                acc.toString().trim()
            } else {
                val matched = SparkGuideCatalog.matchOfflineAnswer(cleanQuestion, _state.value.articles)
                if (matched != null) {
                    usedOfflineFallback = true
                    "Offline Spark Guide Help (Gemini unavailable: ${streamError ?: "offline mode"}):\n\n" +
                        "${matched.title}\n${matched.answer}"
                } else {
                    "Spark Guide couldn't reach Gemini right now (${streamError ?: "network error"}). " +
                        "Please check your connection and tap a starter topic or retry."
                }
            }

            val assistantBubble = LocalGuideBubble(
                id = "a_${System.currentTimeMillis()}",
                role = "assistant",
                text = finalReply,
                offlineFallback = usedOfflineFallback
            )

            val savedConvId = if (currentUser != null) {
                guideRepo.saveGuideTurn(
                    conversationId = _state.value.activeConversationId,
                    title = cleanQuestion.take(80),
                    contextSection = _state.value.contextSection,
                    language = _state.value.selectedLanguage,
                    userText = cleanQuestion,
                    assistantText = finalReply,
                    offlineFallback = usedOfflineFallback
                ).getOrNull() ?: _state.value.activeConversationId
            } else {
                _state.value.activeConversationId
            }

            _state.value = _state.value.copy(
                activeConversationId = savedConvId,
                messages = _state.value.messages + assistantBubble,
                streamingText = "",
                isGenerating = false
            )
        }
    }
}
