package app.plainly.music.domain.model

/**
 * A grouping of songs. Albums, artists and folders differ only in how they are
 * derived, so they share a shape.
 */
sealed interface SongGroup {
    val name: String
    val songCount: Int
    val durationMs: Long

    /**
     * MediaStore id of a song to pull artwork from. Album art lives in the audio
     * files themselves, so a "cover" is really "one of my tracks' embedded art".
     */
    val artworkSongId: Long?
}

data class Album(
    val key: Long,
    override val name: String,
    val albumArtist: String,
    val artistKey: Long,
    val year: Int?,
    override val songCount: Int,
    override val durationMs: Long,
    override val artworkSongId: Long?,
) : SongGroup

data class Artist(
    val key: Long,
    override val name: String,
    val albumCount: Int,
    override val songCount: Int,
    override val durationMs: Long,
    override val artworkSongId: Long?,
) : SongGroup

data class Folder(
    /** MediaStore `RELATIVE_PATH`, e.g. `"Music/Rock/"`. Also the identity. */
    val path: String,
    override val name: String,
    override val songCount: Int,
    override val durationMs: Long,
    override val artworkSongId: Long?,
) : SongGroup

data class Playlist(
    val id: Long,
    override val name: String,
    override val songCount: Int,
    override val durationMs: Long,
    override val artworkSongId: Long?,
    val createdAtMs: Long,
    val updatedAtMs: Long,
) : SongGroup
