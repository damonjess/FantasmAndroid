package com.example.fantasmandroid.ui

import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.fantasmandroid.FantasmController
import com.example.fantasmandroid.Session
import com.example.fantasmandroid.shareSession
import com.example.fantasmandroid.ui.theme.AccentCyan
import com.example.fantasmandroid.ui.theme.AccentGreen
import com.example.fantasmandroid.ui.theme.FantasmHand
import com.example.fantasmandroid.ui.theme.HairlineDim
import com.example.fantasmandroid.ui.theme.IceWhite
import com.example.fantasmandroid.ui.theme.Ink
import com.example.fantasmandroid.ui.theme.SignalGreen
import com.example.fantasmandroid.ui.theme.TextDim
import com.example.fantasmandroid.ui.theme.WarmRed
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.roundToInt

@Composable
fun PlaybackScreen(
    controller: FantasmController,
    sessionId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val sessions by controller.store.sessions.collectAsState()
    val session = sessions.firstOrNull { it.id == sessionId }
    var showDelete by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Ink)) {
        FantasmTopBar(title = session?.id ?: "Recording", onBack = onBack)

        if (session == null) {
            EmptyState(
                title = "RECORDING NOT FOUND",
                body = "This session may have been deleted from the device."
            )
            return@Column
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp)
        ) {
            Spacer(Modifier.height(14.dp))

            if (session.videoFile != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(3f / 4f)
                        .background(Color.Black)
                        .border(1.dp, HairlineDim)
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                setVideoPath(session.videoFile.absolutePath)
                                val controls = MediaController(ctx)
                                controls.setAnchorView(this)
                                setMediaController(controls)
                                setOnPreparedListener { player ->
                                    player.isLooping = false
                                    seekTo(1)
                                }
                            }
                        }
                    )
                }
            } else {
                MediaPreview(session)
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = formatSessionDate(session.startedAt),
                color = IceWhite,
                fontSize = 15.sp
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DataChip(formatDuration(session.durationMs), AccentCyan)
                DataChip(formatFileSize(session.totalBytes))
                DataChip("EMF ${(session.peakEmf * 100).roundToInt()}%", AccentGreen)
                DataChip("${session.transcript.size} WORDS")
            }

            if (session.audioFile != null) {
                Spacer(Modifier.height(16.dp))
                AudioPlayerBar(session.audioFile, session.durationMs)
            }

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionPill(
                    glyph = Glyph.Share,
                    label = "SHARE",
                    enabled = true,
                    modifier = Modifier.weight(1f)
                ) { shareSession(context, session) }
                ActionPill(
                    glyph = Glyph.Trash,
                    label = "DELETE",
                    enabled = true,
                    accent = WarmRed,
                    modifier = Modifier.weight(1f)
                ) { showDelete = true }
            }

            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "ITC TRANSCRIPT",
                    color = TextDim,
                    fontSize = 10.sp,
                    letterSpacing = 2.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = String.format("%02d", session.transcript.size),
                    color = TextDim,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(HairlineDim))

            if (session.transcript.isEmpty()) {
                Text(
                    text = "No ITC responses were captured in this session.",
                    color = TextDim,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 22.dp)
                )
            } else {
                session.transcript.takeLast(160).forEach { word ->
                    val offset = ((word.timestamp - session.startedAt) / 1000).coerceAtLeast(0)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = String.format("%02d:%02d", offset / 60, offset % 60),
                            color = TextDim,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.width(44.dp)
                        )
                        Text(
                            text = word.phrase,
                            color = IceWhite,
                            fontFamily = FantasmHand,
                            fontSize = 21.sp,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = word.cause,
                            color = AccentGreen.copy(alpha = 0.6f),
                            fontSize = 8.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(HairlineDim.copy(alpha = 0.4f)))
                }
            }

            Spacer(Modifier.height(30.dp))
            Text(
                text = "Recorded with Fantasm for Android - entertainment purposes only.",
                color = TextDim.copy(alpha = 0.55f),
                fontSize = 9.sp,
                modifier = Modifier.padding(bottom = 30.dp)
            )
        }
    }

    if (showDelete && session != null) {
        FantasmDialog(
            title = "DELETE RECORDING?",
            message = "This removes the ${session.id} session, its video, EVP audio and ITC transcript from this device.",
            confirmLabel = "DELETE",
            onConfirm = {
                showDelete = false
                controller.store.delete(session)
                onBack()
            },
            onDismiss = { showDelete = false }
        )
    }
}

@Composable
private fun MediaPreview(session: Session) {
    val bitmap = remember(session.thumbFile?.absolutePath) {
        session.thumbFile?.let { file ->
            runCatching { BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() }.getOrNull()
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .background(Brush.verticalGradient(listOf(Color(0xFF0B2424), Color(0xFF030909))))
            .border(1.dp, HairlineDim),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Session still",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().padding(10.dp)
            )
        } else {
            Glyph(Glyph.Film, size = 34.dp, tint = AccentCyan.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun AudioPlayerBar(file: File, durationMs: Long) {
    var playing by remember { mutableStateOf(false) }
    var position by remember { mutableIntStateOf(0) }
    var duration by remember { mutableIntStateOf(durationMs.toInt().coerceAtLeast(1)) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(file) {
        val mediaPlayer = MediaPlayer()
        runCatching {
            mediaPlayer.setDataSource(file.absolutePath)
            mediaPlayer.prepare()
            duration = mediaPlayer.duration.coerceAtLeast(1)
        }
        mediaPlayer.setOnCompletionListener {
            playing = false
            position = 0
        }
        player = mediaPlayer
        onDispose {
            runCatching { mediaPlayer.release() }
            player = null
        }
    }

    LaunchedEffect(playing) {
        while (playing) {
            player?.let { position = runCatching { it.currentPosition }.getOrDefault(0) }
            delay(220)
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF070707))
            .border(1.dp, HairlineDim)
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(AccentGreen.copy(alpha = 0.12f))
                    .border(1.dp, AccentGreen.copy(alpha = 0.6f), CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        val mp = player ?: return@clickable
                        if (playing) {
                            runCatching { mp.pause() }
                            playing = false
                        } else {
                            if (position >= duration - 150) {
                                runCatching { mp.seekTo(0) }
                                position = 0
                            }
                            runCatching { mp.start() }
                            playing = true
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Glyph(
                    if (playing) Glyph.Pause else Glyph.Play,
                    size = 15.dp,
                    tint = AccentGreen,
                    strokeWidth = 1.4.dp
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "EVP AUDIO",
                    color = IceWhite,
                    fontSize = 10.sp,
                    letterSpacing = 1.4.sp
                )
                Text(
                    text = "${formatDuration(position.toLong())} / ${formatDuration(duration.toLong())}",
                    color = TextDim,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
        Slider(
            value = (position.toFloat() / duration).coerceIn(0f, 1f),
            onValueChange = { fraction ->
                val target = (fraction * duration).toInt()
                position = target
                runCatching { player?.seekTo(target) }
            },
            colors = SliderDefaults.colors(
                thumbColor = SignalGreen,
                activeTrackColor = SignalGreen,
                inactiveTrackColor = Color(0xFF2A2A2A)
            ),
            modifier = Modifier.fillMaxWidth().height(30.dp)
        )
    }
}

@Composable
private fun ActionPill(
    glyph: Glyph,
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    accent: Color = AccentGreen,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .height(42.dp)
            .background(Color(0xFF080808), RoundedCornerShape(2.dp))
            .border(1.dp, if (enabled) accent.copy(alpha = 0.55f) else HairlineDim, RoundedCornerShape(2.dp))
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Glyph(glyph, size = 16.dp, tint = accent, strokeWidth = 1.3.dp)
        Spacer(Modifier.width(7.dp))
        Text(label, color = accent, fontSize = 10.sp, letterSpacing = 1.4.sp)
    }
}
