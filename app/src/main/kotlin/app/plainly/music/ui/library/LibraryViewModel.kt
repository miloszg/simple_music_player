package app.plainly.music.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.plainly.music.data.prefs.Settings
import app.plainly.music.data.prefs.SettingsStore
import app.plainly.music.data.repo.LibraryRepository
import app.plainly.music.domain.AlbumSort
import app.plainly.music.domain.ArtistSort
import app.plainly.music.domain.LibraryIndex
import app.plainly.music.domain.SearchResults
import app.plainly.music.domain.SongSort
import app.plainly.music.domain.SortSpec
import app.plainly.music.domain.Sorters
import app.plainly.music.domain.model.Album
import app.plainly.music.domain.model.Artist
import app.plainly.music.domain.model.Folder
import app.plainly.music.domain.model.Song
import app.plainly.music.playback.PlaybackConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The library lists, already sorted, ready to render. */
data class LibraryUiState(
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val folders: List<Folder> = emptyList(),
    val favourites: Set<Long> = emptySet(),
    val isScanning: Boolean = false,
    val songSort: SortSpec<SongSort> = SortSpec(SongSort.Title),
    val albumSort: SortSpec<AlbumSort> = SortSpec(AlbumSort.Title),
    val artistSort: SortSpec<ArtistSort> = SortSpec(ArtistSort.Name),
) {
    val isEmpty: Boolean get() = songs.isEmpty()
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val library: LibraryRepository,
    private val settingsStore: SettingsStore,
    private val playback: PlaybackConnection,
) : ViewModel() {

    /** The raw index, for detail screens that need to resolve keys. */
    val index: StateFlow<LibraryIndex> = library.index

    val uiState: StateFlow<LibraryUiState> = combine(
        library.index,
        settingsStore.settings,
        library.favourites,
        library.syncState,
    ) { index, settings, favourites, syncState ->
        index.toUiState(settings, favourites, syncState)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), LibraryUiState())

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /**
     * Search runs against the in-memory index, so the debounce is only there to
     * avoid re-filtering a large library on every keystroke — not to hide
     * latency. 150ms is below the threshold where typing feels laggy.
     */
    @OptIn(FlowPreview::class)
    val searchResults: StateFlow<SearchResults> =
        combine(_query.debounce(SEARCH_DEBOUNCE_MS), library.index) { query, index ->
            index.search(query)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SearchResults.Empty)

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun start() = library.startWatching()

    fun rescan() = viewModelScope.launch { library.rescan() }

    fun toggleFavourite(song: Song) = viewModelScope.launch {
        library.setFavourite(song.contentKey, song.contentKey !in library.favourites.value)
    }

    fun playAll(songs: List<Song>, startIndex: Int) = playback.play(songs, startIndex)

    fun shuffleAll(songs: List<Song>) = playback.shuffleAll(songs)

    fun playNext(songs: List<Song>) = playback.playNext(songs)

    fun addToQueue(songs: List<Song>) = playback.addToQueue(songs)

    fun setSongSort(spec: SortSpec<SongSort>) = viewModelScope.launch { settingsStore.setSongSort(spec) }

    fun setAlbumSort(spec: SortSpec<AlbumSort>) = viewModelScope.launch { settingsStore.setAlbumSort(spec) }

    fun setArtistSort(spec: SortSpec<ArtistSort>) = viewModelScope.launch { settingsStore.setArtistSort(spec) }

    private fun LibraryIndex.toUiState(
        settings: Settings,
        favourites: Set<Long>,
        syncState: LibraryRepository.SyncState,
    ) = LibraryUiState(
        songs = songs.sortedWith(Sorters.songs(settings.songSort)),
        albums = albums.sortedWith(Sorters.albums(settings.albumSort)),
        artists = artists.sortedWith(Sorters.artists(settings.artistSort)),
        folders = folders,
        favourites = favourites,
        isScanning = syncState == LibraryRepository.SyncState.Scanning,
        songSort = settings.songSort,
        albumSort = settings.albumSort,
        artistSort = settings.artistSort,
    )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val SEARCH_DEBOUNCE_MS = 150L
    }
}
