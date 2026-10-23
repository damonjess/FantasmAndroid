package com.example.fantasmandroid.ui

import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fantasmandroid.ui.theme.AccentCyan
import com.example.fantasmandroid.ui.theme.AccentGreen
import com.example.fantasmandroid.ui.theme.Amber
import com.example.fantasmandroid.ui.theme.ColdBlue
import com.example.fantasmandroid.ui.theme.FantasmDisplay
import com.example.fantasmandroid.ui.theme.FantasmHand
import com.example.fantasmandroid.ui.theme.Hairline
import com.example.fantasmandroid.ui.theme.HairlineDim
import com.example.fantasmandroid.ui.theme.IceWhite
import com.example.fantasmandroid.ui.theme.Ink
import com.example.fantasmandroid.ui.theme.PanelGlass
import com.example.fantasmandroid.ui.theme.SignalGreen
import com.example.fantasmandroid.ui.theme.Steel
import com.example.fantasmandroid.ui.theme.TextDim
import com.example.fantasmandroid.ui.theme.WarmRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

enum class Glyph {
    List, Film, Mail, Gear, Help, Scale, Camera, VideoCamera, Headphones, Mic, Globe,
    Magnet, ChevronRight, Play, Pause, Share, Trash, Close, Flip, Thermometer
}

@Composable
fun Glyph(
    glyph: Glyph,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    tint: Color = IceWhite,
    strokeWidth: Dp = 1.4.dp
) {
    androidx.compose.foundation.Canvas(modifier.size(size)) {
        val scale = this.size.minDimension / 24f
        val stroke = (strokeWidth.toPx() / scale).coerceAtLeast(0.6f)
        withTransform({ scale(scale, scale, pivot = Offset.Zero) }) {
            drawGlyph(glyph, tint, stroke)
        }
    }
}

private fun DrawScope.drawGlyph(glyph: Glyph, color: Color, width: Float) {
    fun strokePath(build: Path.() -> Unit) {
        val path = Path().apply(build)
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }

    when (glyph) {
        Glyph.List -> {
            for (row in 0..2) {
                val y = 6f + row * 6f
                drawRect(
                    color = color,
                    topLeft = Offset(3f, y - 2f),
                    size = Size(4f, 4f),
                    style = Stroke(width = width)
                )
                drawLine(color, Offset(10f, y), Offset(21f, y), strokeWidth = width, cap = StrokeCap.Round)
            }
        }

        Glyph.Film -> {
            drawRoundRect(
                color = color,
                topLeft = Offset(3f, 4f),
                size = Size(18f, 16f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5f),
                style = Stroke(width = width)
            )
            drawLine(color, Offset(8.5f, 4f), Offset(8.5f, 20f), strokeWidth = width)
            drawLine(color, Offset(15.5f, 4f), Offset(15.5f, 20f), strokeWidth = width)
            for (row in 0..2) {
                val y = 8f + row * 4f
                drawRect(color, Offset(4.5f, y), Size(2f, 2f))
                drawRect(color, Offset(17.5f, y), Size(2f, 2f))
            }
        }

        Glyph.Mail -> {
            drawRoundRect(
                color = color,
                topLeft = Offset(3f, 6f),
                size = Size(18f, 12f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.8f),
                style = Stroke(width = width)
            )
            strokePath {
                moveTo(4f, 7.5f)
                lineTo(12f, 13f)
                lineTo(20f, 7.5f)
            }
        }

        Glyph.Gear -> {
            drawCircle(color, radius = 6.2f, center = Offset(12f, 12f), style = Stroke(width = width))
            drawCircle(color, radius = 2.4f, center = Offset(12f, 12f), style = Stroke(width = width))
            for (i in 0 until 8) {
                val angle = Math.toRadians(i * 45.0)
                val startR = 6.8f
                val endR = 9f
                drawLine(
                    color = color,
                    start = Offset(
                        12f + (Math.cos(angle) * startR).toFloat(),
                        12f + (Math.sin(angle) * startR).toFloat()
                    ),
                    end = Offset(
                        12f + (Math.cos(angle) * endR).toFloat(),
                        12f + (Math.sin(angle) * endR).toFloat()
                    ),
                    strokeWidth = width,
                    cap = StrokeCap.Round
                )
            }
        }

        Glyph.Help -> {
            drawCircle(color, radius = 8.8f, center = Offset(12f, 12f), style = Stroke(width = width))
            strokePath {
                moveTo(8.6f, 9.4f)
                cubicTo(8.8f, 6.4f, 11.2f, 5.6f, 13.2f, 6.6f)
                cubicTo(15.6f, 7.8f, 15.3f, 10.6f, 12.6f, 12.1f)
                cubicTo(11.8f, 12.6f, 12f, 13.4f, 12f, 14.4f)
            }
            drawCircle(color, radius = 0.9f, center = Offset(12f, 17.4f))
        }

        Glyph.Scale -> {
            drawLine(color, Offset(12f, 4.4f), Offset(12f, 20f), strokeWidth = width)
            drawLine(color, Offset(7f, 20f), Offset(17f, 20f), strokeWidth = width)
            drawLine(color, Offset(4f, 6.5f), Offset(20f, 6.5f), strokeWidth = width)
            drawLine(color, Offset(12f, 4.2f), Offset(12f, 6.5f), strokeWidth = width)
            strokePath {
                moveTo(10.2f, 4.2f); lineTo(13.8f, 4.2f); lineTo(12f, 6.6f); close()
            }
            for (side in listOf(-1f, 1f)) {
                val x = 12f + side * 6f
                drawLine(color, Offset(12f + side * 2.4f, 6.5f), Offset(x, 10f), strokeWidth = width)
                strokePath {
                    moveTo(x - 2.6f, 10f)
                    quadraticBezierTo(x, 13.6f, x + 2.6f, 10f)
                }
            }
        }

        Glyph.Camera -> {
            strokePath {
                moveTo(3.4f, 8f)
                lineTo(3.4f, 7f)
                quadraticBezierTo(3.4f, 6f, 4.4f, 6f)
                lineTo(8.4f, 6f)
                lineTo(9.6f, 4.2f)
                lineTo(14.4f, 4.2f)
                lineTo(15.6f, 6f)
                lineTo(19.6f, 6f)
                quadraticBezierTo(20.6f, 6f, 20.6f, 7f)
                lineTo(20.6f, 18f)
                quadraticBezierTo(20.6f, 19f, 19.6f, 19f)
                lineTo(4.4f, 19f)
                quadraticBezierTo(3.4f, 19f, 3.4f, 18f)
                close()
            }
            drawCircle(color, radius = 3.6f, center = Offset(12f, 12.4f), style = Stroke(width = width))
            drawCircle(color, radius = 0.9f, center = Offset(18f, 8.6f))
        }

        Glyph.VideoCamera -> {
            drawRoundRect(
                color = color,
                topLeft = Offset(2.4f, 7f),
                size = Size(12.6f, 10f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f),
                style = Stroke(width = width)
            )
            drawCircle(color, radius = 2.6f, center = Offset(8.7f, 12f), style = Stroke(width = width))
            strokePath {
                moveTo(15f, 10.4f); lineTo(21f, 7.6f); lineTo(21f, 16.4f); lineTo(15f, 13.6f); close()
            }
        }

        Glyph.Headphones -> {
            strokePath {
                moveTo(4f, 14f)
                lineTo(4f, 12f)
                arcTo(
                    androidx.compose.ui.geometry.Rect(4f, 4f, 20f, 20f),
                    -180f, -180f, false
                )
                lineTo(20f, 14f)
            }
            drawRoundRect(
                color = color,
                topLeft = Offset(2.6f, 13f),
                size = Size(4.2f, 7f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.8f),
                style = Stroke(width = width)
            )
            drawRoundRect(
                color = color,
                topLeft = Offset(17.2f, 13f),
                size = Size(4.2f, 7f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.8f),
                style = Stroke(width = width)
            )
        }

        Glyph.Mic -> {
            drawRoundRect(
                color = color,
                topLeft = Offset(9f, 3f),
                size = Size(6f, 11f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f),
                style = Stroke(width = width)
            )
            strokePath {
                moveTo(5.5f, 11f)
                quadraticBezierTo(5.5f, 17.2f, 12f, 17.2f)
                quadraticBezierTo(18.5f, 17.2f, 18.5f, 11f)
            }
            drawLine(color, Offset(12f, 17.2f), Offset(12f, 20.5f), strokeWidth = width, cap = StrokeCap.Round)
            drawLine(color, Offset(8.5f, 20.5f), Offset(15.5f, 20.5f), strokeWidth = width, cap = StrokeCap.Round)
            for (i in -1..1) {
                drawLine(
                    color = color.copy(alpha = 0.55f),
                    start = Offset(10.4f, 8f + i * 2.2f),
                    end = Offset(13.6f, 8f + i * 2.2f),
                    strokeWidth = width * 0.6f,
                    cap = StrokeCap.Round
                )
            }
        }

        Glyph.Globe -> {
            drawCircle(color, radius = 8.6f, center = Offset(12f, 12f), style = Stroke(width = width))
            drawOval(
                color = color,
                topLeft = Offset(8.6f, 3.4f),
                size = Size(6.8f, 17.2f),
                style = Stroke(width = width * 0.8f)
            )
            drawLine(color, Offset(3.4f, 12f), Offset(20.6f, 12f), strokeWidth = width * 0.8f)
            strokePath {
                moveTo(6f, 7.5f)
                quadraticBezierTo(8.5f, 5.5f, 11f, 7f)
                quadraticBezierTo(9.5f, 9.2f, 7f, 9.5f)
            }
            strokePath {
                moveTo(14.5f, 15f)
                quadraticBezierTo(17f, 13.8f, 18.5f, 15.5f)
                quadraticBezierTo(17f, 18f, 14.5f, 18.2f)
                quadraticBezierTo(13.5f, 16.5f, 14.5f, 15f)
            }
        }

        Glyph.Magnet -> {
            strokePath {
                moveTo(5f, 4f)
                lineTo(5f, 11.5f)
                cubicTo(5f, 16.4f, 8.1f, 19.2f, 12f, 19.2f)
                cubicTo(15.9f, 19.2f, 19f, 16.4f, 19f, 11.5f)
                lineTo(19f, 4f)
            }
            strokePath {
                moveTo(9f, 4f)
                lineTo(9f, 11.5f)
                cubicTo(9f, 13.4f, 10.2f, 15.1f, 12f, 15.1f)
                cubicTo(13.8f, 15.1f, 15f, 13.4f, 15f, 11.5f)
                lineTo(15f, 4f)
            }
            drawLine(color, Offset(5f, 7.2f), Offset(9f, 7.2f), strokeWidth = width)
            drawLine(color, Offset(15f, 7.2f), Offset(19f, 7.2f), strokeWidth = width)
        }

        Glyph.ChevronRight -> {
            strokePath {
                moveTo(9f, 5f); lineTo(16f, 12f); lineTo(9f, 19f)
            }
        }

        Glyph.Play -> {
            strokePath {
                moveTo(8f, 5f); lineTo(19f, 12f); lineTo(8f, 19f); close()
            }
        }

        Glyph.Pause -> {
            drawLine(color, Offset(9f, 5f), Offset(9f, 19f), strokeWidth = width * 1.6f, cap = StrokeCap.Round)
            drawLine(color, Offset(15f, 5f), Offset(15f, 19f), strokeWidth = width * 1.6f, cap = StrokeCap.Round)
        }

        Glyph.Share -> {
            drawCircle(color, radius = 2.2f, center = Offset(17f, 5.5f), style = Stroke(width = width))
            drawCircle(color, radius = 2.2f, center = Offset(17f, 18.5f), style = Stroke(width = width))
            drawCircle(color, radius = 2.2f, center = Offset(6f, 12f), style = Stroke(width = width))
            drawLine(color, Offset(8f, 11f), Offset(15f, 6.6f), strokeWidth = width)
            drawLine(color, Offset(8f, 13f), Offset(15f, 17.4f), strokeWidth = width)
        }

        Glyph.Trash -> {
            drawLine(color, Offset(4.5f, 6.5f), Offset(19.5f, 6.5f), strokeWidth = width, cap = StrokeCap.Round)
            strokePath {
                moveTo(7f, 6.5f); lineTo(8f, 20f); lineTo(16f, 20f); lineTo(17f, 6.5f)
            }
            strokePath {
                moveTo(9.4f, 6.5f); lineTo(9.8f, 4f); lineTo(14.2f, 4f); lineTo(14.6f, 6.5f)
            }
            drawLine(color, Offset(10.4f, 10f), Offset(10.8f, 17f), strokeWidth = width * 0.7f)
            drawLine(color, Offset(13.6f, 10f), Offset(13.2f, 17f), strokeWidth = width * 0.7f)
        }

        Glyph.Close -> {
            drawLine(color, Offset(6f, 6f), Offset(18f, 18f), strokeWidth = width * 1.4f, cap = StrokeCap.Round)
            drawLine(color, Offset(18f, 6f), Offset(6f, 18f), strokeWidth = width * 1.4f, cap = StrokeCap.Round)
        }

        Glyph.Flip -> {
            strokePath {
                moveTo(4.5f, 9f)
                lineTo(18f, 9f)
            }
            strokePath {
                moveTo(15f, 6f); lineTo(18.4f, 9f); lineTo(15f, 12f)
            }
            strokePath {
                moveTo(19.5f, 15f)
                lineTo(6f, 15f)
            }
            strokePath {
                moveTo(9f, 12f); lineTo(5.6f, 15f); lineTo(9f, 18f)
            }
        }

        Glyph.Thermometer -> {
            drawRoundRect(
                color = color,
                topLeft = Offset(10f, 2.6f),
                size = Size(4f, 12f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f),
                style = Stroke(width = width)
            )
            drawCircle(color, radius = 3.2f, center = Offset(12f, 17.4f), style = Stroke(width = width))
            drawLine(color, Offset(12f, 8f), Offset(12f, 16f), strokeWidth = width * 1.1f, cap = StrokeCap.Round)
        }
    }
}

@Composable
fun EmfBar(level: Float, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier.fillMaxWidth().height(11.dp)) {
        val radius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f)
        drawRoundRect(
            color = Color(0xFF161616),
            cornerRadius = radius
        )
        drawRoundRect(
            color = Color(0xFF3A3A3A),
            cornerRadius = radius,
            style = Stroke(width = 1.2f)
        )
        val filled = (size.width * level.coerceIn(0f, 1f)).coerceAtLeast(size.height)
        drawRoundRect(
            brush = Brush.horizontalGradient(
                listOf(ColdBlue, AccentCyan, SignalGreen),
                startX = 0f,
                endX = max(size.width * 0.85f, filled)
            ),
            size = Size(filled, size.height),
            cornerRadius = radius
        )
        drawRoundRect(
            brush = Brush.horizontalGradient(
                listOf(Color.Transparent, SignalGreen.copy(alpha = 0.35f)),
                startX = 0f,
                endX = filled
            ),
            size = Size(filled, size.height / 2f),
            cornerRadius = radius
        )
    }
}

@Composable
fun ItcBanner(
    text: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(Color(0xCC000000))
            .border(1.dp, Color(0x99C9CFC9))
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.ifBlank { "LISTENING..." },
            color = if (text.isBlank()) IceWhite.copy(alpha = 0.45f) else IceWhite,
            fontFamily = FantasmHand,
            fontSize = 26.sp,
            letterSpacing = 1.5.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            lineHeight = 25.sp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}

@Composable
fun EmfDotMatrix(history: List<Float>, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier.fillMaxSize()) {
        val columns = history.size.coerceAtLeast(1)
        val gap = 1.6.dp.toPx()
        val cell = ((size.width - gap * (columns - 1)) / columns).coerceAtMost(3.2.dp.toPx())
        val rows = 3
        val rowGap = 1.4.dp.toPx()
        val startY = size.height - cell * rows - rowGap * (rows - 1)
        for (col in 0 until columns) {
            val value = history.getOrElse(col) { 0f }.coerceIn(0f, 1f)
            val lit = (value * rows).roundToInt().coerceIn(0, rows)
            for (row in 0 until rows) {
                val x = col * (cell + gap) + (gap / 2f)
                val y = startY + (rows - 1 - row) * (cell + rowGap)
                val litRow = row < lit
                val color = if (litRow) {
                    lerp(AccentCyan, SignalGreen, (row + 1) / rows.toFloat() * value)
                } else {
                    Color(0xFF1E3A34).copy(alpha = 0.55f)
                }
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, y),
                    size = Size(cell, cell),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(0.6f)
                )
            }
        }
    }
}

@Composable
fun AudioSpectrum(bands: List<Float>, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier.fillMaxSize()) {
        val columns = bands.size.coerceAtLeast(1)
        val gap = 1.6.dp.toPx()
        val barWidth = ((size.width - gap * (columns - 1)) / columns).coerceAtLeast(1.5f)
        val rows = 12
        val rowGap = 1.2.dp.toPx()
        val blockHeight = ((size.height - rowGap * (rows - 1)) / rows).coerceAtLeast(1.5f)
        for (col in 0 until columns) {
            val value = bands.getOrElse(col) { 0f }.coerceIn(0f, 1f)
            val lit = ceil(value * rows).toInt().coerceIn(0, rows)
            for (row in 0 until rows) {
                val color = when {
                    row >= rows - 2 -> WarmRed
                    row >= rows - 4 -> Amber
                    else -> lerp(Color(0xFF19E845), Color(0xFFB9FF36), row / 8f)
                }
                val y = size.height - (row + 1) * blockHeight - row * rowGap
                val alpha = if (row < lit) 1f else 0.10f
                drawRoundRect(
                    color = color.copy(alpha = alpha),
                    topLeft = Offset(col * (barWidth + gap), y),
                    size = Size(barWidth, blockHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(0.6f)
                )
            }
        }
    }
}

@Composable
fun AudioTrace(trace: List<Float>, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier.fillMaxSize()) {
        val count = trace.size.coerceAtLeast(1)
        val barWidth = (size.width / count).coerceAtLeast(1f)
        for (i in 0 until count) {
            val value = trace.getOrElse(i) { 0f }.coerceIn(0f, 1f)
            val height = (value * size.height * 0.92f).coerceAtLeast(1.2f)
            val color = when {
                value > 0.8f -> WarmRed
                value > 0.62f -> Amber
                value > 0.38f -> SignalGreen
                else -> AccentCyan
            }
            drawRect(
                color = color.copy(alpha = 0.92f),
                topLeft = Offset(i * barWidth, size.height - height),
                size = Size((barWidth - 0.6f).coerceAtLeast(0.8f), height)
            )
        }
        drawLine(
            color = AccentCyan.copy(alpha = 0.35f),
            start = Offset(0f, size.height - 1f),
            end = Offset(size.width, size.height - 1f),
            strokeWidth = 1f
        )
    }
}

@Composable
fun GeoWave(values: List<Float>, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier.fillMaxSize()) {
        val mid = size.height / 2f
        drawLine(
            color = AccentCyan.copy(alpha = 0.18f),
            start = Offset(0f, mid),
            end = Offset(size.width, mid),
            strokeWidth = 1f
        )
        if (values.size < 2) return@Canvas
        val step = size.width / (values.size - 1)
        val path = Path()
        values.forEachIndexed { index, value ->
            val x = index * step
            val y = mid - value.coerceIn(-1f, 1f) * mid * 0.9f
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path,
            color = AccentCyan.copy(alpha = 0.25f),
            style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawPath(
            path,
            color = AccentCyan,
            style = Stroke(width = 1.6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun InstrumentModule(
    icon: Glyph,
    modifier: Modifier = Modifier,
    height: Dp,
    content: @Composable BoxScope.() -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(PanelGlass)
            .border(1.dp, HairlineDim)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(42.dp).fillMaxSize(), contentAlignment = Alignment.Center) {
            Glyph(icon, size = 25.dp, tint = Color(0xFFDDE3DD), strokeWidth = 1.3.dp)
        }
        Box(Modifier.fillMaxSize().padding(end = 8.dp), content = content)
    }
}

@Composable
fun RockerToggle(
    label: String,
    enabled: Boolean,
    tintTop: Color,
    tintBottom: Color,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit
) {
    val knobOffset by animateDpAsState(
        targetValue = if (enabled) 5.dp else 30.dp,
        animationSpec = tween(180),
        label = "knob"
    )
    Column(
        modifier = modifier
            .width(58.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onToggle() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = if (enabled) IceWhite else TextDim,
            fontSize = 9.sp,
            letterSpacing = 0.5.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
        Text(text = "enable", color = TextDim.copy(alpha = 0.8f), fontSize = 8.sp)
        Spacer(Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .width(46.dp)
                .height(54.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Brush.verticalGradient(listOf(tintTop, tintBottom)))
                .border(
                    width = if (enabled) 1.2.dp else 1.dp,
                    color = if (enabled) AccentGreen.copy(alpha = 0.8f) else Color(0x66707470),
                    shape = RoundedCornerShape(9.dp)
                )
        ) {
            androidx.compose.foundation.Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(22.dp)
                    .padding(top = 4.dp)
            ) {
                val w = 9.dp.toPx()
                val cx = size.width / 2f
                val firstY = size.height * 0.32f
                val secondY = size.height * 0.66f
                val color = if (enabled) AccentGreen.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.55f)
                for (y in listOf(firstY, secondY)) {
                    val path = Path().apply {
                        moveTo(cx - w / 2f, y + 3.dp.toPx())
                        lineTo(cx, y)
                        lineTo(cx + w / 2f, y + 3.dp.toPx())
                    }
                    drawPath(
                        path,
                        color,
                        style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = knobOffset)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color.White, Color(0xFFCFCFCF), Color(0xFF8E8E8E))
                        )
                    )
                    .border(0.6.dp, Color(0xFF5E5E5E), CircleShape)
            )
        }
    }
}

@Composable
fun RoundDeckButton(
    active: Boolean,
    enabled: Boolean,
    pulse: Float,
    glowColor: Color,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .alpha(if (enabled) 1f else 0.55f)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFFF0F0F0),
                        Color(0xFF777777),
                        Color(0xFFC9C9C9),
                        Color(0xFF4A4A4A)
                    )
                )
            )
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(2.4.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    if (active) {
                        Brush.radialGradient(
                            listOf(glowColor.copy(alpha = 0.30f), Color(0xFF070707))
                        )
                    } else {
                        Brush.radialGradient(listOf(Color(0xFF171717), Color(0xFF030303)))
                    }
                ),
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}

@Composable
fun HudToolbar(onGlyph: (Glyph) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().background(Color(0xE6050505))) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(HairlineDim))
        Row(
            modifier = Modifier.fillMaxWidth().height(45.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val glyphs = listOf(
                Glyph.List, Glyph.Film, Glyph.Mail, Glyph.Gear,
                Glyph.Help, Glyph.Scale, Glyph.Camera
            )
            glyphs.forEach { glyph ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onGlyph(glyph) },
                    contentAlignment = Alignment.Center
                ) {
                    Glyph(glyph, size = 22.dp, tint = Color(0xFFE9EDE9), strokeWidth = 1.3.dp)
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(HairlineDim))
    }
}

@Composable
fun FantasmTopBar(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth().background(Color(0xFF050505)).statusBarsPadding()) {
        Box(Modifier.fillMaxWidth().height(50.dp)) {
            if (onBack != null) {
                Text(
                    text = "Back",
                    color = Color(0xFF9C9C9C),
                    fontSize = 14.sp,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onBack
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
            Text(
                text = "[ $title ]",
                color = IceWhite,
                fontFamily = FantasmDisplay,
                fontSize = 19.sp,
                letterSpacing = 3.sp,
                modifier = Modifier.align(Alignment.Center)
            )
            Glyph(
                Glyph.Thermometer,
                size = 17.dp,
                tint = AccentCyan.copy(alpha = 0.9f),
                strokeWidth = 1.3.dp,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp)
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(HairlineDim))
    }
}

@Composable
fun FantasmSwitch(checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val offset by animateDpAsState(if (checked) 21.dp else 3.dp, tween(160), label = "switch")
    Box(
        modifier = modifier
            .width(45.dp)
            .height(26.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(if (checked) SignalGreen.copy(alpha = 0.9f) else Color(0xFF1B1B1B))
            .border(1.dp, if (checked) SignalGreen else Color(0xFF4A4F4A), RoundedCornerShape(13.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onChange(!checked) }
    ) {
        Box(
            modifier = Modifier
                .offset(x = offset, y = 3.dp)
                .size(18.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color.White, Color(0xFFBDBDBD))))
        )
    }
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp)) {
        Text(
            text = text,
            color = TextDim,
            fontSize = 10.sp,
            letterSpacing = 2.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(HairlineDim))
    }
}

@Composable
fun SettingRow(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = IceWhite, fontSize = 14.sp, letterSpacing = 0.3.sp)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, color = TextDim, fontSize = 11.sp, lineHeight = 15.sp)
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun FantasmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0A0A0A),
        titleContentColor = IceWhite,
        textContentColor = TextDim,
        tonalElevation = 0.dp,
        modifier = Modifier.border(1.dp, Hairline),
        title = {
            Text(title, fontFamily = FantasmDisplay, fontSize = 19.sp, letterSpacing = 2.sp)
        },
        text = { Text(message, fontSize = 13.sp, lineHeight = 19.sp) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = SignalGreen, fontSize = 13.sp, letterSpacing = 1.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = TextDim, fontSize = 13.sp, letterSpacing = 1.sp)
            }
        }
    )
}

@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            title,
            color = IceWhite,
            fontFamily = FantasmDisplay,
            fontSize = 18.sp,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(body, color = TextDim, fontSize = 12.sp, lineHeight = 18.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun DataChip(text: String, color: Color = TextDim, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = color,
        fontSize = 9.sp,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 0.6.sp,
        modifier = modifier
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(2.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    )
}

fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

fun formatFileSize(bytes: Long): String = when {
    bytes >= 1_048_576 -> String.format(Locale.US, "%.2fMB", bytes / 1_048_576.0)
    bytes >= 1_024 -> String.format(Locale.US, "%.1fKB", bytes / 1_024.0)
    else -> "${bytes}B"
}

fun formatSessionDate(timestamp: Long): String =
    SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss", Locale.US).format(Date(timestamp))

fun formatClock(timestamp: Long): String =
    SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(timestamp))
