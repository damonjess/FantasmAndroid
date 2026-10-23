package com.example.fantasmandroid.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fantasmandroid.FantasmController
import com.example.fantasmandroid.ItcDictionary
import com.example.fantasmandroid.ui.theme.AccentCyan
import com.example.fantasmandroid.ui.theme.AccentGreen
import com.example.fantasmandroid.ui.theme.FantasmDisplay
import com.example.fantasmandroid.ui.theme.HairlineDim
import com.example.fantasmandroid.ui.theme.IceWhite
import com.example.fantasmandroid.ui.theme.Ink
import com.example.fantasmandroid.ui.theme.SignalGreen
import com.example.fantasmandroid.ui.theme.TextDim
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    controller: FantasmController,
    onBack: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenLegal: () -> Unit
) {
    val context = LocalContext.current
    val settings by controller.settings.state.collectAsState()
    val emf by controller.sensors.emfLevel.collectAsState()
    val current by controller.itc.current.collectAsState()

    Column(Modifier.fillMaxSize().background(Ink)) {
        FantasmTopBar(title = "Settings", onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(18.dp))
            Text(
                text = "FANTASM ANDROID",
                color = IceWhite,
                fontFamily = FantasmDisplay,
                fontSize = 22.sp,
                letterSpacing = 4.sp
            )
            Text(
                text = "PARANORMAL RESEARCH DEVICE",
                color = AccentGreen.copy(alpha = 0.7f),
                fontSize = 9.sp,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Sensor configuration, ITC voice and investigation preferences.",
                color = TextDim,
                fontSize = 11.sp
            )

            SectionHeader("ITC SYSTEM")
            SwitchSetting(
                title = "Spirit Vox",
                subtitle = "Speak ITC responses aloud with the device voice",
                checked = settings.voxEnabled,
                onChange = controller.settings::setVox
            )
            SwitchSetting(
                title = "Whisper Mode",
                subtitle = "Soft, close-microphone delivery",
                checked = settings.whisperMode,
                onChange = controller.settings::setWhisper
            )
            SwitchSetting(
                title = "Enhanced Vox",
                subtitle = "Alternating low-register voice for deeper delivery",
                checked = settings.enhancedVox,
                onChange = controller.settings::setEnhancedVox
            )
            SettingRow(
                title = "Sensitivity",
                subtitle = "How readily the system responds to anomalies",
                trailing = {
                    Text(
                        text = "${(settings.itcSensitivity * 100).roundToInt()}%",
                        color = AccentCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            )
            Slider(
                value = settings.itcSensitivity,
                onValueChange = controller.settings::setItcSensitivity,
                valueRange = 0.1f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = SignalGreen,
                    activeTrackColor = SignalGreen,
                    inactiveTrackColor = Color(0xFF2A2A2A)
                ),
                modifier = Modifier.fillMaxWidth().height(32.dp)
            )
            Row(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                Text("LOW", color = TextDim, fontSize = 8.sp, modifier = Modifier.weight(1f))
                Text("HIGH", color = TextDim, fontSize = 8.sp)
            }
            AudioActionRow(
                title = "Trigger Response Now",
                subtitle = current?.let { "LAST: ${it.phrase}" } ?: "No responses in this session yet",
                trailing = "FORCE"
            ) { controller.itc.trigger("MANUAL") }
            SettingRow(
                title = "ITC Dictionary",
                subtitle = "Curated words and phrases available to the engine",
                trailing = {
                    Text(
                        text = String.format("%,d", ItcDictionary.size),
                        color = AccentCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            )

            SectionHeader("OPTICS")
            SwitchSetting(
                title = "SLS Vision",
                subtitle = "AI skeleton tracking overlaid on the live camera",
                checked = settings.slsEnabled,
                onChange = controller.settings::setSls
            )
            SwitchSetting(
                title = "LiDAR Depth Render",
                subtitle = "False-colour depth map. Android uses AI depth segmentation in place of iPhone LiDAR hardware.",
                checked = settings.depthRenderEnabled,
                onChange = controller.settings::setDepthRender
            )
            SwitchSetting(
                title = "Twilight Vision",
                subtitle = "Green night-vision enhancement with low-light grain",
                checked = settings.twilightEnabled,
                onChange = controller.settings::setTwilight
            )
            SwitchSetting(
                title = "Front Camera",
                subtitle = "Use the selfie camera for the viewfinder",
                checked = settings.frontCamera,
                onChange = controller.settings::setFrontCamera
            )

            SectionHeader("SYSTEM")
            SwitchSetting(
                title = "Haptic Feedback",
                subtitle = "Vibrate when a new ITC message arrives",
                checked = settings.hapticsEnabled,
                onChange = controller.settings::setHaptics
            )
            SwitchSetting(
                title = "Keep Screen Awake",
                subtitle = "Prevent the display sleeping during a session",
                checked = settings.keepScreenOn,
                onChange = controller.settings::setKeepScreenOn
            )
            SettingRow(
                title = "Recalibrate EMF",
                subtitle = "Re-zero the magnetic baseline for this room (now ${(emf * 100).roundToInt()}%)",
                trailing = { Glyph(Glyph.ChevronRight, size = 15.dp, tint = TextDim) },
                onClick = { controller.sensors.calibrate() }
            )

            SectionHeader("SUPPORT")
            SettingRow(
                title = "How To Use",
                subtitle = "Step-by-step investigation guidance",
                trailing = { Glyph(Glyph.ChevronRight, size = 15.dp, tint = TextDim) },
                onClick = onOpenHelp
            )
            SettingRow(
                title = "Contact Support",
                subtitle = "JedaSoftwareSystemsHelp@gmail.com",
                trailing = { Glyph(Glyph.Share, size = 15.dp, tint = TextDim) },
                onClick = { openSupportEmail(context) }
            )
            SettingRow(
                title = "Legal & Disclaimer",
                subtitle = "Entertainment use, privacy and attribution",
                trailing = { Glyph(Glyph.ChevronRight, size = 15.dp, tint = TextDim) },
                onClick = onOpenLegal
            )

            Spacer(Modifier.height(26.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(HairlineDim))
            Spacer(Modifier.height(12.dp))
            Text(
                text = "FANTASM FOR ANDROID 1.0\nIndependent implementation. Entertainment purposes only.",
                color = TextDim.copy(alpha = 0.55f),
                fontSize = 9.sp,
                lineHeight = 14.sp
            )
            Spacer(Modifier.height(34.dp))
        }
    }
}

@Composable
private fun SwitchSetting(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    SettingRow(
        title = title,
        subtitle = subtitle,
        trailing = {
            FantasmSwitch(checked = checked, onChange = onChange)
        }
    )
}

@Composable
private fun AudioActionRow(
    title: String,
    subtitle: String,
    trailing: String,
    onClick: () -> Unit
) {
    SettingRow(
        title = title,
        subtitle = subtitle,
        trailing = {
            Text(
                text = trailing,
                color = AccentGreen,
                fontSize = 9.sp,
                letterSpacing = 1.4.sp,
                modifier = Modifier
                    .border(1.dp, AccentGreen.copy(alpha = 0.45f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        },
        onClick = onClick
    )
}
