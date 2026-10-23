package com.example.fantasmandroid

import android.content.Context
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class SessionWord(
    val phrase: String,
    val timestamp: Long,
    val cause: String,
    val emf: Float
)

data class Session(
    val id: String,
    val startedAt: Long,
    val durationMs: Long,
    val peakEmf: Float,
    val transcript: List<SessionWord>,
    val thumbFile: File?,
    val videoFile: File?,
    val audioFile: File?,
    val dir: File
) {
    val totalBytes: Long
        get() = listOfNotNull(thumbFile, videoFile, audioFile)
            .filter { it.exists() }
            .sumOf { it.length() }

    val hasItc: Boolean get() = transcript.isNotEmpty()
    val hasAudio: Boolean get() = audioFile != null
    val hasVideo: Boolean get() = videoFile != null
}

/** Discovers, reads and writes session folders in app-private storage. */
class SessionStore(context: Context) {

    private val root = File(context.filesDir, "sessions").apply { mkdirs() }

    private val _sessions = MutableStateFlow(loadFromDisk())
    val sessions: StateFlow<List<Session>> = _sessions.asStateFlow()

    fun newSessionDir(id: String): File =
        File(root, id).apply { mkdirs() }

    fun save(session: Session) {
        val json = JSONObject().apply {
            put("id", session.id)
            put("startedAt", session.startedAt)
            put("durationMs", session.durationMs)
            put("peakEmf", session.peakEmf.toDouble())
            put("thumb", session.thumbFile?.name ?: JSONObject.NULL)
            put("video", session.videoFile?.name ?: JSONObject.NULL)
            put("audio", session.audioFile?.name ?: JSONObject.NULL)
            val words = JSONArray()
            session.transcript.forEach { word ->
                words.put(
                    JSONObject().apply {
                        put("phrase", word.phrase)
                        put("t", word.timestamp)
                        put("cause", word.cause)
                        put("emf", word.emf.toDouble())
                    }
                )
            }
            put("itc", words)
        }
        runCatching {
            File(session.dir, "session.json").writeText(json.toString())
        }
        reload()
    }

    fun delete(session: Session) {
        runCatching { session.dir.deleteRecursively() }
        reload()
    }

    fun reload() {
        _sessions.value = loadFromDisk()
    }

    private fun loadFromDisk(): List<Session> {
        val folders = root.listFiles { file -> file.isDirectory } ?: return emptyList()
        return folders.mapNotNull { dir ->
            val meta = File(dir, "session.json")
            if (!meta.exists()) return@mapNotNull null
            runCatching {
                val json = JSONObject(meta.readText())
                val words = mutableListOf<SessionWord>()
                json.optJSONArray("itc")?.let { array ->
                    for (i in 0 until array.length()) {
                        val item = array.getJSONObject(i)
                        words.add(
                            SessionWord(
                                phrase = item.optString("phrase"),
                                timestamp = item.optLong("t"),
                                cause = item.optString("cause", "AMBIENT"),
                                emf = item.optDouble("emf", 0.0).toFloat()
                            )
                        )
                    }
                }
                fun optionalFile(key: String): File? {
                    val name = json.optString(key)
                    if (name.isBlank() || name == "null") return null
                    val file = File(dir, name)
                    return file.takeIf { it.exists() && it.length() > 0 }
                }
                Session(
                    id = json.optString("id", dir.name),
                    startedAt = json.optLong("startedAt"),
                    durationMs = json.optLong("durationMs"),
                    peakEmf = json.optDouble("peakEmf", 0.0).toFloat(),
                    transcript = words,
                    thumbFile = optionalFile("thumb"),
                    videoFile = optionalFile("video"),
                    audioFile = optionalFile("audio"),
                    dir = dir
                )
            }.getOrNull()
        }.sortedByDescending { it.startedAt }
    }

    companion object {
        fun newId(): String = UUID.randomUUID().toString().replace("-", "").take(8).uppercase()
    }
}

/** Records the MP4 half of a session without taking the microphone away from analysis. */
class SessionVideoRecorder(private val context: Context) {

    val recorder: Recorder = Recorder.Builder()
        .setQualitySelector(
            QualitySelector.from(
                Quality.FHD,
                FallbackStrategy.lowerQualityOrHigherThan(Quality.HD)
            )
        )
        .build()

    private var recording: Recording? = null

    fun start(file: File): Boolean {
        if (recording != null) return false
        return try {
            file.parentFile?.mkdirs()
            val options = FileOutputOptions.Builder(file).build()
            val pending = recorder.prepareRecording(context, options)
            recording = pending.start(ContextCompat.getMainExecutor(context)) { event ->
                if (event is VideoRecordEvent.Finalize) {
                    recording = null
                    if (event.hasError()) {
                        runCatching { file.delete() }
                    }
                }
            }
            true
        } catch (_: Throwable) {
            runCatching { file.delete() }
            false
        }
    }

    fun stop() {
        runCatching { recording?.stop() }
        recording = null
    }
}
