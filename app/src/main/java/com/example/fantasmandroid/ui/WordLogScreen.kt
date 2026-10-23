package com.example.fantasmandroid.ui

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.fantasmandroid.ItcEvent
import com.example.fantasmandroid.shareText
import com.example.fantasmandroid.ui.theme.AccentCyan
import com.example.fantasmandroid.ui.theme.AccentGreen
import com.example.fantasmandroid.ui.theme.FantasmHand
import com.example.fantasmandroid.ui.theme.HairlineDim
import com.example.fantasmandroid.ui.theme.IceWhite
import com.example.fantasmandroid.ui.theme.Ink
import com.example.fantasmandroid.ui.theme.SignalGreen
import com.example.fantasmandroid.ui.theme.TextDim
import kotlin.math.roundToInt

@Composable
fun WordLogScreen(
    controller: FantasmController,
    onBack: () -> Unit,
    onOpenMatrix: () -> Unit
) {
    val context = LocalContext.current
    val history by controller.itc.history.collectAsState()
    val current by controller.itc.current.collectAsState()

    Column(Modifier.fillMaxSize().background(Ink)) {
        FantasmTopBar(title = "ITC Log", onBack = onBack)

        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .background(Color(0x33000000))
                    .border(1.dp, Color(0x99C9CFC9)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = current?.phrase ?: "LISTENING...",
                    color = if (current == null) IceWhite.copy(alpha = 0.4f) else IceWhite,
                    fontFamily = FantasmHand,
                    fontSize = 26.sp,
                    letterSpacing = 2.sp,
                    maxLines = 1
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LogAction(
                    label = "SHARE LOG",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val text = buildLogText(history)
                        shareText(context, "Fantasm ITC log", text)
                    }
                )
                LogAction(
                    label = "MATRIX",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenMatrix
                )
            }
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "EVENTS",
                    color = TextDim,
                    fontSize = 10.sp,
                    letterSpacing = 2.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = String.format("%02d", history.size),
                    color = TextDim,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(HairlineDim))
        }

        if (history.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                EmptyState(
                    title = "NO RESPONSES YET",
                    body = "Ask a question, leave a pause, and watch the EMF spectrum. " +
                        "Every ITC response is timestamped here with the trigger that produced it."
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 28.dp, top = 4.dp)
            ) {
                items(history.asReversed(), key = { it.id }) { event ->
                    LogRow(event)
                }
            }
        }
    }
}

@Composable
private fun LogAction(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(36.dp)
            .background(Color(0xFF080808), androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
            .border(1.dp, AccentGreen.copy(alpha = 0.4f), androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = AccentGreen, fontSize = 9.sp, letterSpacing = 1.4.sp)
    }
}

@Composable
private fun LogRow(event: ItcEvent) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.width(58.dp)) {
                Text(
                    text = formatClock(event.timestamp),
                    color = TextDim,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${(event.emf * 100).roundToInt()}% EMF",
                    color = AccentCyan.copy(alpha = 0.65f),
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Text(
                text = event.phrase,
                color = IceWhite,
                fontFamily = FantasmHand,
                fontSize = 21.sp,
                letterSpacing = 0.5.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = event.cause,
                color = SignalGreen.copy(alpha = 0.55f),
                fontSize = 8.sp,
                letterSpacing = 0.6.sp
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(1.dp)
                .background(HairlineDim.copy(alpha = 0.5f))
        )
    }
}

private fun buildLogText(history: List<ItcEvent>): String {
    val builder = StringBuilder()
    builder.appendLine("FANTASM ITC LOG")
    builder.appendLine("${history.size} responses captured this session")
    builder.appendLine()
    history.asReversed().forEach { event ->
        builder.appendLine(
            String.format(
                java.util.Locale.US,
                "%s  %s  [%s]",
                formatClock(event.timestamp),
                event.phrase,
                event.cause
            )
        )
    }
    builder.appendLine()
    builder.appendLine("Fantasm for Android - entertainment purposes only.")
    return builder.toString()
}
