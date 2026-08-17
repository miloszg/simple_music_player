package app.flow.music.ui.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.flow.music.data.repo.LibraryRepository
import app.flow.music.data.repo.PlaylistRepository
import app.flow.music.domain.model.Playlist
import app.flow.music.domain.model.Song
import app.flow.music.playback.PlaybackConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Which sheet or dialog the playlists UI currently has open. */
sealed interface PlaylistDialog {
    data object None : PlaylistDialog

    /** Naming a new playlist. [songsToAdd] is non-empty when created from a selection. */
    data class Create(val songsToAdd: List<Long> = emptyList()) : PlaylistDialog

    /** Choosing which playlist some songs go into. */
    data class Pick(val songsToAdd: List<Long>) : PlaylistDialog

    data class Rename(val playlist: Playlist) : PlaylistDialog
    data class ConfirmDelete(val playlist: Playlist) : PlaylistDialog
}

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val playlists: PlaylistRepository,
    private val library: LibraryRepository,
    private val playback: PlaybackConnection,
) : ViewModel() {

    val all: StateFlow<List<Playlist>> = playlists.playlists

    val favouriteSongs: StateFlow<List<Song>> =
        combine(library.favourites, library.index) { keys, index ->
            // Favourites have no user-defined order, so present them the way the
            // library is presented: by title.
            index.songs.filter { it.contentKey in keys }.sortedBy { it.title.lowercase() }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    private val _dialog = MutableStateFlow<PlaylistDialog>(PlaylistDialog.None)
    val dialog: StateFlow<PlaylistDialog> = _dialog.asStateFlow()

    fun songsOf(playlistId: Long): StateFlow<List<Song>> =
        playlists.songsOf(playlistId, library.index.value)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    fun playlist(playlistId: Long): StateFlow<Playlist?> =
        playlists.playlist(playlistId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    fun openCreate(songsToAdd: List<Long> = emptyList()) {
        _dialog.value = PlaylistDialog.Create(songsToAdd)
    }

    fun openPicker(songsToAdd: List<Long>) {
        // With no playlists yet, "add to playlist" has nothing to pick from, so
        // go straight to creating one rather than showing an empty chooser.
        _dialog.value = if (all.value.isEmpty()) {
            PlaylistDialog.Create(songsToAdd)
        } else {
            PlaylistDialog.Pick(songsToAdd)
        }
    }

    fun openRename(playlist: Playlist) {
        _dialog.value = PlaylistDialog.Rename(playlist)
    }

    fun openDelete(playlist: Playlist) {
        _dialog.value = PlaylistDialog.ConfirmDelete(playlist)
    }

    fun dismissDialog() {
        _dialog.value = PlaylistDialog.None
    }

    fun create(name: String, songsToAdd: List<Long>) = viewModelScope.launch {
        playlists.create(name, songsToAdd, System.currentTimeMillis())
        dismissDialog()
    }

    fun addTo(playlistId: Long, contentKeys: List<Long>) = viewModelScope.launch {
        playlists.add(playlistId, contentKeys, System.currentTimeMillis())
        dismissDialog()
    }

    fun rename(playlistId: Long, name: String) = viewModelScope.launch {
        playlists.rename(playlistId, name, System.currentTimeMillis())
        dismissDialog()
    }

    fun delete(playlistId: Long) = viewModelScope.launch {
        playlists.delete(playlistId)
        dismissDialog()
    }

    fun removeAt(playlistId: Long, position: Int) = viewModelScope.launch {
        playlists.removeAt(playlistId, position, System.currentTimeMillis())
    }

    fun move(playlistId: Long, from: Int, to: Int) = viewModelScope.launch {
        playlists.move(playlistId, from, to, System.currentTimeMillis())
    }

    fun play(songs: List<Song>, startIndex: Int = 0) = playback.play(songs, startIndex)

    fun shuffle(songs: List<Song>) = playback.shuffleAll(songs)

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
