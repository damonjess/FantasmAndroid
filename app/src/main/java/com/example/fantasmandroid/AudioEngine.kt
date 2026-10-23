package com.example.fantasmandroid

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Microphone analysis for the frequency spectrum, VU strip and EVP capture.
 * The same PCM stream drives the FFT display and the WAV file written for a
 * session, so recording never fights the analyser for the microphone.
 */
class AudioEngine(private val context: Context) {

    private val _bands = MutableStateFlow(List(BANDS) { 0f })
    val bands: StateFlow<List<Float>> = _bands.asStateFlow()

    private val _trace = MutableStateFlow(List(TRACE_POINTS) { 0f })
    val trace: StateFlow<List<Float>> = _trace.asStateFlow()

    private val _level = MutableStateFlow(0f)
    val level: StateFlow<Float> = _level.asStateFlow()

    var onFrame: ((level: Float, transient: Boolean) -> Unit)? = null

    private var scope: CoroutineScope? = null
    private var job: Job? = null
    private var recorder: AudioRecord? = null

    private val wavLock = Any()
    private var wavFile: RandomAccessFile? = null
    private var wavBytes: Long = 0

    private var smoothedLevel = 0f
    private var lastTransientAt = 0L
    private var traceBuffer = FloatArray(TRACE_POINTS)
    private var traceCursor = 0
    private var lastEmitAt = 0L

    @SuppressLint("MissingPermission")
    fun start() {
        if (job != null) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val newScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        scope = newScope
        job = newScope.launch { captureLoop() }
    }

    fun stop() {
        job?.cancel()
        job = null
        scope?.cancel()
        scope = null
        val rec = recorder
        recorder = null
        runCatching { rec?.stop() }
        runCatching { rec?.release() }
        endWav()
        _bands.value = List(BANDS) { 0f }
        _trace.value = List(TRACE_POINTS) { 0f }
        _level.value = 0f
        smoothedLevel = 0f
    }

    fun beginWav(file: File): Boolean = synchronized(wavLock) {
        closeWavLocked()
        try {
            file.parentFile?.mkdirs()
            val raf = RandomAccessFile(file, "rw")
            raf.setLength(0)
            raf.write(wavHeader(0))
            wavFile = raf
            wavBytes = 0
            true
        } catch (t: Throwable) {
            wavFile = null
            false
        }
    }

    fun endWav() = synchronized(wavLock) {
        closeWavLocked()
    }

    private fun closeWavLocked() {
        val raf = wavFile ?: return
        try {
            raf.seek(4)
            raf.write(intLe((36 + wavBytes).toInt()))
            raf.seek(40)
            raf.write(intLe(wavBytes.toInt()))
            raf.close()
        } catch (_: Throwable) {
        }
        wavFile = null
        wavBytes = 0
    }

    @SuppressLint("MissingPermission")
    private suspend fun captureLoop() {
        val sampleRate = 44_100
        val minBuffer = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = if (minBuffer > 0) maxOf(minBuffer, FFT_SIZE * 4) else FFT_SIZE * 4
        val record = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )
        } catch (_: Throwable) {
            null
        }
        if (record == null || record.state != AudioRecord.STATE_INITIALIZED) {
            record?.release()
            return
        }
        recorder = record
        try {
            record.startRecording()
        } catch (_: Throwable) {
            record.release()
            recorder = null
            return
        }

        val samples = ShortArray(FFT_SIZE)
        val wave = FloatArray(FFT_SIZE)
        val spectrum = SpectrumAnalyser(sampleRate, FFT_SIZE)
        val pcmBytes = ByteArray(FFT_SIZE * 2)

        while (currentCoroutineContext().isActive) {
            val read = try {
                record.read(samples, 0, samples.size)
            } catch (_: Throwable) {
                -1
            }
            if (read <= 0) {
                delay(20)
                continue
            }

            // ---- WAV tee ----
            synchronized(wavLock) {
                val raf = wavFile
                if (raf != null) {
                    var b = 0
                    for (i in 0 until read) {
                        val v = samples[i].toInt()
                        pcmBytes[b++] = (v and 0xFF).toByte()
                        pcmBytes[b++] = ((v shr 8) and 0xFF).toByte()
                    }
                    try {
                        raf.write(pcmBytes, 0, read * 2)
                        wavBytes += read * 2
                    } catch (_: Throwable) {
                        closeWavLocked()
                    }
                }
            }

            // ---- Level ----
            var sumSquares = 0.0
            for (i in 0 until read) {
                val v = samples[i] / 32768f
                wave[i] = v
                sumSquares += (v * v).toDouble()
            }
            if (read < FFT_SIZE) {
                for (i in read until FFT_SIZE) wave[i] = 0f
            }
            val rms = sqrt(sumSquares / read).toFloat()
            val db = 20f * log10(rms.coerceAtLeast(1e-6f))
            val level = ((db + 62f) / 50f).coerceIn(0f, 1f)

            val now = System.currentTimeMillis()
            val transient = level > 0.26f && level - smoothedLevel > 0.17f && now - lastTransientAt > 900
            if (transient) lastTransientAt = now
            smoothedLevel = smoothedLevel * 0.62f + level * 0.38f

            // ---- Spectrum ----
            val rawBands = spectrum.bands(wave, read)
            val current = _bands.value
            val next = List(BANDS) { i ->
                val attack = if (rawBands[i] > current[i]) 0.62f else 0.24f
                (current[i] + (rawBands[i] - current[i]) * attack).coerceIn(0f, 1f)
            }

            traceBuffer[traceCursor] = smoothedLevel
            traceCursor = (traceCursor + 1) % TRACE_POINTS

            if (now - lastEmitAt > 45) {
                lastEmitAt = now
                _bands.value = next
                _level.value = smoothedLevel
                _trace.value = orderedTrace()
                onFrame?.invoke(smoothedLevel, transient)
            }
            delay(8)
        }

        runCatching { record.stop() }
        runCatching { record.release() }
    }

    private fun orderedTrace(): List<Float> {
        val out = ArrayList<Float>(TRACE_POINTS)
        for (i in 0 until TRACE_POINTS) {
            out.add(traceBuffer[(traceCursor + i) % TRACE_POINTS])
        }
        return out
    }

    private fun wavHeader(dataBytes: Int): ByteArray {
        val sampleRate = 44_100
        return ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray())
            putInt(36 + dataBytes)
            put("WAVE".toByteArray())
            put("fmt ".toByteArray())
            putInt(16)
            putShort(1)
            putShort(1)
            putInt(sampleRate)
            putInt(sampleRate * 2)
            putShort(2)
            putShort(16)
            put("data".toByteArray())
            putInt(dataBytes)
        }.array()
    }

    private fun intLe(value: Int): ByteArray =
        ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()

    companion object {
        const val BANDS = 16
        const val TRACE_POINTS = 64
        private const val FFT_SIZE = 1024
    }
}

/** Radix-2 FFT plus logarithmic band mapping for the 16 channel display. */
private class SpectrumAnalyser(
    private val sampleRate: Int,
    private val fftSize: Int
) {
    private val window = FloatArray(fftSize) { i ->
        (0.5 - 0.5 * cos(2.0 * PI * i / (fftSize - 1))).toFloat()
    }
    private val re = FloatArray(fftSize)
    private val im = FloatArray(fftSize)
    private val mags = FloatArray(fftSize / 2)
    private val edges = FloatArray(AudioEngine.BANDS + 1) { i ->
        60f * Math.pow((11_500.0 / 60.0), i.toDouble() / AudioEngine.BANDS).toFloat()
    }

    fun bands(samples: FloatArray, count: Int): FloatArray {
        for (i in 0 until fftSize) {
            val v = if (i < count) samples[i] * window[i] else 0f
            re[i] = v
            im[i] = 0f
        }
        transform()
        for (i in 0 until fftSize / 2) {
            val amplitude = 2f * sqrt(re[i] * re[i] + im[i] * im[i]) / fftSize
            mags[i] = amplitude
        }
        val output = FloatArray(AudioEngine.BANDS)
        val binHz = sampleRate.toFloat() / fftSize
        for (band in 0 until AudioEngine.BANDS) {
            val lowBin = (edges[band] / binHz).toInt().coerceIn(0, mags.size - 1)
            val highBin = (edges[band + 1] / binHz).toInt().coerceIn(lowBin + 1, mags.size)
            var peak = 0f
            for (i in lowBin until highBin) {
                if (mags[i] > peak) peak = mags[i]
            }
            val db = 20f * log10(peak.coerceAtLeast(1e-7f))
            output[band] = ((db + 82f) / 74f).coerceIn(0f, 1f)
        }
        return output
    }

    private fun transform() {
        val n = fftSize
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit
            if (i < j) {
                val tr = re[i]; re[i] = re[j]; re[j] = tr
                val ti = im[i]; im[i] = im[j]; im[j] = ti
            }
        }
        var len = 2
        while (len <= n) {
            val angle = -2.0 * PI / len
            val wLenReal = cos(angle).toFloat()
            val wLenImag = sin(angle).toFloat()
            var i = 0
            while (i < n) {
                var wReal = 1f
                var wImag = 0f
                for (k in 0 until len / 2) {
                    val uReal = re[i + k]
                    val uImag = im[i + k]
                    val vReal = re[i + k + len / 2] * wReal - im[i + k + len / 2] * wImag
                    val vImag = re[i + k + len / 2] * wImag + im[i + k + len / 2] * wReal
                    re[i + k] = uReal + vReal
                    im[i + k] = uImag + vImag
                    re[i + k + len / 2] = uReal - vReal
                    im[i + k + len / 2] = uImag - vImag
                    val nextReal = wReal * wLenReal - wImag * wLenImag
                    wImag = wReal * wLenImag + wImag * wLenReal
                    wReal = nextReal
                }
                i += len
            }
            len = len shl 1
        }
    }
}
