package app.flow.music.playback

import android.content.Context
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import app.flow.music.data.repo.LibraryRepository
import app.flow.music.di.ApplicationScope
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Remembers what was playing.
 *
 * Stored as content keys rather than URIs, so a queue survives the media
 * rescan that would invalidate every MediaStore id in it. Written to a plain
 * file rather than the database because it is one small blob rewritten
 * constantly, which is the one shape SQLite is bad at.
 *
 * Writes are debounced by only persisting on the events that matter — track
 * changes and play/pause — rather than on position updates, which would mean a
 * disk write every frame.
 */
@UnstableApi
@Singleton
class QueueStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val library: LibraryRepository,
    @param:ApplicationScope private val scope: CoroutineScope,
) {

    @Serializable
    private data class Snapshot(
        val contentKeys: List<Long>,
        val index: Int,
        val positionMs: Long,
    )

    private val file: File get() = File(context.filesDir, FILE_NAME)

    /**
     * Restores the last queue into [player] and keeps the file in step from
     * then on. Restores paused: waking a phone to music it started on its own
     * is alarming.
     */
    suspend fun attach(player: Player) {
        read()?.let { snapshot ->
            val songs = library.index.value.songs(snapshot.contentKeys)
            if (songs.isNotEmpty()) {
                player.setMediaItems(
                    songs.map(MediaItemMapper::toPersistableMediaItem),
                    snapshot.index.coerceIn(0, songs.lastIndex),
                    snapshot.positionMs,
                )
                player.prepare()
            }
        }

        player.addListener(
            object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) {
                    if (
                        events.containsAny(
                            Player.EVENT_MEDIA_ITEM_TRANSITION,
                            Player.EVENT_TIMELINE_CHANGED,
                            Player.EVENT_IS_PLAYING_CHANGED,
                        )
                    ) {
                        persist(player)
                    }
                }
            },
        )
    }

    /** Feeds the system's "resume playback" affordance after a reboot. */
    fun restoreForResumption(): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
        val snapshot = readBlocking()
            ?: return Futures.immediateFailedFuture(IllegalStateException("no saved queue"))
        val songs = library.index.value.songs(snapshot.contentKeys)
        if (songs.isEmpty()) {
            return Futures.immediateFailedFuture(IllegalStateException("saved queue no longer resolves"))
        }
        return Futures.immediateFuture(
            MediaSession.MediaItemsWithStartPosition(
                songs.map(MediaItemMapper::toPersistableMediaItem),
                snapshot.index.coerceIn(0, songs.lastIndex),
                snapshot.positionMs,
            ),
        )
    }

    private fun persist(player: Player) {
        val keys = (0 until player.mediaItemCount).mapNotNull {
            MediaItemMapper.contentKeyOf(player.getMediaItemAt(it))
        }
        val snapshot = Snapshot(keys, player.currentMediaItemIndex, player.currentPosition)
        scope.launch { write(snapshot) }
    }

    private suspend fun write(snapshot: Snapshot) = withContext(Dispatchers.IO) {
        runCatching {
            if (snapshot.contentKeys.isEmpty()) file.delete()
            else file.writeText(json.encodeToString(snapshot))
        }
    }

    private suspend fun read(): Snapshot? = withContext(Dispatchers.IO) { readBlocking() }

    private fun readBlocking(): Snapshot? = runCatching {
        if (!file.exists()) null else json.decodeFromString<Snapshot>(file.readText())
    }.getOrNull()

    private companion object {
        const val FILE_NAME = "queue.json"
        val json = Json { ignoreUnknownKeys = true }
    }
}
