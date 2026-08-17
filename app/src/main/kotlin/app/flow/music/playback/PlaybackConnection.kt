package app.flow.music.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import app.flow.music.data.repo.LibraryRepository
import app.flow.music.di.ApplicationScope
import app.flow.music.domain.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class RepeatMode { Off, All, One }

/** Everything a player surface needs, and nothing that changes every frame. */
data class PlaybackState(
    val current: Song? = null,
    val isPlaying: Boolean = false,
    val durationMs: Long = 0,
    val shuffle: Boolean = false,
    val repeat: RepeatMode = RepeatMode.Off,
    val queue: List<Song> = emptyList(),
    val queueIndex: Int = -1,
) {
    val hasSomethingLoaded: Boolean get() = current != null
}

/**
 * The UI's handle on the playback service.
 *
 * Deliberately excludes the playback position. Position changes 60 times a
 * second; putting it in [state] would recompose every screen holding this flow
 * on every frame. Surfaces that need it call [positionMs] on their own cadence
 * and interpolate — see the seek bar.
 */
@UnstableApi
@Singleton
class PlaybackConnection @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val library: LibraryRepository,
    @param:ApplicationScope private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val ready = CompletableDeferred<MediaController>()
    private var controller: MediaController? = null

    /** Live position, for surfaces that are visible. Zero when nothing is loaded. */
    val positionMs: Long get() = controller?.currentPosition ?: 0

    fun connect() {
        if (controller != null || ready.isCompleted) return
        scope.launch { bind() }
    }

    private suspend fun bind() = withContext(Dispatchers.Main) {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener(
            {
                val c = runCatching { future.get() }.getOrNull() ?: return@addListener
                controller = c
                c.addListener(StateListener(c))
                publish(c)
                ready.complete(c)
            },
            /* executor = */ { it.run() },
        )
    }

    /** Plays [songs] starting at [startIndex]. The whole list becomes the queue. */
    fun play(songs: List<Song>, startIndex: Int = 0) = onController { c ->
        if (songs.isEmpty()) return@onController
        c.setMediaItems(
            songs.map(MediaItemMapper::toPersistableMediaItem),
            startIndex.coerceIn(songs.indices),
            0L,
        )
        c.prepare()
        c.play()
    }

    /**
     * Plays [songs] in a random order, starting from a random track.
     *
     * Shuffle is turned on *and* the start index randomised: ExoPlayer's
     * shuffle order keeps the current item first, so without this "shuffle all"
     * would always open with the same song.
     */
    fun shuffleAll(songs: List<Song>) = onController { c ->
        if (songs.isEmpty()) return@onController
        c.shuffleModeEnabled = true
        play(songs, songs.indices.random())
    }

    fun togglePlayPause() = onController { c -> if (c.isPlaying) c.pause() else c.play() }

    fun next() = onController { it.seekToNextMediaItem() }

    /**
     * Restarts the current track if it is more than a few seconds in, which is
     * what every physical transport control has done since the CD player.
     */
    fun previous() = onController { c ->
        if (c.currentPosition > RESTART_THRESHOLD_MS) c.seekTo(0) else c.seekToPreviousMediaItem()
    }

    fun seekTo(positionMs: Long) = onController { it.seekTo(positionMs) }

    fun playNext(songs: List<Song>) = onController { c ->
        c.addMediaItems(
            (c.currentMediaItemIndex + 1).coerceAtMost(c.mediaItemCount),
            songs.map(MediaItemMapper::toPersistableMediaItem),
        )
    }

    fun addToQueue(songs: List<Song>) = onController { c ->
        c.addMediaItems(songs.map(MediaItemMapper::toPersistableMediaItem))
    }

    fun removeFromQueue(index: Int) = onController { c ->
        if (index in 0 until c.mediaItemCount) c.removeMediaItem(index)
    }

    fun moveInQueue(from: Int, to: Int) = onController { c ->
        if (from in 0 until c.mediaItemCount && to in 0 until c.mediaItemCount) {
            c.moveMediaItem(from, to)
        }
    }

    fun skipTo(index: Int) = onController { c ->
        if (index in 0 until c.mediaItemCount) {
            c.seekTo(index, 0)
            c.play()
        }
    }

    fun toggleShuffle() = onController { it.shuffleModeEnabled = !it.shuffleModeEnabled }

    fun cycleRepeat() = onController { c ->
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    private fun onController(block: (MediaController) -> Unit) {
        controller?.let(block) ?: scope.launch { withContext(Dispatchers.Main) { block(ready.await()) } }
    }

    private inner class StateListener(private val c: MediaController) : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            // Rebuilding the queue means resolving every item against the index.
            // onEvents fires several times a second during playback — position
            // discontinuities, buffering, metadata — and the queue changes on
            // almost none of them, so only pay for it when the timeline or the
            // shuffle order actually moved.
            val queueChanged = events.containsAny(
                Player.EVENT_TIMELINE_CHANGED,
                Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
            )
            publish(c, rebuildQueue = queueChanged)
        }
    }

    private fun publish(c: MediaController, rebuildQueue: Boolean = true) {
        val index = library.index.value
        val queue = if (rebuildQueue) {
            (0 until c.mediaItemCount).mapNotNull { position ->
                resolve(index, c.getMediaItemAt(position))
            }
        } else {
            _state.value.queue
        }
        _state.value = PlaybackState(
            current = c.currentMediaItem?.let { resolve(index, it) },
            isPlaying = c.isPlaying,
            // The controller reports TIME_UNSET before the track is prepared;
            // a negative duration makes the seek bar jump.
            durationMs = c.duration.takeIf { it > 0 } ?: 0,
            shuffle = c.shuffleModeEnabled,
            repeat = when (c.repeatMode) {
                Player.REPEAT_MODE_ALL -> RepeatMode.All
                Player.REPEAT_MODE_ONE -> RepeatMode.One
                else -> RepeatMode.Off
            },
            queue = queue,
            queueIndex = c.currentMediaItemIndex,
        )
    }

    private fun resolve(index: app.flow.music.domain.LibraryIndex, item: MediaItem): Song? =
        MediaItemMapper.contentKeyOf(item)?.let(index::song)

    private companion object {
        const val RESTART_THRESHOLD_MS = 3_000L
    }
}
