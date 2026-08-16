package app.flow.music.playback

import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import app.flow.music.data.mediastore.MediaStoreScanner
import app.flow.music.domain.model.Song

/**
 * Translates between the app's [Song] and Media3's [MediaItem].
 *
 * The `mediaId` is the app's stable content key, not MediaStore's id. That is
 * what makes a restored queue survive a media rescan: the URI in a persisted
 * item may be stale, but the id still resolves to the right song.
 */
object MediaItemMapper {

    fun toMediaItem(song: Song): MediaItem = MediaItem.Builder()
        .setMediaId(song.contentKey.toString())
        .setUri(MediaStoreScanner.contentUri(song.mediaStoreId))
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(song.title)
                .setArtist(song.artist)
                .setAlbumTitle(song.album)
                .setAlbumArtist(song.albumArtistName)
                .setTrackNumber(song.trackNumber)
                .setDiscNumber(song.discNumber)
                .setRecordingYear(song.year)
                .setDurationMs(song.durationMs)
                // The notification and lock screen pull artwork from this URI.
                .setArtworkUri(MediaStoreScanner.contentUri(song.mediaStoreId))
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .build(),
        )
        .build()

    /**
     * Restores the playback URI on an item that crossed a process boundary.
     *
     * Media3 strips `localConfiguration` — and with it the URI — when a
     * controller sends items to the session, because the URI may not be
     * meaningful in the receiving process. Ours always is, so we rebuild it.
     */
    fun rehydrate(item: MediaItem): MediaItem {
        if (item.localConfiguration != null) return item
        val uri = item.requestMetadata.mediaUri ?: return item
        return item.buildUpon().setUri(uri).build()
    }

    /** Round-trips through [MediaItem.requestMetadata] so [rehydrate] has something to use. */
    fun toPersistableMediaItem(song: Song): MediaItem = toMediaItem(song).buildUpon()
        .setRequestMetadata(
            MediaItem.RequestMetadata.Builder()
                .setMediaUri(MediaStoreScanner.contentUri(song.mediaStoreId))
                .build(),
        )
        .build()

    fun contentKeyOf(item: MediaItem): Long? = item.mediaId.toLongOrNull()

    fun uriOf(item: MediaItem) = item.localConfiguration?.uri
        ?: item.requestMetadata.mediaUri
        ?: "".toUri()
}
