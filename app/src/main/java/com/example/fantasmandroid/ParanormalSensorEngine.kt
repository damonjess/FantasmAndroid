package com.example.fantasmandroid

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt

/**
 * Magnetometer (EMF) and accelerometer (geophone) pipeline for the device HUD.
 * Levels are normalised against a slowly adapting room baseline so the meters
 * respond to changes in the field rather than a fixed ambient value.
 */
class ParanormalSensorEngine(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    val hasMagnetometer: Boolean = magnetometer != null

    private val _emfLevel = MutableStateFlow(0.05f)
    val emfLevel: StateFlow<Float> = _emfLevel.asStateFlow()

    private val _emfHistory = MutableStateFlow(List(EMF_POINTS) { 0f })
    val emfHistory: StateFlow<List<Float>> = _emfHistory.asStateFlow()

    private val _geoHistory = MutableStateFlow(List(GEO_POINTS) { 0f })
    val geoHistory: StateFlow<List<Float>> = _geoHistory.asStateFlow()

    private val _geoEnergy = MutableStateFlow(0f)
    val geoEnergy: StateFlow<Float> = _geoEnergy.asStateFlow()

    /** Fired when the local magnetic field moves sharply away from baseline. */
    var onReading: ((level: Float, spike: Boolean) -> Unit)? = null

    private var baseline = -1f
    private var lastMagnitude = 42f
    private var smoothedLevel = 0.05f
    private var lastSpikeAt = 0L
    private var lastEmitAt = 0L
    private var historyEmitAt = 0L
    private var lastGeoEmitAt = 0L
    private var emfBuffer = ArrayDeque<Float>()
    private var geoBuffer = ArrayDeque<Float>()
    private val gravity = FloatArray(3)
    private var running = false

    init {
        repeat(EMF_POINTS) { emfBuffer.addLast(0f) }
        repeat(GEO_POINTS) { geoBuffer.addLast(0f) }
    }

    fun start() {
        if (running) return
        running = true
        magnetometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        running = false
        sensorManager.unregisterListener(this)
    }

    /** Re-zeroes the ambient magnetic baseline for the current room. */
    fun calibrate() {
        baseline = lastMagnitude
        smoothedLevel = 0.18f
        _emfLevel.value = smoothedLevel
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        when (event.sensor.type) {
            Sensor.TYPE_MAGNETIC_FIELD -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val magnitude = sqrt(x * x + y * y + z * z)
                if (magnitude <= 0f) return
                lastMagnitude = magnitude

                // First reading seeds the room baseline, then it adapts slowly.
                if (baseline < 0f) baseline = magnitude
                baseline += (magnitude - baseline) * 0.0035f
                val delta = magnitude - baseline
                val target = (0.18f + delta / 50f).coerceIn(0.03f, 1f)

                val now = System.currentTimeMillis()
                val spike = delta > 9f && now - lastSpikeAt > 1_100L
                if (spike) lastSpikeAt = now

                smoothedLevel = smoothedLevel * 0.72f + target * 0.28f
                if (now - lastEmitAt > 50) {
                    lastEmitAt = now
                    _emfLevel.value = smoothedLevel
                    onReading?.invoke(smoothedLevel, spike)
                }
                if (now - historyEmitAt > 90) {
                    historyEmitAt = now
                    emfBuffer.addLast(smoothedLevel)
                    while (emfBuffer.size > EMF_POINTS) emfBuffer.removeFirst()
                    _emfHistory.value = emfBuffer.toList()
                }
            }

            Sensor.TYPE_ACCELEROMETER -> {
                // High-pass the gravity component so the trace shows movement.
                for (i in 0..2) {
                    gravity[i] = gravity[i] * 0.9f + event.values[i] * 0.1f
                }
                val lx = event.values[0] - gravity[0]
                val ly = event.values[1] - gravity[1]
                val lz = event.values[2] - gravity[2]
                val magnitude = sqrt(lx * lx + ly * ly + lz * lz)
                val signed = (magnitude / 4.5f).coerceIn(0f, 1f) * (if (ly >= 0f) 1f else -1f)

                val now = System.currentTimeMillis()
                if (now - lastGeoEmitAt > 40) {
                    lastGeoEmitAt = now
                    geoBuffer.addLast(signed)
                    while (geoBuffer.size > GEO_POINTS) geoBuffer.removeFirst()
                    _geoHistory.value = geoBuffer.toList()
                    _geoEnergy.value = magnitude.coerceIn(0f, 1f)
                }
            }

            Sensor.TYPE_GYROSCOPE -> {
                // Rotational energy nudges the geophone trace when the device is moved.
                val spin = sqrt(
                    event.values[0] * event.values[0] +
                        event.values[1] * event.values[1] +
                        event.values[2] * event.values[2]
                )
                if (spin > 1.2f) {
                    _geoEnergy.value = (_geoEnergy.value * 0.8f + (spin / 8f).coerceAtMost(1f) * 0.2f)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private companion object {
        const val EMF_POINTS = 48
        const val GEO_POINTS = 72
    }
}
