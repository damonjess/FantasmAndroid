package com.example.fantasmandroid.ui

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fantasmandroid.FantasmController
import com.example.fantasmandroid.ItcDictionary
import com.example.fantasmandroid.ui.theme.AccentCyan
import com.example.fantasmandroid.ui.theme.AccentGreen
import com.example.fantasmandroid.ui.theme.FantasmHand
import com.example.fantasmandroid.ui.theme.HairlineDim
import com.example.fantasmandroid.ui.theme.IceWhite
import com.example.fantasmandroid.ui.theme.Ink
import com.example.fantasmandroid.ui.theme.SignalGreen
import com.example.fantasmandroid.ui.theme.TextDim
import kotlin.random.Random

@Composable
fun MatrixScreen(
    controller: FantasmController,
    onBack: () -> Unit
) {
    val current by controller.itc.current.collectAsState()
    val words = remember {
        ItcDictionary.all
            .filter { it.length in 4..18 }
            .shuffled(Random(17))
            .take(24)
    }
    // If the live response is not already on the board, swap it into a cell so the
    // grid always lights up with the current message.
    val board = remember(words, current?.id) {
        val list = words.toMutableList()
        val event = current
        if (event != null && event.phrase !in list && list.isNotEmpty()) {
            val index = (event.id % list.size).toInt()
            list[index] = event.phrase
        }
        list
    }

    Column(Modifier.fillMaxSize().background(Ink)) {
        FantasmTopBar(title = "Matrix", onBack = onBack)

        Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 10.dp)) {
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .background(Color(0x33000000))
                    .border(1.dp, Color(0x88C9CFC9)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = current?.phrase ?: "AWAITING RESPONSE",
                    color = if (current == null) TextDim else IceWhite,
                    fontFamily = FantasmHand,
                    fontSize = 23.sp,
                    letterSpacing = 1.6.sp,
                    maxLines = 1
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "SENSOR EVENTS ILLUMINATE THE MATRIX - TAP A CELL TO FOCUS IT",
                color = TextDim.copy(alpha = 0.75f),
                fontSize = 8.sp,
                letterSpacing = 1.1.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                for (row in 0 until 6) {
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        for (col in 0 until 4) {
                            val index = row * 4 + col
                            val word = board.getOrElse(index) { "" }
                            val highlighted = current?.phrase == word || current?.phrase?.contains(word) == true
                            val background by animateColorAsState(
                                targetValue = if (highlighted) {
                                    SignalGreen.copy(alpha = 0.85f)
                                } else {
                                    Color(0xB3060606)
                                },
                                animationSpec = tween(220),
                                label = "matrixCell"
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .background(background)
                                    .border(
                                        1.dp,
                                        if (highlighted) SignalGreen else HairlineDim
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        if (word.isNotBlank()) {
                                            controller.itc.publish(word, "MATRIX")
                                        }
                                    }
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = word,
                                    color = if (highlighted) Color.Black else IceWhite.copy(alpha = 0.82f),
                                    fontSize = 8.sp,
                                    lineHeight = 10.sp,
                                    letterSpacing = 0.4.sp,
                                    textAlign = TextAlign.Center,
                                    maxLines = 3
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .border(1.dp, AccentGreen.copy(alpha = 0.5f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { controller.itc.trigger("MANUAL") },
                    contentAlignment = Alignment.Center
                ) {
                    Text("FORCE RESPONSE", color = AccentGreen, fontSize = 9.sp, letterSpacing = 1.4.sp)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .border(1.dp, HairlineDim)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("BACK TO DEVICE", color = TextDim, fontSize = 9.sp, letterSpacing = 1.4.sp)
                }
            }
            Text(
                text = "MATRIX WORD DECK - ${ItcDictionary.size} entries loaded",
                color = AccentCyan.copy(alpha = 0.5f),
                fontSize = 8.sp,
                letterSpacing = 1.sp,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}
