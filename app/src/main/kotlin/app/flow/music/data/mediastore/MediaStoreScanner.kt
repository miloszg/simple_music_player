package app.flow.music.data.mediastore

import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import app.flow.music.domain.ContentKey
import app.flow.music.domain.model.ScannedSong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads the device's audio library out of MediaStore.
 *
 * Two entry points on purpose. [scanFingerprints] pulls only the four columns
 * needed to tell whether anything changed, which is cheap enough to run on
 * every app start and every content-observer ping. [scanFull] does the
 * expensive read, and the sync only asks for the rows that actually moved.
 */
@Singleton
class MediaStoreScanner @Inject constructor(
    private val contentResolver: ContentResolver,
) {

    /** What a row looked like last time, for change detection. */
    data class Fingerprint(
        val contentKey: Long,
        val mediaStoreId: Long,
        val dateModifiedSec: Long,
    )

    fun scanFingerprints(): List<Fingerprint> = query(FINGERPRINT_PROJECTION) { cursor ->
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val modifiedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
        val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.RELATIVE_PATH)
        val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)

        buildList(cursor.count) {
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameCol) ?: continue
                add(
                    Fingerprint(
                        contentKey = ContentKey.of(cursor.getString(pathCol), name),
                        mediaStoreId = cursor.getLong(idCol),
                        dateModifiedSec = cursor.getLong(modifiedCol),
                    ),
                )
            }
        }
    }

    /**
     * Full read of every row. [onlyMediaStoreIds] narrows it to the rows the
     * fingerprint pass flagged; pass null on a cold start to read everything.
     */
    fun scanFull(onlyMediaStoreIds: Set<Long>? = null): List<ScannedSong> {
        if (onlyMediaStoreIds != null && onlyMediaStoreIds.isEmpty()) return emptyList()

        return query(FULL_PROJECTION) { cursor ->
            val cols = FullColumns(cursor)
            buildList(cursor.count) {
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(cols.id)
                    if (onlyMediaStoreIds != null && id !in onlyMediaStoreIds) continue
                    add(cols.read(cursor, id) ?: continue)
                }
            }
        }
    }

    private fun <T> query(projection: Array<String>, read: (Cursor) -> List<T>): List<T> =
        contentResolver.query(collectionUri(), projection, SELECTION, null, null)
            ?.use(read)
            .orEmpty()

    private fun collectionUri(): Uri =
        MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)

    private class FullColumns(cursor: Cursor) {
        val id = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val title = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artist = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val album = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
        val duration = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        val track = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
        val year = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
        val dateAdded = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
        val dateModified = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
        val size = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
        val mime = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
        val relativePath = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.RELATIVE_PATH)
        val displayName = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)

        // Added in API 30. Below that the columns simply aren't in the cursor,
        // and we fall back to inferring the disc from TRACK / leaving the album
        // artist untagged.
        val albumArtist = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ARTIST)
        val disc = cursor.getColumnIndex(MediaStore.Audio.Media.DISC_NUMBER)

        fun read(cursor: Cursor, id: Long): ScannedSong? {
            val displayNameValue = cursor.getString(displayName) ?: return null
            val relativePathValue = normalisePath(cursor.getString(relativePath))
            val rawTrack = cursor.getIntOrNull(track)

            return ScannedSong(
                contentKey = ContentKey.of(relativePathValue, displayNameValue),
                mediaStoreId = id,
                // A file with no TITLE tag shows its filename rather than a
                // blank row; that is what every other player does and it is what
                // lets people find badly-tagged rips at all.
                title = cursor.getString(title).orIfBlank { displayNameValue.substringBeforeLast('.') },
                artist = cursor.getString(artist).orEmpty(),
                albumArtist = albumArtist.takeIf { it >= 0 }?.let { cursor.getString(it) },
                album = cursor.getString(album).orEmpty(),
                durationMs = cursor.getLong(duration),
                trackNumber = rawTrack?.let { discAndTrack(it).second },
                discNumber = disc.takeIf { it >= 0 }?.let { cursor.getIntOrNull(it) }
                    ?: rawTrack?.let { discAndTrack(it).first },
                year = cursor.getIntOrNull(year)?.takeIf { it > 0 },
                dateAddedSec = cursor.getLong(dateAdded),
                dateModifiedSec = cursor.getLong(dateModified),
                sizeBytes = cursor.getLong(size),
                mimeType = cursor.getString(mime).orEmpty(),
                relativePath = relativePathValue,
                displayName = displayNameValue,
            )
        }
    }

    companion object {
        /**
         * `IS_MUSIC` excludes ringtones, notifications and alarms, which the
         * media scanner files under audio but nobody wants in a music library.
         */
        private const val SELECTION = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        private val FINGERPRINT_PROJECTION = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.RELATIVE_PATH,
            MediaStore.Audio.Media.DISPLAY_NAME,
        )

        private val FULL_PROJECTION = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.DISC_NUMBER,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.RELATIVE_PATH,
            MediaStore.Audio.Media.DISPLAY_NAME,
        )

        /** A content URI for a song, given the id MediaStore currently has for it. */
        fun contentUri(mediaStoreId: Long): Uri = ContentUris.withAppendedId(
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL),
            mediaStoreId,
        )

        /**
         * MediaStore mostly hands back `"Music/Rock/"` but is not consistent
         * about the trailing slash, and a null means the volume root. The
         * stable song key is derived from this, so it has to be canonical.
         */
        fun normalisePath(raw: String?): String {
            val trimmed = raw?.replace('\\', '/')?.trim('/').orEmpty()
            return if (trimmed.isEmpty()) "/" else "$trimmed/"
        }

        /**
         * Some taggers pack the disc into TRACK as `dtt` or `dtnn` — 1004 means
         * disc 1, track 4. Values below 1000 are a plain track number.
         */
        fun discAndTrack(raw: Int): Pair<Int?, Int?> = when {
            raw <= 0 -> null to null
            raw >= 1000 -> (raw / 1000) to (raw % 1000).takeIf { it > 0 }
            else -> null to raw
        }
    }
}

private fun Cursor.getIntOrNull(column: Int): Int? =
    if (column < 0 || isNull(column)) null else getInt(column)

private fun String?.orIfBlank(fallback: () -> String): String =
    if (this.isNullOrBlank()) fallback() else this
