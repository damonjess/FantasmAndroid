package com.example.fantasmandroid

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.fantasmandroid.ui.formatDuration
import com.example.fantasmandroid.ui.formatSessionDate
import java.io.File

fun sessionTranscriptText(session: Session): String {
    val builder = StringBuilder()
    builder.appendLine("FANTASM SESSION ${session.id}")
    builder.appendLine(formatSessionDate(session.startedAt))
    builder.appendLine("Duration ${formatDuration(session.durationMs)}  |  Peak EMF ${(session.peakEmf * 100).toInt()}%")
    builder.appendLine()
    builder.appendLine("ITC TRANSCRIPT")
    if (session.transcript.isEmpty()) {
        builder.appendLine("No ITC responses recorded.")
    } else {
        session.transcript.forEach { word ->
            val offset = ((word.timestamp - session.startedAt) / 1000).coerceAtLeast(0)
            builder.appendLine(
                String.format(
                    java.util.Locale.US,
                    "%02d:%02d  %s  [%s]",
                    offset / 60,
                    offset % 60,
                    word.phrase,
                    word.cause
                )
            )
        }
    }
    builder.appendLine()
    builder.appendLine("Recorded with Fantasm for Android - entertainment purposes only.")
    return builder.toString()
}

fun shareText(context: Context, subject: String, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    runCatching { context.startActivity(Intent.createChooser(intent, "Share")) }
}

fun shareSession(context: Context, session: Session) {
    val files = listOfNotNull(session.videoFile, session.audioFile)
    if (files.isEmpty()) {
        shareText(context, "Fantasm session ${session.id}", sessionTranscriptText(session))
        return
    }
    val uris = files.mapNotNull { file -> fileUri(context, file) }
    if (uris.isEmpty()) {
        shareText(context, "Fantasm session ${session.id}", sessionTranscriptText(session))
        return
    }
    val mime = if (uris.size == 1) {
        if (session.videoFile != null) "video/mp4" else "audio/wav"
    } else {
        "*/*"
    }
    val intent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uris.first())
        }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = mime
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        }
    }
    intent.putExtra(Intent.EXTRA_SUBJECT, "Fantasm session ${session.id}")
    intent.putExtra(Intent.EXTRA_TEXT, sessionTranscriptText(session))
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    runCatching { context.startActivity(Intent.createChooser(intent, "Share session")) }
}

fun shareMediaFile(context: Context, file: File, mime: String) {
    val uri = fileUri(context, file) ?: return
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mime
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching { context.startActivity(Intent.createChooser(intent, "Share")) }
}

private fun fileUri(context: Context, file: File): Uri? = runCatching {
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}.getOrNull()
