package com.example.core

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

data class VoiceOption(
    val id: String,
    val label: String,
    val locale: Locale,
    val genderHint: String,
    val pitchMultiplier: Float
)

class VoiceAndAudioEngine(context: Context) {
    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private var mediaPlayer: MediaPlayer? = null

    private val _ttsReady = MutableStateFlow(false)
    val ttsReady: StateFlow<Boolean> = _ttsReady.asStateFlow()

    private val _supportedVoices = MutableStateFlow<List<VoiceOption>>(emptyList())
    val supportedVoices: StateFlow<List<VoiceOption>> = _supportedVoices.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isAudioPlaying = MutableStateFlow(false)
    val isAudioPlaying: StateFlow<Boolean> = _isAudioPlaying.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                _ttsReady.value = true
                discoverSupportedVoices()
            } else {
                _ttsReady.value = false
                _statusMessage.value = "Device Text-to-Speech engine is unavailable."
            }
        }
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
            }

            override fun onDone(utteranceId: String?) {
                _isSpeaking.value = false
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
                _statusMessage.value = "Voice synthesis failed for selected language."
            }
        })
    }

    private fun discoverSupportedVoices() {
        val engine = tts ?: return
        val candidateLocales = listOf(
            Locale("en", "NG") to "Nigerian English",
            Locale.US to "US English",
            Locale.UK to "British English",
            Locale.FRENCH to "French",
            Locale("es", "ES") to "Spanish",
            Locale("pt", "BR") to "Portuguese",
            Locale.GERMAN to "German"
        )
        val available = mutableListOf<VoiceOption>()
        for ((loc, name) in candidateLocales) {
            val res = engine.isLanguageAvailable(loc)
            if (res >= TextToSpeech.LANG_AVAILABLE) {
                available.add(
                    VoiceOption(
                        id = "${loc.toLanguageTag()}_female",
                        label = "$name — Warm Female",
                        locale = loc,
                        genderHint = "Female",
                        pitchMultiplier = 1.12f
                    )
                )
                available.add(
                    VoiceOption(
                        id = "${loc.toLanguageTag()}_male",
                        label = "$name — Deep Male",
                        locale = loc,
                        genderHint = "Male",
                        pitchMultiplier = 0.85f
                    )
                )
            }
        }
        if (available.isEmpty()) {
            available.add(
                VoiceOption(
                    id = "default_female",
                    label = "Device Default — Female",
                    locale = Locale.getDefault(),
                    genderHint = "Female",
                    pitchMultiplier = 1.1f
                )
            )
            available.add(
                VoiceOption(
                    id = "default_male",
                    label = "Device Default — Male",
                    locale = Locale.getDefault(),
                    genderHint = "Male",
                    pitchMultiplier = 0.86f
                )
            )
        }
        _supportedVoices.value = available
    }

    fun speakPreview(
        text: String,
        voiceOption: VoiceOption?,
        speed: Float,
        tonePitchOffset: Float
    ) {
        val engine = tts
        if (engine == null || !_ttsReady.value) {
            _statusMessage.value = "TTS voice engine is still initializing or unavailable on this device."
            return
        }
        if (text.isBlank()) {
            _statusMessage.value = "Enter dialogue or narration text to preview."
            return
        }
        val locale = voiceOption?.locale ?: Locale.getDefault()
        val langRes = engine.setLanguage(locale)
        if (langRes == TextToSpeech.LANG_MISSING_DATA || langRes == TextToSpeech.LANG_NOT_SUPPORTED) {
            _statusMessage.value = "${locale.displayName} voice data is not installed on this device."
            return
        }
        engine.setSpeechRate(speed.coerceIn(0.5f, 2.0f))
        val pitch = ((voiceOption?.pitchMultiplier ?: 1.0f) + tonePitchOffset).coerceIn(0.5f, 1.8f)
        engine.setPitch(pitch)
        _statusMessage.value = null
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
    }

    fun stopSpeaking() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun playLocalAudio(uri: Uri, volume: Float) {
        stopAudio()
        try {
            val player = MediaPlayer().apply {
                setDataSource(appContext, uri)
                setVolume(volume.coerceIn(0f, 1f), volume.coerceIn(0f, 1f))
                setOnPreparedListener {
                    it.start()
                    _isAudioPlaying.value = true
                }
                setOnCompletionListener {
                    _isAudioPlaying.value = false
                }
                setOnErrorListener { _, _, _ ->
                    _isAudioPlaying.value = false
                    _statusMessage.value = "Could not play selected audio track."
                    true
                }
                prepareAsync()
            }
            mediaPlayer = player
            _statusMessage.value = null
        } catch (e: Exception) {
            _statusMessage.value = e.localizedMessage ?: "Audio preview error."
            _isAudioPlaying.value = false
        }
    }

    fun setAudioVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        mediaPlayer?.setVolume(clamped, clamped)
    }

    private var mediaRecorder: android.media.MediaRecorder? = null
    private var recordingFile: java.io.File? = null
    private var recordingStartMs: Long = 0L

    private val _isRecordingVoice = MutableStateFlow(false)
    val isRecordingVoice: StateFlow<Boolean> = _isRecordingVoice.asStateFlow()

    fun startVoiceRecording(): Boolean {
        stopVoiceRecording(cancel = true)
        return try {
            val outDir = java.io.File(appContext.cacheDir, "voice_msgs").apply { mkdirs() }
            val outFile = java.io.File(outDir, "voice_${System.currentTimeMillis()}.m4a")
            val recorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                android.media.MediaRecorder(appContext)
            } else {
                @Suppress("DEPRECATION")
                android.media.MediaRecorder()
            }
            recorder.setAudioSource(android.media.MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(android.media.MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioEncodingBitRate(64000)
            recorder.setAudioSamplingRate(22050)
            recorder.setOutputFile(outFile.absolutePath)
            recorder.prepare()
            recorder.start()
            mediaRecorder = recorder
            recordingFile = outFile
            recordingStartMs = System.currentTimeMillis()
            _isRecordingVoice.value = true
            _statusMessage.value = null
            true
        } catch (e: Exception) {
            _statusMessage.value = e.localizedMessage ?: "Unable to start microphone recording."
            _isRecordingVoice.value = false
            false
        }
    }

    fun stopVoiceRecording(cancel: Boolean = false): Pair<String, Int>? {
        val file = recordingFile
        val elapsedSec = ((System.currentTimeMillis() - recordingStartMs) / 1000L).toInt().coerceAtLeast(1)
        try {
            mediaRecorder?.stop()
        } catch (_: Exception) {
        }
        try {
            mediaRecorder?.release()
        } catch (_: Exception) {
        }
        mediaRecorder = null
        recordingFile = null
        _isRecordingVoice.value = false
        if (cancel || file == null || !file.exists()) return null
        return try {
            val bytes = file.readBytes()
            if (bytes.size > 240_000) {
                _statusMessage.value = "Voice clip is too long for inline message storage (keep under ~20s)."
                null
            } else {
                val b64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                "data:audio/mp4;base64,$b64" to elapsedSec
            }
        } catch (e: Exception) {
            _statusMessage.value = e.localizedMessage
            null
        }
    }

    fun playBase64Audio(dataUri: String) {
        stopAudio()
        try {
            val clean = dataUri.substringAfter("base64,", dataUri)
            val bytes = android.util.Base64.decode(clean, android.util.Base64.DEFAULT)
            val temp = java.io.File(appContext.cacheDir, "play_voice_${System.currentTimeMillis()}.m4a")
            temp.writeBytes(bytes)
            playLocalAudio(Uri.fromFile(temp), 1.0f)
        } catch (e: Exception) {
            _statusMessage.value = e.localizedMessage ?: "Failed to play voice note."
        }
    }

    fun stopAudio() {
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {
        }
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {
        }
        mediaPlayer = null
        _isAudioPlaying.value = false
    }

    fun release() {
        stopSpeaking()
        stopAudio()
        stopVoiceRecording(cancel = true)
        tts?.shutdown()
        tts = null
    }
}
