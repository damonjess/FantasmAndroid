package com.example.fantasmandroid.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fantasmandroid.ui.theme.AccentCyan
import com.example.fantasmandroid.ui.theme.AccentGreen
import com.example.fantasmandroid.ui.theme.FantasmDisplay
import com.example.fantasmandroid.ui.theme.HairlineDim
import com.example.fantasmandroid.ui.theme.IceWhite
import com.example.fantasmandroid.ui.theme.Ink
import com.example.fantasmandroid.ui.theme.SignalGreen
import com.example.fantasmandroid.ui.theme.TextDim
import com.example.fantasmandroid.ui.theme.WarmRed

private data class HelpStep(
    val number: Int,
    val title: String,
    val body: String,
    val accent: Color
)

private val helpSteps = listOf(
    HelpStep(
        1,
        "Find a quiet location.",
        "The ideal environment is quiet and free from distractions. Ambient noise and local wiring both affect the sensors, so pick a room you know.",
        AccentCyan
    ),
    HelpStep(
        2,
        "Power on the ITC tool.",
        "The ITC system starts with the device and calibrates its magnetic baseline. Give it a few moments to settle before you begin asking questions.",
        Color(0xFF1F7BFF)
    ),
    HelpStep(
        3,
        "Ask questions.",
        "Speak clearly and leave pauses between each question. The microphone and ITC system listen for anomalies and for the silence after a question.",
        AccentGreen
    ),
    HelpStep(
        4,
        "Observe sensor data.",
        "Watch the EMF bar, the frequency spectrum and the geophone trace for changes, fluctuations or unusual patterns in the readings.",
        AccentCyan
    ),
    HelpStep(
        5,
        "Be patient.",
        "Words may not be presented immediately after initialisation. It can take time before a phonetic event is reported by the system.",
        SignalGreen
    ),
    HelpStep(
        6,
        "Record the session.",
        "Use the green session button to capture the ITC data, the EVP audio and the video view simultaneously in one recording.",
        AccentGreen
    ),
    HelpStep(
        7,
        "Review the recordings.",
        "Select the filmstrip icon on the main menu to review EVP ITC, audio and video recordings, then share the evidence you want to keep.",
        AccentCyan
    ),
    HelpStep(
        8,
        "Interpret findings cautiously.",
        "Approach results with a logical and skeptical mindset. It may take some time before a phonetic event is reported. Fantasm cannot prove or disprove the paranormal.",
        WarmRed
    )
)

@Composable
fun HelpScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Ink)) {
        FantasmTopBar(title = "Help", onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(18.dp))
            Text(
                text = "How To Use",
                color = IceWhite,
                fontFamily = FantasmDisplay,
                fontSize = 26.sp,
                letterSpacing = 2.sp
            )
            Text(
                text = "FANTASM PARANORMAL RESEARCH DEVICE",
                color = AccentGreen.copy(alpha = 0.65f),
                fontSize = 8.sp,
                letterSpacing = 1.8.sp
            )
            Spacer(Modifier.height(20.dp))

            helpSteps.forEach { step ->
                Row(Modifier.fillMaxWidth().padding(bottom = 18.dp)) {
                    HelpTile(step)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = step.title,
                            color = IceWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = step.body,
                            color = TextDim,
                            fontSize = 11.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            SectionHeader("ANDROID NOTES")
            HelpNote(
                "Camera and microphone",
                "The device needs camera and microphone permission. Video is written to app-private storage."
            )
            HelpNote(
                "About the LiDAR render",
                "Most Android phones do not have a LiDAR scanner. Fantasm uses AI depth segmentation to produce the same false-colour depth render."
            )
            HelpNote(
                "About SLS",
                "Skeletal tracking runs on-device with Google ML Kit pose detection. No frames leave the phone."
            )
            HelpNote(
                "About Spirit Vox",
                "Responses are spoken with the Android text-to-speech voice. Whisper and Enhanced Vox alter pitch, rate and volume."
            )
            HelpNote(
                "Privacy",
                "Sessions, thumbnails and transcripts stay on this device. The app contains no analytics and no network code."
            )

            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(HairlineDim))
            Spacer(Modifier.height(12.dp))
            Text(
                text = "IGNORED. UNTIL NOW.",
                color = AccentCyan.copy(alpha = 0.8f),
                fontFamily = FantasmDisplay,
                fontSize = 15.sp,
                letterSpacing = 2.sp
            )
            Text(
                text = "Real guidance for your investigation. Approach every result with a logical," +
                    " skeptical mindset - and above all else, enjoy.",
                color = TextDim,
                fontSize = 11.sp,
                lineHeight = 17.sp
            )
            Spacer(Modifier.height(34.dp))
        }
    }
}

@Composable
private fun HelpTile(step: HelpStep) {
    Box(
        modifier = Modifier
            .width(58.dp)
            .height(72.dp)
            .background(
                Brush.verticalGradient(
                    listOf(step.accent.copy(alpha = 0.35f), Color(0xFF050505))
                )
            )
            .border(1.dp, HairlineDim)
    ) {
        Canvas(Modifier.fillMaxSize().padding(4.dp)) {
            val w = size.width
            val h = size.height
            drawRect(Color(0x88000000))
            // Module frames.
            drawRect(
                color = Color(0x559AA39A),
                topLeft = Offset(0f, h * 0.13f),
                size = Size(w, h * 0.2f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(1f)
            )
            drawRect(
                color = Color(0x559AA39A),
                topLeft = Offset(0f, h * 0.38f),
                size = Size(w, h * 0.28f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(1f)
            )
            // Spectrum bars.
            val bars = 11
            val gap = 1.4f
            val barWidth = (w - gap * (bars - 1)) / bars
            for (i in 0 until bars) {
                val height = h * (0.08f + ((i * 37) % 11) / 32f)
                val color = when {
                    i % 7 == 3 -> WarmRed
                    i % 5 == 2 -> Color(0xFFFFD400)
                    else -> SignalGreen
                }
                drawRect(
                    color = color,
                    topLeft = Offset(i * (barWidth + gap), h * 0.62f - height),
                    size = Size(barWidth, height)
                )
            }
            // Geo trace.
            val points = 12
            var previous = Offset(0f, h * 0.8f)
            for (i in 1 until points) {
                val x = w * i / (points - 1)
                val y = h * 0.8f - (((i * 29) % 7) - 3) * h * 0.012f
                drawLine(
                    color = AccentCyan,
                    start = previous,
                    end = Offset(x, y),
                    strokeWidth = 1.2f,
                    cap = StrokeCap.Round
                )
                previous = Offset(x, y)
            }
            // Deck.
            drawCircle(step.accent, radius = w * 0.09f, center = Offset(w * 0.18f, h * 0.94f))
            drawCircle(SignalGreen, radius = w * 0.09f, center = Offset(w * 0.82f, h * 0.94f))
            drawLine(
                color = Color(0x889AA39A),
                start = Offset(0f, h * 0.34f),
                end = Offset(w, h * 0.34f),
                strokeWidth = 0.8f
            )
        }
    }
}

@Composable
private fun HelpNote(title: String, body: String) {
    Column(Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
        Text(title, color = IceWhite, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(3.dp))
        Text(body, color = TextDim, fontSize = 11.sp, lineHeight = 17.sp)
    }
}

@Composable
fun LegalScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Ink)) {
        FantasmTopBar(title = "Disclaimer", onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
        ) {
            Spacer(Modifier.height(20.dp))
            Text(
                text = "DISCLAIMER",
                color = IceWhite,
                fontFamily = FantasmDisplay,
                fontSize = 24.sp,
                letterSpacing = 3.sp
            )
            Spacer(Modifier.height(14.dp))
            LegalBlock(
                "ENTERTAINMENT ONLY",
                "Fantasm is designed to aid the paranormal investigator in identifying unexplained " +
                    "phenomena. Fantasm cannot prove or disprove the paranormal. The app is provided " +
                    "for entertainment purposes only. We are not responsible for the use, misuse or " +
                    "interpretation of this software. Please investigate responsibly."
            )
            LegalBlock(
                "SENSOR INTERPRETATION",
                "EMF, geophone and frequency readings reflect real hardware sensors on this device, " +
                    "but environmental and device interference is common. ITC responses are generated " +
                    "by the Fantasm engine from sensor activity and are not evidence of an external " +
                    "intelligence. Treat every result with a skeptical mindset."
            )
            LegalBlock(
                "PRIVACY",
                "Fantasm collects no personal data, contains no analytics and makes no network " +
                    "requests. Camera frames are analysed on-device for SLS and the depth render. " +
                    "Sessions, thumbnails, EVP audio, video and transcripts are stored in the " +
                    "app's private storage and are removed when you delete the session or uninstall " +
                    "the app. Nothing is uploaded."
            )
            LegalBlock(
                "PERMISSIONS",
                "Camera access powers the viewfinder, SLS, depth render, Twilight vision and video " +
                    "recording. Microphone access powers the frequency spectrum, geophone events, " +
                    "EVP audio capture and the ITC response engine. Both are required for a session."
            )
            LegalBlock(
                "ATTRIBUTION",
                "Fantasm(TM) is the product of its original developer. This Android application is an " +
                    "independent implementation for personal use and is not affiliated with or " +
                    "endorsed by the original publisher. Oswald and Caveat typefaces are used under " +
                    "the SIL Open Font License."
            )
            LegalBlock(
                "AGE RATING",
                "13+ - frequent horror and fear themes. Parental guidance is recommended for younger " +
                    "investigators."
            )
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(HairlineDim))
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Copyright (c) 2026. Fantasm for Android.",
                color = TextDim.copy(alpha = 0.6f),
                fontSize = 9.sp
            )
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun LegalBlock(title: String, body: String) {
    Column(Modifier.padding(bottom = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(4.dp).background(AccentGreen))
            Spacer(Modifier.width(7.dp))
            Text(
                text = title,
                color = AccentGreen.copy(alpha = 0.8f),
                fontSize = 9.sp,
                letterSpacing = 1.6.sp
            )
        }
        Spacer(Modifier.height(7.dp))
        Text(body, color = TextDim, fontSize = 11.sp, lineHeight = 18.sp)
    }
}
