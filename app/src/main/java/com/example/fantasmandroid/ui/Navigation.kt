package com.example.fantasmandroid.ui

sealed interface Screen {
    data object Device : Screen
    data object Recordings : Screen
    data class Filtered(val kind: RecordingKind) : Screen
    data class Playback(val sessionId: String) : Screen
    data object Settings : Screen
    data object Help : Screen
    data object Legal : Screen
    data object WordLog : Screen
    data object Matrix : Screen
    data object Gallery : Screen
}

enum class RecordingKind { EVP, AUDIO, VIDEO }
