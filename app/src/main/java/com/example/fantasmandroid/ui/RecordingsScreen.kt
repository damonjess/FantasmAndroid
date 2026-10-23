package com.example.fantasmandroid.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fantasmandroid.FantasmController
import com.example.fantasmandroid.Session
import com.example.fantasmandroid.ui.theme.AccentGreen
import com.example.fantasmandroid.ui.theme.HairlineDim
import com.example.fantasmandroid.ui.theme.IceWhite
import com.example.fantasmandroid.ui.theme.Ink
import com.example.fantasmandroid.ui.theme.PanelBlack
import com.example.fantasmandroid.ui.theme.TextDim

@Composable
fun RecordingsScreen(
    controller: FantasmController,
    kind: RecordingKind?,
    onBack: () -> Unit,
    onOpenSession: (String) -> Unit,
    onOpenKind: (RecordingKind) -> Unit
) {
    val all by controller.store.sessions.collectAsState()

    val sessions = remember(all, kind) {
        when (kind) {
            null -> all
            RecordingKind.EVP -> all.filter { it.hasItc }
            RecordingKind.AUDIO -> all.filter { it.hasAudio }
            RecordingKind.VIDEO -> all.filter { it.hasVideo }
        }
    }
    val title = when (kind) {
        null -> "Recordings"
        RecordingKind.EVP -> "EVP ITC"
        RecordingKind.AUDIO -> "Audio"
        RecordingKind.VIDEO -> "Video"
    }

    Column(Modifier.fillMaxSize().background(Ink)) {
        FantasmTopBar(title = title, onBack = onBack)

        if (kind == null) {
            CategoryRow(
                glyph = Glyph.Headphones,
                title = "EVP",
                subtitle = "EVP ITC Recordings",
                count = all.count { it.hasItc },
                onClick = { onOpenKind(RecordingKind.EVP) }
            )
            CategoryRow(
                glyph = Glyph.Mic,
                title = "Audio",
                subtitle = "Audio Recordings",
                count = all.count { it.hasAudio },
                onClick = { onOpenKind(RecordingKind.AUDIO) }
            )
            CategoryRow(
                glyph = Glyph.VideoCamera,
                title = "Video",
                subtitle = "Video Recordings",
                count = all.count { it.hasVideo },
                onClick = { onOpenKind(RecordingKind.VIDEO) }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SESSIONS",
                color = TextDim,
                fontSize = 10.sp,
                letterSpacing = 2.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = String.format("%02d", sessions.size),
                color = TextDim,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        if (sessions.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                EmptyState(
                    title = "NO RECORDINGS YET",
                    body = "Start a full session from the device to capture ITC responses, EVP audio " +
                        "and video. Every session is logged here for review."
                )
            }
            Spacer(Modifier.height(24.dp))
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 28.dp)
            ) {
                items(sessions, key = { it.id }) { session ->
                    SessionRow(session) { onOpenSession(session.id) }
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(
    glyph: Glyph,
    title: String,
    subtitle: String,
    count: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .background(Color(0xFF090909))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Glyph(glyph, size = 27.dp, tint = IceWhite, strokeWidth = 1.3.dp)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = IceWhite, fontSize = 14.sp)
            Text(subtitle, color = TextDim, fontSize = 10.sp)
        }
        if (count > 0) {
            Text(
                text = String.format("%02d", count),
                color = AccentGreen.copy(alpha = 0.8f),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
        Glyph(Glyph.ChevronRight, size = 16.dp, tint = TextDim, strokeWidth = 1.3.dp)
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(HairlineDim).padding(horizontal = 16.dp))
}

@Composable
private fun SessionRow(session: Session, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .background(PanelBlack)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Thumbnail(session, 46.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = formatSessionDate(session.startedAt),
                color = IceWhite,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "${session.id} (${formatDuration(session.durationMs)} - ${formatFileSize(session.totalBytes)})",
                color = TextDim,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.3.sp
            )
        }
        Glyph(Glyph.ChevronRight, size = 15.dp, tint = TextDim.copy(alpha = 0.7f), strokeWidth = 1.2.dp)
    }
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(HairlineDim)
    )
}

@Composable
fun Thumbnail(session: Session, size: androidx.compose.ui.unit.Dp) {
    val bitmap = remember(session.thumbFile?.absolutePath) {
        session.thumbFile?.let { file ->
            runCatching {
                BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
            }.getOrNull()
        }
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(2.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0E2A2A), Color(0xFF071010))
                )
            )
            .border(1.dp, HairlineDim),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Session thumbnail",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Glyph(Glyph.Camera, size = size * 0.42f, tint = AccentGreen.copy(alpha = 0.6f))
        }
    }
}
