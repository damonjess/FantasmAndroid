package com.example.fantasmandroid

import android.content.Context

class FantasmController(context: Context) {

    val settings = SettingsStore(context)
    val sensors = ParanormalSensorEngine(context)
    val audio = AudioEngine(context)
    val itc = ItcEngine(context, settings)
    val store = SessionStore(context)
    val videoRecorder = SessionVideoRecorder(context)
    val sessionManager = SessionManager(
        store = store,
        itc = itc,
        audio = audio,
        video = videoRecorder,
        emfProvider = { sensors.emfLevel.value }
    )

    private var running = false

    init {
        sensors.onReading = { level, spike -> itc.submitEmf(level, spike) }
        audio.onFrame = { level, transient -> itc.submitAudio(level, transient) }
    }

    fun start() {
        if (running) return
        running = true
        sensors.start()
        audio.start()
        itc.start()
    }

    fun stop() {
        if (!running) return
        running = false
        sensors.stop()
        audio.stop()
        itc.stop()
    }

    fun shutdown() {
        stop()
        itc.shutdown()
    }
}
