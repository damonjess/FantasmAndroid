package com.example.fantasmandroid

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Everything the device screen toggles or the settings panel exposes.
 * Persisted in SharedPreferences so the rig comes back the way it was left.
 */
data class FantasmSettings(
    val voxEnabled: Boolean = false,
    val whisperMode: Boolean = false,
    val enhancedVox: Boolean = false,
    val slsEnabled: Boolean = false,
    val depthRenderEnabled: Boolean = false,
    val twilightEnabled: Boolean = false,
    val frontCamera: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val keepScreenOn: Boolean = true,
    val itcSensitivity: Float = 0.55f
)

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("fantasm_settings", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(load())
    val state: StateFlow<FantasmSettings> = _state.asStateFlow()

    private fun load(): FantasmSettings {
        val d = FantasmSettings()
        return FantasmSettings(
            voxEnabled = prefs.getBoolean("vox", d.voxEnabled),
            whisperMode = prefs.getBoolean("whisper", d.whisperMode),
            enhancedVox = prefs.getBoolean("enhanced_vox", d.enhancedVox),
            slsEnabled = prefs.getBoolean("sls", d.slsEnabled),
            depthRenderEnabled = prefs.getBoolean("depth", d.depthRenderEnabled),
            twilightEnabled = prefs.getBoolean("twilight", d.twilightEnabled),
            frontCamera = prefs.getBoolean("front_camera", d.frontCamera),
            hapticsEnabled = prefs.getBoolean("haptics", d.hapticsEnabled),
            keepScreenOn = prefs.getBoolean("keep_screen_on", d.keepScreenOn),
            itcSensitivity = prefs.getFloat("itc_sensitivity", d.itcSensitivity)
        )
    }

    private fun update(transform: (FantasmSettings) -> FantasmSettings) {
        val next = transform(_state.value)
        _state.value = next
        prefs.edit()
            .putBoolean("vox", next.voxEnabled)
            .putBoolean("whisper", next.whisperMode)
            .putBoolean("enhanced_vox", next.enhancedVox)
            .putBoolean("sls", next.slsEnabled)
            .putBoolean("depth", next.depthRenderEnabled)
            .putBoolean("twilight", next.twilightEnabled)
            .putBoolean("front_camera", next.frontCamera)
            .putBoolean("haptics", next.hapticsEnabled)
            .putBoolean("keep_screen_on", next.keepScreenOn)
            .putFloat("itc_sensitivity", next.itcSensitivity)
            .apply()
    }

    fun setVox(enabled: Boolean) = update { it.copy(voxEnabled = enabled) }
    fun setWhisper(enabled: Boolean) = update { it.copy(whisperMode = enabled) }
    fun setEnhancedVox(enabled: Boolean) = update { it.copy(enhancedVox = enabled) }
    fun setSls(enabled: Boolean) = update { it.copy(slsEnabled = enabled) }
    fun setDepthRender(enabled: Boolean) = update { it.copy(depthRenderEnabled = enabled) }
    fun setTwilight(enabled: Boolean) = update { it.copy(twilightEnabled = enabled) }
    fun setFrontCamera(enabled: Boolean) = update { it.copy(frontCamera = enabled) }
    fun setHaptics(enabled: Boolean) = update { it.copy(hapticsEnabled = enabled) }
    fun setKeepScreenOn(enabled: Boolean) = update { it.copy(keepScreenOn = enabled) }
    fun setItcSensitivity(value: Float) = update { it.copy(itcSensitivity = value.coerceIn(0.1f, 1f)) }
}
