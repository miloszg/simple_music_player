package app.flow.music.ui

import java.util.Locale

/** `3:07`, or `1:02:33` for anything over an hour. */
fun formatDuration(millis: Long): String {
    val totalSeconds = (millis.coerceAtLeast(0) + 500) / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
    }
}

/**
 * `12 tracks · 48 min` — the subtitle under an album or playlist.
 *
 * Rounded to whole minutes: nobody cares that a compilation is 48 minutes and
 * 17 seconds long, and the extra digits make the line harder to scan.
 */
fun formatCollectionSummary(songCount: Int, durationMs: Long): String {
    val tracks = if (songCount == 1) "1 track" else "$songCount tracks"
    val minutes = (durationMs / 60_000).toInt()
    val length = when {
        minutes < 1 -> "under a minute"
        minutes < 60 -> "$minutes min"
        else -> {
            val hours = minutes / 60
            val remainder = minutes % 60
            if (remainder == 0) "$hours h" else "$hours h $remainder min"
        }
    }
    return "$tracks · $length"
}
