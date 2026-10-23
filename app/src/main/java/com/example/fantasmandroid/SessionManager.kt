package com.example.fantasmandroid

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.max

/**
 * Owns the record button. A single session captures three artefacts at once:
 * the MP4 from the camera, the EVP WAV from the microphone and the ITC
 * transcript with timestamps and causes.
 */
class SessionManager(
    private val store: SessionStore,
    private val itc: ItcEngine,
    private val audio: AudioEngine,
    private val video: SessionVideoRecorder,
    private val emfProvider: () -> Float
) {
    data class ActiveSession(
        val id: String,
        val startedAt: Long,
        val dir: File,
        val thumbFile: File,
        val videoFile: File,
        val audioFile: File,
        val videoStarted: Boolean
    )

    private val _active = MutableStateFlow<ActiveSession?>(null)
    val active: StateFlow<ActiveSession?> = _active.asStateFlow()

    var camera: CameraHandle? = null

    private var startHistoryIndex = 0
    private var peakEmf = 0f
    private var scope: CoroutineScope? = null
    private var peakJob: Job? = null

    fun start(): Boolean {
        if (_active.value != null) return false
        val id = SessionStore.newId()
        val dir = store.newSessionDir(id)
        val thumbFile = File(dir, "thumb.jpg")
        val videoFile = File(dir, "session.mp4")
        val audioFile = File(dir, "session.wav")

        startHistoryIndex = itc.history.value.size
        peakEmf = emfProvider()

        val videoStarted = camera?.startVideo(videoFile) ?: false
        audio.beginWav(audioFile)
        camera?.captureThumbnail(thumbFile)

        _active.value = ActiveSession(
            id = id,
            startedAt = System.currentTimeMillis(),
            dir = dir,
            thumbFile = thumbFile,
            videoFile = videoFile,
            audioFile = audioFile,
            videoStarted = videoStarted
        )

        val monitorScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        scope = monitorScope
        peakJob = monitorScope.launch {
            while (isActive) {
                peakEmf = max(peakEmf, emfProvider())
                delay(200)
            }
        }
        return true
    }

    suspend fun stop(): Session? {
        val active = _active.value ?: return null
        _active.value = null

        video.stop()
        audio.endWav()
        peakJob?.cancel()
        scope?.cancel()
        peakJob = null
        scope = null

        // Let CameraX finalise the MP4 before reading its size.
        delay(1_100)

        val words = itc.history.value
            .drop(startHistoryIndex)
            .map { SessionWord(it.phrase, it.timestamp, it.cause, it.emf) }

        val session = Session(
            id = active.id,
            startedAt = active.startedAt,
            durationMs = System.currentTimeMillis() - active.startedAt,
            peakEmf = peakEmf,
            transcript = words,
            thumbFile = active.thumbFile.takeIf { it.exists() && it.length() > 0 },
            videoFile = active.videoFile.takeIf { it.exists() && it.length() > 0 },
            audioFile = active.audioFile.takeIf { it.exists() && it.length() > 0 },
            dir = active.dir
        )
        store.save(session)
        return session
    }
}
