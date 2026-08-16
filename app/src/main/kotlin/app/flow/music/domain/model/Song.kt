package app.flow.music.domain.model

/**
 * One audio file, exactly as MediaStore describes it.
 *
 * This is the shape that gets scanned and persisted. It carries no album or
 * artist identity, because working out which songs belong to the same album
 * needs to see the whole library at once — see `LibraryGrouper`, which turns
 * these into [Song]s.
 *
 * [contentKey] is this app's identity for the file and is what playlists,
 * favourites and play counts reference. [mediaStoreId] is MediaStore's, is only
 * valid until the next media scan, and exists solely to build a content URI.
 */
data class ScannedSong(
    val contentKey: Long,
    val mediaStoreId: Long,
    val title: String,
    val artist: String,
    /** Tagged album artist. Null when the file carries no such tag. */
    val albumArtist: String?,
    val album: String,
    val durationMs: Long,
    val trackNumber: Int?,
    val discNumber: Int?,
    val year: Int?,
    val dateAddedSec: Long,
    val dateModifiedSec: Long,
    val sizeBytes: Long,
    val mimeType: String,
    /** MediaStore `RELATIVE_PATH`, e.g. `"Music/Rock/"`. Always trailing-slashed. */
    val relativePath: String,
    val displayName: String,
)

/** A [ScannedSong] once the grouper has decided which album and artist it belongs to. */
data class Song(
    val scanned: ScannedSong,
    val albumKey: Long,
    val artistKey: Long,
    /** Credited album artist after compilation detection; may differ from the tag. */
    val albumArtistName: String,
) {
    val contentKey: Long get() = scanned.contentKey
    val mediaStoreId: Long get() = scanned.mediaStoreId
    val title: String get() = scanned.title
    val artist: String get() = scanned.artist
    val album: String get() = scanned.album
    val durationMs: Long get() = scanned.durationMs
    val trackNumber: Int? get() = scanned.trackNumber
    val discNumber: Int? get() = scanned.discNumber
    val year: Int? get() = scanned.year
    val dateAddedSec: Long get() = scanned.dateAddedSec
    val relativePath: String get() = scanned.relativePath
    val displayName: String get() = scanned.displayName
    val mimeType: String get() = scanned.mimeType
    val sizeBytes: Long get() = scanned.sizeBytes

    /**
     * Sorts discs before tracks, so a multi-disc album reads 1-1, 1-2, 2-1
     * rather than interleaving. Untagged tracks sort last within their disc.
     */
    val discTrackOrder: Int
        get() = (discNumber ?: 0) * 1000 + (trackNumber ?: 999)
}
