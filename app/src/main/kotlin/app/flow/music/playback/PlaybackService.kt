package app.flow.music.playback

import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionError
import app.flow.music.data.repo.LibraryRepository
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Where playback actually happens.
 *
 * A [MediaLibraryService] rather than a plain `MediaSessionService`: the extra
 * surface is a browse tree, which is what Android Auto and Wear ask for. Adding
 * it later would mean changing the service type — and therefore the session
 * every controller has already bound to — so it costs nothing now and a
 * migration later.
 *
 * Media3 provides the notification, lock-screen controls, headset button
 * handling and audio focus. There is deliberately no custom notification code
 * here; the default provider is better tested than anything hand-rolled and it
 * tracks platform changes for free.
 */
@AndroidEntryPoint
@UnstableApi
class PlaybackService : MediaLibraryService() {

    @Inject lateinit var library: LibraryRepository
    @Inject lateinit var queueStore: QueueStore

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var session: MediaLibrarySession? = null

    override fun onCreate() {
        super.onCreate()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            // Pause when headphones are unplugged. Without this the phone
            // starts blasting the track out of its speaker on a crowded train.
            .setHandleAudioBecomingNoisy(true)
            // Hold a wake lock only while actually playing, so the CPU can sleep
            // between buffer fills but playback does not stutter.
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        session = MediaLibrarySession.Builder(this, player, LibrarySessionCallback())
            .build()

        scope.launch { queueStore.attach(player) }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = session

    /**
     * Swiping the app away should not kill music that is playing — but if the
     * user swiped away a *paused* app, they meant to close it.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = session?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }

    private inner class LibrarySessionCallback : MediaLibrarySession.Callback {
        /**
         * Lets the system resume the last queue from the notification's "play"
         * button after a reboot, without the app ever having been opened.
         */
        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> =
            queueStore.restoreForResumption()

        /**
         * Required for *any* browser to connect — including System UI, which
         * probes for a root before it will offer the resumption tile. Returning
         * an error here makes playback resumption fail silently.
         */
        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> =
            Futures.immediateFuture(LibraryResult.ofItem(BrowseTree.root(), params))

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val all = BrowseTree.children(library.index.value, parentId)
            val from = (page * pageSize).coerceAtMost(all.size)
            val to = (from + pageSize).coerceAtMost(all.size)
            return Futures.immediateFuture(
                LibraryResult.ofItemList(ImmutableList.copyOf(all.subList(from, to)), params),
            )
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String,
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val item = BrowseTree.item(library.index.value, mediaId)
                ?: return Futures.immediateFuture(LibraryResult.ofError(SessionError.ERROR_BAD_VALUE))
            return Futures.immediateFuture(LibraryResult.ofItem(item, null))
        }

        /**
         * Turns whatever a controller sends into something playable.
         *
         * Two distinct cases. A browser (Android Auto) sends a bare browse id
         * with no URI, and expects the rest of its album to queue up behind it.
         * Our own UI sends fully-formed items whose URI was stripped in transit
         * and only needs it put back.
         */
        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
            startIndex: Int,
            startPositionMs: Long,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val single = mediaItems.singleOrNull()
            if (single != null && single.requestMetadata.mediaUri == null) {
                BrowseTree.playableFor(library.index.value, single.mediaId)?.let { (songs, index) ->
                    return Futures.immediateFuture(
                        MediaSession.MediaItemsWithStartPosition(
                            songs.map(MediaItemMapper::toPersistableMediaItem),
                            index,
                            C.TIME_UNSET,
                        ),
                    )
                }
            }
            return Futures.immediateFuture(
                MediaSession.MediaItemsWithStartPosition(
                    mediaItems.map(MediaItemMapper::rehydrate),
                    startIndex,
                    startPositionMs,
                ),
            )
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
        ): ListenableFuture<MutableList<MediaItem>> =
            // Controllers hand back items whose URI was stripped for IPC; put
            // it back from the id before ExoPlayer tries to open them.
            Futures.immediateFuture(mediaItems.map(MediaItemMapper::rehydrate).toMutableList())
    }

    companion object {
        const val SESSION_ID = "flow-music"
    }
}
