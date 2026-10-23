package com.example.fantasmandroid.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.fantasmandroid.FantasmController

@Composable
fun FantasmApp(controller: FantasmController) {
    var screen by remember { mutableStateOf<Screen>(Screen.Device) }

    BackHandler(enabled = screen != Screen.Device) {
        screen = when (screen) {
            is Screen.Playback, is Screen.Filtered -> Screen.Recordings
            else -> Screen.Device
        }
    }

    when (val current = screen) {
        Screen.Device -> DeviceScreen(controller) { screen = it }

        Screen.Recordings -> RecordingsScreen(
            controller = controller,
            kind = null,
            onBack = { screen = Screen.Device },
            onOpenSession = { screen = Screen.Playback(it) },
            onOpenKind = { screen = Screen.Filtered(it) }
        )

        is Screen.Filtered -> RecordingsScreen(
            controller = controller,
            kind = current.kind,
            onBack = { screen = Screen.Recordings },
            onOpenSession = { screen = Screen.Playback(it) },
            onOpenKind = { screen = Screen.Filtered(it) }
        )

        is Screen.Playback -> PlaybackScreen(
            controller = controller,
            sessionId = current.sessionId,
            onBack = { screen = Screen.Recordings }
        )

        Screen.Settings -> SettingsScreen(
            controller = controller,
            onBack = { screen = Screen.Device },
            onOpenHelp = { screen = Screen.Help },
            onOpenLegal = { screen = Screen.Legal }
        )

        Screen.Help -> HelpScreen(onBack = { screen = Screen.Settings })

        Screen.Legal -> LegalScreen(onBack = { screen = Screen.Device })

        Screen.WordLog -> WordLogScreen(
            controller = controller,
            onBack = { screen = Screen.Device },
            onOpenMatrix = { screen = Screen.Matrix }
        )

        Screen.Matrix -> MatrixScreen(
            controller = controller,
            onBack = { screen = Screen.Device }
        )

        Screen.Gallery -> GalleryScreen(
            controller = controller,
            onBack = { screen = Screen.Device },
            onOpenSession = { screen = Screen.Playback(it) }
        )
    }
}
