package app.flow.music.domain

import app.flow.music.domain.model.ScannedSong

/** Builds a scanned row with sensible defaults so tests only state what matters. */
fun scannedSong(
    title: String,
    artist: String = "Some Artist",
    album: String = "Some Album",
    albumArtist: String? = null,
    path: String = "Music/",
    track: Int? = null,
    disc: Int? = null,
    year: Int? = null,
    durationMs: Long = 200_000,
    dateAddedSec: Long = 0,
    fileName: String = "$title.mp3",
): ScannedSong = ScannedSong(
    contentKey = ContentKey.of(path, fileName),
    mediaStoreId = ContentKey.of(path, fileName) and 0x7FFFFFFF,
    title = title,
    artist = artist,
    albumArtist = albumArtist,
    album = album,
    durationMs = durationMs,
    trackNumber = track,
    discNumber = disc,
    year = year,
    dateAddedSec = dateAddedSec,
    dateModifiedSec = 0,
    sizeBytes = 1_000_000,
    mimeType = "audio/mpeg",
    relativePath = path,
    displayName = fileName,
)
