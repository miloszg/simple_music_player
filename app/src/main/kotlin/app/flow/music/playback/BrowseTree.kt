package app.flow.music.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import app.flow.music.domain.LibraryIndex
import app.flow.music.domain.model.Song
import com.google.common.collect.ImmutableList

/**
 * The library as a browsable tree, for clients that cannot show our UI.
 *
 * Android Auto and Wear browse this. So, less obviously, does System UI: after
 * a reboot it connects as a `MediaBrowser` and asks for the root before it will
 * offer the "resume playback" tile. A service that refuses that connection logs
 * `Cannot resume with ComponentInfo{...}` and the tile silently never appears —
 * which is why the tree exists even though the app itself never uses it.
 */
object BrowseTree {

    const val ROOT_ID = "root"
    const val SONGS_ID = "songs"
    const val ALBUMS_ID = "albums"
    private const val ALBUM_PREFIX = "album:"

    fun root(): MediaItem = browsable(ROOT_ID, "Flow")

    fun children(index: LibraryIndex, parentId: String): ImmutableList<MediaItem> = when {
        parentId == ROOT_ID -> ImmutableList.of(
            browsable(SONGS_ID, "Songs"),
            browsable(ALBUMS_ID, "Albums"),
        )

        parentId == SONGS_ID -> ImmutableList.copyOf(index.songs.map(MediaItemMapper::toMediaItem))

        parentId == ALBUMS_ID -> ImmutableList.copyOf(
            index.albums.map { browsable("$ALBUM_PREFIX${it.key}", it.name, it.albumArtist) },
        )

        parentId.startsWith(ALBUM_PREFIX) -> {
            val key = parentId.removePrefix(ALBUM_PREFIX).toLongOrNull()
            ImmutableList.copyOf(
                key?.let { index.songsOfAlbum(it).map(MediaItemMapper::toMediaItem) }.orEmpty(),
            )
        }

        else -> ImmutableList.of()
    }

    /**
     * Resolves a browse id to something playable, plus the queue it sits in.
     *
     * A browser that picks one track out of an album expects the rest of the
     * album to follow, not for playback to stop after three minutes.
     */
    fun playableFor(index: LibraryIndex, mediaId: String): Pair<List<Song>, Int>? = when {
        mediaId.startsWith(ALBUM_PREFIX) ->
            mediaId.removePrefix(ALBUM_PREFIX).toLongOrNull()
                ?.let { index.songsOfAlbum(it) }
                ?.takeIf { it.isNotEmpty() }
                ?.let { it to 0 }

        else -> mediaId.toLongOrNull()?.let(index::song)?.let { song ->
            val queue = index.songsOfAlbum(song.albumKey).ifEmpty { listOf(song) }
            queue to queue.indexOfFirst { it.contentKey == song.contentKey }.coerceAtLeast(0)
        }
    }

    fun item(index: LibraryIndex, mediaId: String): MediaItem? = when {
        mediaId == ROOT_ID -> root()
        mediaId == SONGS_ID -> browsable(SONGS_ID, "Songs")
        mediaId == ALBUMS_ID -> browsable(ALBUMS_ID, "Albums")
        mediaId.startsWith(ALBUM_PREFIX) ->
            mediaId.removePrefix(ALBUM_PREFIX).toLongOrNull()
                ?.let(index::album)
                ?.let { browsable(mediaId, it.name, it.albumArtist) }
        else -> mediaId.toLongOrNull()?.let(index::song)?.let(MediaItemMapper::toMediaItem)
    }

    private fun browsable(id: String, title: String, subtitle: String? = null): MediaItem =
        MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                    .build(),
            )
            .build()
}
