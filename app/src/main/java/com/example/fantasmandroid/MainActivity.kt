package com.example.fantasmandroid

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    private lateinit var sensorEngine: ParanormalSensorEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorEngine = ParanormalSensorEngine(this)

        setContent {
            var permissionsGranted by remember { mutableStateOf(false) }
            val launcher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { results ->
                permissionsGranted = results.values.all { it }
                if (permissionsGranted) sensorEngine.start(enableAudio = true)
            }

            LaunchedEffect(Unit) {
                launcher.launch(
                    arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                )
            }

            FantasmScreen(sensorEngine = sensorEngine)
        }
    }

    override fun onResume() {
        super.onResume()
        // Automatically resume sensors and mic when the app is focused
        sensorEngine.start(enableAudio = true)
    }

    override fun onPause() {
        super.onPause()
        sensorEngine.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorEngine.stop()
    }
}

@Composable
fun FantasmScreen(sensorEngine: ParanormalSensorEngine) {
    val emfHistory by sensorEngine.emfHistory.collectAsState()
    val geoHistory by sensorEngine.geoHistory.collectAsState()
    val audioBands by sensorEngine.audioBands.collectAsState()
    val itcPhrase by sensorEngine.itcPhrase.collectAsState()

    var voxEnabled by remember { mutableStateOf(false) }
    var slsEnabled by remember { mutableStateOf(true) }
    var twilightEnabled by remember { mutableStateOf(false) }
    var flipEnabled by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // 1. Live Camera + SLS Stick-Figure Layer
        SlsCameraPreview(
            slsEnabled = slsEnabled,
            twilightEnabled = twilightEnabled,
            flipCamera = flipEnabled
        )

        // 2. HUD Overlay Column
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Header & ITC Word Banner
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x99222222))
                    .padding(top = 12.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "FANTASM",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontFamily = FontFamily.Serif,
                    letterSpacing = 4.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = itcPhrase,
                    color = Color(0xFFE0E0E0),
                    fontSize = 28.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 6.sp
                )
            }

            // Bottom Instrument Panel & Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xCC383430))
                    .navigationBarsPadding() // <-- Prevents buttons from sliding under system nav keys
                    .padding(8.dp)
            ) {
                // EMF Bar Graph (Magnet)
                InstrumentRow(iconLabel = "🧲") {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val barWidth = size.width / emfHistory.size
                        emfHistory.forEachIndexed { idx, value ->
                            val barHeight = value * size.height
                            drawRect(
                                color = if (value > 0.6f) Color.Red else Color(0xFF00FF44),
                                topLeft = Offset(idx * barWidth, size.height - barHeight),
                                size = Size(barWidth * 0.7f, barHeight)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Audio VU LED Matrix (Microphone)
                InstrumentRow(iconLabel = "🎙️", height = 90.dp) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cols = audioBands.size
                        val rows = 8
                        val cellW = size.width / cols
                        val cellH = size.height / rows

                        audioBands.forEachIndexed { col, level ->
                            val activeRows = (level * rows).toInt()
                            for (row in 0 until activeRows) {
                                val color = when {
                                    row >= 6 -> Color.Red
                                    row >= 4 -> Color.Yellow
                                    else -> Color(0xFF00FF00)
                                }
                                drawRect(
                                    color = color,
                                    topLeft = Offset(col * cellW + 2f, size.height - (row + 1) * cellH + 2f),
                                    size = Size(cellW - 4f, cellH - 4f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Geo Seismograph Waveform (Globe)
                InstrumentRow(iconLabel = "🌍") {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val path = Path()
                        val stepX = size.width / (geoHistory.size - 1).coerceAtLeast(1)
                        val midY = size.height / 2f

                        geoHistory.forEachIndexed { idx, value ->
                            val x = idx * stepX
                            val y = midY - (value * midY * 0.9f)
                            if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(path = path, color = Color.Cyan, style = Stroke(width = 3f))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Pill Toggles (VOX, FLIP, SLS, TWILIGHT)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FantasmToggle("VOX", voxEnabled) {
                        voxEnabled = !voxEnabled
                        sensorEngine.setVoxEnabled(voxEnabled)
                    }
                    FantasmToggle("FLIP", flipEnabled) { flipEnabled = !flipEnabled }
                    FantasmToggle("SLS", slsEnabled) { slsEnabled = !slsEnabled }
                    FantasmToggle("TWILIGHT", twilightEnabled) { twilightEnabled = !twilightEnabled }
                }
            }
        }
    }
}

@Composable
fun InstrumentRow(
    iconLabel: String,
    height: androidx.compose.ui.unit.Dp = 52.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .border(1.dp, Color.Gray)
            .background(Color(0xFF1E1E1E))
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = iconLabel,
            fontSize = 22.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Box(modifier = Modifier.fillMaxSize(), content = content)
    }
}

@Composable
fun FantasmToggle(label: String, enabled: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(text = "enable", color = Color.Gray, fontSize = 9.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(50.dp)
                .height(80.dp)
                .clip(RoundedCornerShape(25.dp))
                .border(2.dp, Color.LightGray, RoundedCornerShape(25.dp))
                .background(Color(0xFF2B2B2B))
                .clickable { onClick() }
                .padding(6.dp),
            contentAlignment = if (enabled) Alignment.BottomCenter else Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (enabled) Color(0xFF00FF44) else Color.Gray)
            )
        }
    }
}