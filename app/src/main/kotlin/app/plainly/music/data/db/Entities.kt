package app.plainly.music.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * The library mirror. One row per audio file, keyed by the app's stable
 * [app.plainly.music.domain.ContentKey] rather than MediaStore's id.
 *
 * This table is disposable — it is rebuilt from MediaStore — which is why the
 * user's own data lives in separate tables that reference `contentKey` without
 * a foreign key. A song vanishing from the device must not silently delete the
 * playlist entry pointing at it; if the file comes back, so should its place in
 * the playlist.
 */
@Entity(
    tableName = "songs",
    indices = [
        Index("mediaStoreId", unique = true),
        Index("relativePath"),
    ],
)
data class SongEntity(
    @PrimaryKey val contentKey: Long,
    val mediaStoreId: Long,
    val title: String,
    val artist: String,
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
    val relativePath: String,
    val displayName: String,
)

/** Per-song user state. Absent row means "never favourited, never played". */
@Entity(tableName = "song_stats")
data class SongStatsEntity(
    @PrimaryKey val contentKey: Long,
    @ColumnInfo(defaultValue = "0") val isFavourite: Boolean = false,
    @ColumnInfo(defaultValue = "0") val playCount: Int = 0,
    val lastPlayedAtMs: Long? = null,
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAtMs: Long,
    val updatedAtMs: Long,
)

/**
 * Playlist membership.
 *
 * [position] is an explicit integer rather than relying on insertion order,
 * because playlists are reorderable. The primary key includes it so the same
 * song can legitimately appear twice in one playlist — people do that.
 */
@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "position"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("contentKey")],
)
data class PlaylistSongEntity(
    val playlistId: Long,
    val position: Int,
    val contentKey: Long,
)
