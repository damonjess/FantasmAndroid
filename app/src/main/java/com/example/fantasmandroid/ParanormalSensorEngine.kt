package com.example.fantasmandroid

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import kotlin.math.sqrt
import kotlin.random.Random

class ParanormalSensorEngine(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

    // Dynamic baseline EMF adaptation
    private var baselineEmf = 45f

    // Rolling histories for the UI graphs (last 50 ticks)
    private val _emfHistory = MutableStateFlow(List(50) { 0f })
    val emfHistory = _emfHistory.asStateFlow()

    private val _geoHistory = MutableStateFlow(List(50) { 0f })
    val geoHistory = _geoHistory.asStateFlow()

    // 16-band audio visualizer levels (0f to 1f)
    private val _audioBands = MutableStateFlow(List(16) { 0f })
    val audioBands = _audioBands.asStateFlow()

    // Spirit Box ITC phrase output
    private val _itcPhrase = MutableStateFlow("WAITING FOR YOU")
    val itcPhrase = _itcPhrase.asStateFlow()

    private var audioJob: Job? = null
    private var tts: TextToSpeech? = null
    private var voxEnabled = false

    private val wordBank = listOf(
        "WAITING FOR YOU", "BEHIND YOU", "GET OUT", "IM HERE",
        "COLD", "LISTEN", "HELP ME", "WATCHING", "DARKNESS", "HELLO"
    )

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.UK
                tts?.setPitch(0.6f) // Lower pitch for eerie Vox effect
                tts?.setSpeechRate(0.75f)
            }
        }
    }

    fun start(enableAudio: Boolean) {
        magnetometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        if (enableAudio) startAudioCapture()
    }

    fun setVoxEnabled(enabled: Boolean) {
        voxEnabled = enabled
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        audioJob?.cancel()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        when (event.sensor.type) {
            Sensor.TYPE_MAGNETIC_FIELD -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val magnitude = sqrt(x * x + y * y + z * z)

                // Slowly adapt baseline to local ambient room interference
                baselineEmf = (baselineEmf * 0.98f) + (magnitude * 0.02f)

                // Fluctuation relative to the current room baseline
                val delta = magnitude - baselineEmf
                val normalized = (0.25f + (delta / 40f)).coerceIn(0.05f, 1f)

                _emfHistory.value = (_emfHistory.value.drop(1) + normalized)

                if (delta > 25f && Random.nextFloat() < 0.08f) {
                    triggerItcWord()
                }
            }
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val vibration = sqrt(x * x + y * y + z * z)
                val normalized = (vibration / 5f).coerceIn(-1f, 1f)
                _geoHistory.value = (_geoHistory.value.drop(1) + normalized)
            }
        }
    }

    private fun triggerItcWord() {
        val nextWord = wordBank.random()
        if (_itcPhrase.value != nextWord) {
            _itcPhrase.value = nextWord
            if (voxEnabled) {
                tts?.speak(nextWord, TextToSpeech.QUEUE_FLUSH, null, "ITC_ID")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startAudioCapture() {
        audioJob?.cancel()
        audioJob = CoroutineScope(Dispatchers.IO).launch {
            val sampleRate = 44100
            val minBuf = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = if (minBuf > 0) minBuf else 2048

            val recorder = try {
                AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                )
            } catch (e: Exception) {
                null
            }

            recorder?.startRecording()
            val buffer = ShortArray(bufferSize)

            while (isActive && recorder?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                val read = recorder.read(buffer, 0, bufferSize)
                if (read > 0) {
                    val chunkSize = (read / 16).coerceAtLeast(1)
                    val bands = List(16) { bandIdx ->
                        var sum = 0.0
                        val start = bandIdx * chunkSize
                        val end = (start + chunkSize).coerceAtMost(read)
                        for (i in start until end) {
                            sum += buffer[i] * buffer[i]
                        }
                        val rms = sqrt(sum / (end - start).coerceAtLeast(1))
                        // Lower divisor so quiet room audio still lights up lower green bars
                        (rms.toFloat() / 1800f).coerceIn(0.08f, 1f)
                    }
                    _audioBands.value = bands
                }
                delay(30)
            }
            recorder?.stop()
            recorder?.release()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}