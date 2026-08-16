package app.plainly.music.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.plainly.music.data.repo.LibraryRepository
import app.plainly.music.playback.PlaybackConnection
import app.plainly.music.playback.PlaybackState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playback: PlaybackConnection,
    private val library: LibraryRepository,
) : ViewModel() {

    val state: StateFlow<PlaybackState> = playback.state
    val favourites: StateFlow<Set<Long>> = library.favourites

    fun connect() = playback.connect()

    fun positionMs(): Long = playback.positionMs

    fun togglePlayPause() = playback.togglePlayPause()
    fun next() = playback.next()
    fun previous() = playback.previous()
    fun seekTo(ms: Long) = playback.seekTo(ms)
    fun toggleShuffle() = playback.toggleShuffle()
    fun cycleRepeat() = playback.cycleRepeat()
    fun skipTo(index: Int) = playback.skipTo(index)
    fun removeFromQueue(index: Int) = playback.removeFromQueue(index)
    fun moveInQueue(from: Int, to: Int) = playback.moveInQueue(from, to)

    fun toggleFavourite(contentKey: Long) = viewModelScope.launch {
        library.setFavourite(contentKey, contentKey !in library.favourites.value)
    }
}

/**
 * Samples the playback position, but only while the caller is composed.
 *
 * Position is deliberately not part of [PlaybackState]: it changes continuously
 * and putting it in a shared flow would recompose every screen observing
 * playback on every tick. This polls at [POLL_INTERVAL_MS] and stops the moment
 * the surface using it leaves composition, so a backgrounded app does no work.
 *
 * 500ms is coarse for a moving progress bar; the seek bar animates between
 * samples rather than the poll rate being raised.
 */
@Composable
fun rememberPlaybackPosition(
    isPlaying: Boolean,
    read: () -> Long,
): State<Long> = produceState(initialValue = read(), isPlaying, read) {
    while (true) {
        value = read()
        if (!isPlaying) break
        delay(POLL_INTERVAL_MS)
    }
}

private const val POLL_INTERVAL_MS = 500L
