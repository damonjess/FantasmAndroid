package com.example.fantasmandroid

import android.content.Context
import android.media.AudioAttributes
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import kotlin.random.Random

data class ItcEvent(
    val id: Long,
    val phrase: String,
    val timestamp: Long,
    val cause: String,
    val emf: Float
)

/**
 * The ITC (instrumental transcommunication) core. It watches EMF, audio and
 * speech pauses, then surfaces phonetic messages from [ItcDictionary] and,
 * when Spirit Vox is enabled, speaks them with the device voice.
 *
 * The messaging is a simulated ITC experience for entertainment, exactly like
 * the sensor-driven presentation in the iOS app.
 */
class ItcEngine(
    private val context: Context,
    private val settings: SettingsStore
) {
    private val _current = MutableStateFlow<ItcEvent?>(null)
    val current: StateFlow<ItcEvent?> = _current.asStateFlow()

    private val _history = MutableStateFlow<List<ItcEvent>>(emptyList())
    val history: StateFlow<List<ItcEvent>> = _history.asStateFlow()

    private val _voxSpeaking = MutableStateFlow(false)
    val voxSpeaking: StateFlow<Boolean> = _voxSpeaking.asStateFlow()

    private var scope: CoroutineScope? = null
    private var ticker: Job? = null

    @Volatile private var emfLevel = 0f
    @Volatile private var lastEmfSpikeAt = 0L
    @Volatile private var audioLevel = 0f
    @Volatile private var lastTransientAt = 0L
    @Volatile private var lastSpeechEndAt = 0L
    @Volatile private var speechWasActive = false

    private var lastTriggerAt = 0L
    private var lastSpokenAt = 0L
    private var recentPhrases = ArrayDeque<String>()
    private var nextId = 1L
    private val random = Random(System.nanoTime())

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var pendingSpeech: String? = null

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.UK
                tts?.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                ttsReady = true
                pendingSpeech?.let { phrase ->
                    pendingSpeech = null
                    speakInternal(phrase)
                }
            }
        }
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _voxSpeaking.value = true
            }

            override fun onDone(utteranceId: String?) {
                _voxSpeaking.value = false
                lastSpokenAt = System.currentTimeMillis()
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _voxSpeaking.value = false
            }
        })
    }

    fun start() {
        if (scope != null) return
        val newScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        scope = newScope
        ticker = newScope.launch {
            var lastSpeech = System.currentTimeMillis()
            while (isActive) {
                val now = System.currentTimeMillis()
                if (speechWasActive && audioLevel < 0.12f && now - lastSpeech > 350) {
                    speechWasActive = false
                    lastSpeechEndAt = now
                }
                if (audioLevel > 0.2f) {
                    lastSpeech = now
                    speechWasActive = true
                }
                evaluate(now)
                delay(90)
            }
        }
    }

    fun stop() {
        ticker?.cancel()
        ticker = null
        scope?.cancel()
        scope = null
    }

    fun submitEmf(level: Float, spike: Boolean) {
        emfLevel = level
        if (spike) lastEmfSpikeAt = System.currentTimeMillis()
    }

    fun submitAudio(level: Float, transient: Boolean) {
        audioLevel = level
        if (transient) lastTransientAt = System.currentTimeMillis()
    }

    private fun evaluate(now: Long) {
        if (_voxSpeaking.value) return
        if (now - lastTriggerAt < 2_600L) return
        if (now - lastSpokenAt < 2_200L) return

        val sensitivity = settings.state.value.itcSensitivity
        var chance = 0.0015f + sensitivity * 0.002f

        if (now - lastEmfSpikeAt in 0..1_600L) chance += 0.075f * sensitivity
        if (now - lastTransientAt in 0..1_500L) chance += 0.05f * sensitivity
        if (now - lastSpeechEndAt in 250..4_500L) chance += 0.055f * sensitivity
        if (now - lastTriggerAt > 30_000L) chance += 0.012f
        if (emfLevel > 0.75f) chance += 0.01f

        if (random.nextFloat() > chance) return

        val cause = when {
            now - lastEmfSpikeAt in 0..1_600L -> "EMF SPIKE"
            now - lastTransientAt in 0..1_500L -> "AUDIO EVENT"
            now - lastSpeechEndAt in 250..4_500L -> "RESPONSE"
            else -> "AMBIENT"
        }
        trigger(cause)
    }

    fun trigger(cause: String = "MANUAL") {
        publish(pickPhrase(), cause)
    }

    /** Publishes a specific phrase, used by the Matrix word grid. */
    fun publish(phrase: String, cause: String = "MATRIX") {
        val event = ItcEvent(
            id = nextId++,
            phrase = phrase.uppercase(),
            timestamp = System.currentTimeMillis(),
            cause = cause,
            emf = emfLevel
        )
        lastTriggerAt = event.timestamp
        _current.value = event
        _history.value = (_history.value + event).takeLast(400)
        if (settings.state.value.voxEnabled) speak(event.phrase)
    }

    private fun pickPhrase(): String {
        var phrase: String
        var attempts = 0
        do {
            phrase = ItcDictionary.random(random)
            attempts++
        } while (phrase in recentPhrases && attempts < 12)
        recentPhrases.addLast(phrase)
        while (recentPhrases.size > 18) recentPhrases.removeFirst()
        return phrase
    }

    fun speak(phrase: String? = null) {
        val text = phrase ?: _current.value?.phrase ?: return
        if (!ttsReady) {
            pendingSpeech = text
            return
        }
        speakInternal(text)
    }

    private fun speakInternal(phrase: String) {
        val config = settings.state.value
        val engine = tts ?: return
        engine.setSpeechRate(
            when {
                config.whisperMode -> 0.98f
                config.enhancedVox -> 0.72f
                else -> 0.82f
            }
        )
        engine.setPitch(
            when {
                config.whisperMode -> 1.08f
                config.enhancedVox -> 0.52f + random.nextFloat() * 0.24f
                else -> 0.66f
            }
        )
        val params = Bundle().apply {
            if (config.whisperMode) {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 0.45f)
            }
        }
        engine.speak(phrase, TextToSpeech.QUEUE_FLUSH, params, "itc-${System.currentTimeMillis()}")
    }

    fun shutdown() {
        stop()
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
