package com.example.fantasmandroid.ui

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fantasmandroid.CameraHandle
import com.example.fantasmandroid.CameraLayer
import com.example.fantasmandroid.FantasmController
import com.example.fantasmandroid.FantasmSettings
import com.example.fantasmandroid.ui.theme.AccentCyan
import com.example.fantasmandroid.ui.theme.ColdBlue
import com.example.fantasmandroid.ui.theme.HairlineDim
import com.example.fantasmandroid.ui.theme.IceWhite
import com.example.fantasmandroid.ui.theme.Ink
import com.example.fantasmandroid.ui.theme.PanelBlack
import com.example.fantasmandroid.ui.theme.SignalGreen
import com.example.fantasmandroid.ui.theme.TextDim
import com.example.fantasmandroid.ui.theme.WarmRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DeviceScreen(
    controller: FantasmController,
    onNavigate: (Screen) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    val settings by controller.settings.state.collectAsState()
    val emf by controller.sensors.emfLevel.collectAsState()
    val emfHistory by controller.sensors.emfHistory.collectAsState()
    val geo by controller.sensors.geoHistory.collectAsState()
    val bands by controller.audio.bands.collectAsState()
    val trace by controller.audio.trace.collectAsState()
    val itcEvent by controller.itc.current.collectAsState()
    val history by controller.itc.history.collectAsState()
    val active by controller.sessionManager.active.collectAsState()

    val cameraHandle = remember { CameraHandle() }
    DisposableEffect(cameraHandle) {
        controller.sessionManager.camera = cameraHandle
        onDispose {
            if (controller.sessionManager.camera === cameraHandle) {
                controller.sessionManager.camera = null
            }
        }
    }

    DisposableEffect(settings.keepScreenOn) {
        view.keepScreenOn = settings.keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    var toast by remember { mutableStateOf<String?>(null) }
    var toastToken by remember { mutableIntStateOf(0) }
    fun showToast(message: String) {
        toast = message
        toastToken++
    }
    LaunchedEffect(toastToken) {
        if (toast != null) {
            delay(2_600)
            toast = null
        }
    }

    var elapsedNow by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(active?.id) {
        while (true) {
            elapsedNow = System.currentTimeMillis()
            if (active == null) break
            delay(500)
        }
    }

    val vibrator = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(VibratorManager::class.java)
            manager?.defaultVibrator
        } else {
            context.getSystemService(Vibrator::class.java)
        }
    }
    LaunchedEffect(itcEvent?.id) {
        val event = itcEvent ?: return@LaunchedEffect
        if (settings.hapticsEnabled) {
            runCatching {
                vibrator?.vibrate(VibrationEffect.createOneShot(30L, 90))
            }
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "deck")
    val pulse by pulseTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(850), RepeatMode.Reverse),
        label = "pulse"
    )

    val batteryTemp = rememberBatteryTemperature(context)
    var showTemp by remember { mutableStateOf(false) }
    LaunchedEffect(showTemp, batteryTemp) {
        if (showTemp) {
            delay(2_800)
            showTemp = false
        }
    }

    val topInset = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding()

    Box(Modifier.fillMaxSize().background(Ink)) {
        CameraLayer(
            settings = settings,
            handle = cameraHandle,
            videoRecorder = controller.videoRecorder,
            modifier = Modifier.fillMaxSize()
        )

        Column(Modifier.fillMaxSize()) {
            DeviceHeader(
                level = emf,
                topInset = topInset,
                temperature = batteryTemp,
                showTemperature = showTemp,
                onTemperatureClick = { showTemp = !showTemp }
            )

            ItcBanner(
                text = itcEvent?.phrase.orEmpty(),
                modifier = Modifier.padding(horizontal = 10.dp),
                onClick = { controller.itc.trigger("MANUAL") }
            )

            Box(Modifier.weight(1f).fillMaxWidth()) {
                FlipButton(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 14.dp, bottom = 26.dp)
                ) {
                    controller.settings.setFrontCamera(!settings.frontCamera)
                }

                active?.let { session ->
                    RecordingPill(
                        elapsedMs = elapsedNow - session.startedAt,
                        pulse = pulse,
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp)
                    )
                }

                toast?.let { message ->
                    Text(
                        text = message,
                        color = IceWhite,
                        fontSize = 10.sp,
                        letterSpacing = 1.2.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 6.dp)
                            .background(Color(0xE6000000), RoundedCornerShape(2.dp))
                            .border(1.dp, HairlineDim, RoundedCornerShape(2.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            InstrumentPanel(
                emfHistory = emfHistory,
                bands = bands,
                trace = trace,
                geo = geo
            )

            HudToolbar(
                onGlyph = { glyph ->
                    if (active != null) {
                        showToast("STOP THE SESSION FIRST")
                    } else {
                        when (glyph) {
                            Glyph.List -> onNavigate(Screen.Matrix)
                            Glyph.Film -> onNavigate(Screen.Recordings)
                            Glyph.Mail -> onNavigate(Screen.WordLog)
                            Glyph.Gear -> onNavigate(Screen.Settings)
                            Glyph.Help -> onNavigate(Screen.Help)
                            Glyph.Scale -> onNavigate(Screen.Legal)
                            Glyph.Camera -> onNavigate(Screen.Gallery)
                            else -> Unit
                        }
                    }
                }
            )

            ControlDeck(
                settings = settings,
                recording = active != null,
                pulse = pulse,
                bottomInset = bottomInset,
                onToggleSls = { controller.settings.setSls(!settings.slsEnabled) },
                onToggleDepth = { controller.settings.setDepthRender(!settings.depthRenderEnabled) },
                onToggleTwilight = { controller.settings.setTwilight(!settings.twilightEnabled) },
                onStop = {
                    if (active == null) {
                        showToast("NO ACTIVE SESSION")
                    } else {
                        scope.launch {
                            toast = "SAVING SESSION..."
                            toastToken++
                            val session = controller.sessionManager.stop()
                            controller.store.reload()
                            showToast(if (session != null) "SESSION ${session.id} SAVED" else "SESSION ENDED")
                        }
                    }
                },
                onStart = {
                    if (active != null) {
                        showToast("RECORDING IN PROGRESS")
                    } else if (controller.sessionManager.start()) {
                        showToast("FULL SESSION RECORD")
                    } else {
                        showToast("UNABLE TO START SESSION")
                    }
                }
            )
        }
    }
}

@Composable
private fun DeviceHeader(
    level: Float,
    topInset: androidx.compose.ui.unit.Dp,
    temperature: Float,
    showTemperature: Boolean,
    onTemperatureClick: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(top = topInset)
    ) {
        Box(Modifier.fillMaxWidth().height(46.dp)) {
            Text(
                text = "FANTASM",
                color = IceWhite,
                fontFamily = com.example.fantasmandroid.ui.theme.FantasmDisplay,
                fontSize = 24.sp,
                letterSpacing = 2.6.sp,
                modifier = Modifier.align(Alignment.Center)
            )
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onTemperatureClick
                    )
                    .padding(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showTemperature && temperature > 0f) {
                    Text(
                        text = String.format(java.util.Locale.US, "%.1f°", temperature),
                        color = AccentCyan,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
                Glyph(
                    Glyph.Thermometer,
                    size = 19.dp,
                    tint = AccentCyan,
                    strokeWidth = 1.2.dp
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(HairlineDim))
        EmfBar(
            level = level,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun FlipButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Color(0x59000000))
            .border(1.dp, Color(0xB3FFFFFF), CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Glyph(Glyph.Flip, size = 20.dp, tint = Color.White, strokeWidth = 1.3.dp)
    }
}

@Composable
private fun RecordingPill(elapsedMs: Long, pulse: Float, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(Color(0xB3000000), RoundedCornerShape(13.dp))
            .border(1.dp, WarmRed.copy(alpha = 0.7f), RoundedCornerShape(13.dp))
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(WarmRed.copy(alpha = 0.45f + pulse * 0.55f))
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "REC ${formatDuration(elapsedMs)}",
            color = IceWhite,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun InstrumentPanel(
    emfHistory: List<Float>,
    bands: List<Float>,
    trace: List<Float>,
    geo: List<Float>
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color(0x4D000000))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        InstrumentModule(Glyph.Magnet, height = 50.dp) {
            Column(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .padding(top = 3.dp)
                        .width(23.dp)
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(ColdBlue)
                )
                EmfDotMatrix(emfHistory, Modifier.fillMaxWidth().weight(1f))
            }
        }
        InstrumentModule(Glyph.Mic, height = 104.dp) {
            Column(Modifier.fillMaxSize()) {
                AudioSpectrum(bands, Modifier.fillMaxWidth().weight(1f))
                Spacer(Modifier.height(3.dp))
                AudioTrace(trace, Modifier.fillMaxWidth().height(13.dp))
            }
        }
        InstrumentModule(Glyph.Globe, height = 46.dp) {
            GeoWave(geo, Modifier.fillMaxSize().padding(vertical = 5.dp))
        }
    }
}

@Composable
private fun ControlDeck(
    settings: FantasmSettings,
    recording: Boolean,
    pulse: Float,
    bottomInset: androidx.compose.ui.unit.Dp,
    onToggleSls: () -> Unit,
    onToggleDepth: () -> Unit,
    onToggleTwilight: () -> Unit,
    onStop: () -> Unit,
    onStart: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PanelBlack)
            .padding(start = 10.dp, end = 10.dp, top = 9.dp, bottom = 7.dp + bottomInset),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RoundDeckButton(
            active = recording,
            enabled = true,
            pulse = pulse,
            glowColor = WarmRed,
            onClick = onStop
        ) {
            Box(
                Modifier
                    .size(if (recording) (18 + pulse * 4).dp else 19.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (recording) WarmRed else WarmRed.copy(alpha = 0.55f))
            )
        }

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RockerToggle(
                label = "LIDAR",
                enabled = settings.depthRenderEnabled,
                tintTop = Color(0xFF37204F),
                tintBottom = Color(0xFF120A1D),
                onToggle = onToggleDepth
            )
            RockerToggle(
                label = "SLS",
                enabled = settings.slsEnabled,
                tintTop = Color(0xFF17354F),
                tintBottom = Color(0xFF07131D),
                onToggle = onToggleSls
            )
            RockerToggle(
                label = "TWILIGHT",
                enabled = settings.twilightEnabled,
                tintTop = Color(0xFF1A3D24),
                tintBottom = Color(0xFF07170D),
                onToggle = onToggleTwilight
            )
        }

        RoundDeckButton(
            active = false,
            enabled = !recording,
            pulse = pulse,
            glowColor = SignalGreen,
            onClick = onStart
        ) {
            Box(
                Modifier
                    .size(37.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFFA8FFB6), SignalGreen, Color(0xFF12C936))
                        )
                    )
                    .border(1.dp, Color(0xFF0A7A26), CircleShape)
            )
        }
    }
}

@Composable
private fun rememberBatteryTemperature(context: Context): Float {
    var temperature by remember { mutableStateOf(0f) }
    LaunchedEffect(context) {
        while (true) {
            val intent = runCatching {
                context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            }.getOrNull()
            val tenths = intent?.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
            if (tenths > 0) temperature = tenths / 10f
            delay(30_000)
        }
    }
    return temperature
}

fun openSupportEmail(context: Context) {
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:JedaSoftwareSystemsHelp@gmail.com")).apply {
        putExtra(Intent.EXTRA_SUBJECT, "Fantasm Android feedback")
    }
    runCatching { context.startActivity(intent) }
}
