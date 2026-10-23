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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fantasmandroid.FantasmController
import com.example.fantasmandroid.Session
import com.example.fantasmandroid.ui.theme.AccentGreen
import com.example.fantasmandroid.ui.theme.HairlineDim
import com.example.fantasmandroid.ui.theme.IceWhite
import com.example.fantasmandroid.ui.theme.Ink
import com.example.fantasmandroid.ui.theme.TextDim

@Composable
fun GalleryScreen(
    controller: FantasmController,
    onBack: () -> Unit,
    onOpenSession: (String) -> Unit
) {
    val sessions by controller.store.sessions.collectAsState()
    val captures = remember(sessions) { sessions.filter { it.thumbFile != null } }

    Column(Modifier.fillMaxSize().background(Ink)) {
        FantasmTopBar(title = "Camera", onBack = onBack)

        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
            Text(
                text = "STILLS CAPTURED AT SESSION START",
                color = TextDim,
                fontSize = 9.sp,
                letterSpacing = 1.4.sp,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                text = "${captures.size} CAPTURES",
                color = AccentGreen.copy(alpha = 0.8f),
                fontSize = 9.sp,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(top = 3.dp, bottom = 12.dp)
            )
        }

        if (captures.isEmpty()) {
            EmptyState(
                title = "NO CAPTURES",
                body = "A still is saved automatically each time you start a full session. " +
                    "Start one from the device and it will appear here."
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                items(captures, key = { it.id }) { session ->
                    GalleryTile(session) { onOpenSession(session.id) }
                }
            }
        }
    }
}

@Composable
private fun GalleryTile(session: Session, onClick: () -> Unit) {
    val bitmap = remember(session.thumbFile?.absolutePath) {
        session.thumbFile?.let { file ->
            runCatching { BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() }.getOrNull()
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(Brush.verticalGradient(listOf(Color(0xFF0C2020), Color(0xFF040808))))
            .border(1.dp, HairlineDim)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Session ${session.id}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Glyph(Glyph.Camera, size = 24.dp, tint = AccentGreen.copy(alpha = 0.5f))
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000)))
                )
                .padding(horizontal = 5.dp, vertical = 4.dp)
        ) {
            Text(
                text = session.id,
                color = IceWhite,
                fontSize = 7.sp,
                letterSpacing = 0.4.sp
            )
        }
    }
}
