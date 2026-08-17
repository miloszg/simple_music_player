package app.flow.music.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.flow.music.data.prefs.Settings
import app.flow.music.data.prefs.SettingsStore
import app.flow.music.data.repo.LibraryRepository
import app.flow.music.data.repo.PlaylistRepository
import app.flow.music.domain.AlbumSort
import app.flow.music.domain.ArtistSort
import app.flow.music.domain.LibraryIndex
import app.flow.music.domain.SearchResults
import app.flow.music.domain.SongSort
import app.flow.music.domain.SortSpec
import app.flow.music.domain.Sorters
import app.flow.music.domain.model.Album
import app.flow.music.domain.model.Artist
import app.flow.music.domain.model.Folder
import app.flow.music.domain.model.Song
import app.flow.music.ui.flow.FlowHomeContent
import app.flow.music.ui.flow.FlowLibraryContent
import app.flow.music.playback.PlaybackConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The library lists, already sorted, ready to render.
 *
 * Sorting is expensive on a large library, so this state depends on *only* the
 * two things that can change the order: the index and the sort settings.
 */
data class LibraryUiState(
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val folders: List<Folder> = emptyList(),
    val songSort: SortSpec<SongSort> = SortSpec(SongSort.Title),
    val albumSort: SortSpec<AlbumSort> = SortSpec(AlbumSort.Title),
    val artistSort: SortSpec<ArtistSort> = SortSpec(ArtistSort.Name),
) {
    val isEmpty: Boolean get() = songs.isEmpty()
}

/** Just the sort preferences, so a theme change doesn't invalidate the sorted lists. */
private data class SortSettings(
    val song: SortSpec<SongSort>,
    val album: SortSpec<AlbumSort>,
    val artist: SortSpec<ArtistSort>,
) {
    constructor(s: Settings) : this(s.songSort, s.albumSort, s.artistSort)
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val library: LibraryRepository,
    private val playlists: PlaylistRepository,
    private val settingsStore: SettingsStore,
    private val playback: PlaybackConnection,
) : ViewModel() {

    /** The raw index, for detail screens that resolve keys. */
    val index: StateFlow<LibraryIndex> = library.index

    /**
     * Sorted lists.
     *
     * Favourites and scan state are deliberately *not* inputs here. They used
     * to be, which meant tapping a single heart re-sorted every song, album and
     * artist in the library — three full sorts to repaint one icon. They are
     * exposed separately below and read directly by the rows that need them.
     */
    val uiState: StateFlow<LibraryUiState> = combine(
        library.index,
        settingsStore.settings.map(::SortSettings).distinctUntilChanged(),
    ) { index, sort ->
        LibraryUiState(
            songs = index.songs.sortedWith(Sorters.songs(sort.song)),
            albums = index.albums.sortedWith(Sorters.albums(sort.album)),
            artists = index.artists.sortedWith(Sorters.artists(sort.artist)),
            folders = index.folders,
            songSort = sort.song,
            albumSort = sort.album,
            artistSort = sort.artist,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), LibraryUiState())

    val favourites: StateFlow<Set<Long>> = library.favourites

    /**
     * The FLOW home screen.
     *
     * Hero is the most recently added album; "Recently played" and "Back in
     * rotation" come from play stats. Kept separate from the sorted library
     * lists so a favourite toggle never re-sorts anything.
     */
    val flowHome: StateFlow<FlowHomeContent> = combine(
        library.index,
        library.recentlyPlayed,
        playlists.playlists,
    ) { index, recentKeys, lists ->
        if (index.isEmpty) return@combine FlowHomeContent()
        val byRecency = index.albums.sortedByDescending { album ->
            index.songsOfAlbum(album.key).maxOfOrNull { it.dateAddedSec } ?: 0L
        }
        val hero = byRecency.firstOrNull()
        FlowHomeContent(
            hero = hero,
            heroTrack = hero?.let { index.songsOfAlbum(it.key).firstOrNull() },
            recent = byRecency.take(SHELF_LIMIT),
            playlists = lists.take(SHELF_LIMIT),
            rotation = index.songs(recentKeys).ifEmpty { index.songs.take(4) }.take(4),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), FlowHomeContent())

    val flowLibrary: StateFlow<FlowLibraryContent> = combine(
        library.index,
        library.favourites,
        playlists.playlists,
    ) { index, favs, lists ->
        FlowLibraryContent(
            albums = index.albums,
            playlists = lists,
            artists = index.artists,
            songs = index.songs,
            likedCount = favs.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), FlowLibraryContent())


    val isScanning: StateFlow<Boolean> = library.syncState
        .map { it == LibraryRepository.SyncState.Scanning }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), false)

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /**
     * Search runs against the in-memory index, so the debounce only avoids
     * re-filtering a large library on every keystroke — it is not hiding
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

    fun toggleFavourite(contentKey: Long) = viewModelScope.launch {
        library.setFavourite(contentKey, contentKey !in library.favourites.value)
    }

    fun playAll(songs: List<Song>, startIndex: Int) = playback.play(songs, startIndex)

    fun shuffleAll(songs: List<Song>) = playback.shuffleAll(songs)

    fun playNext(songs: List<Song>) = playback.playNext(songs)

    fun addToQueue(songs: List<Song>) = playback.addToQueue(songs)

    fun setSongSort(spec: SortSpec<SongSort>) = viewModelScope.launch { settingsStore.setSongSort(spec) }

    fun setAlbumSort(spec: SortSpec<AlbumSort>) = viewModelScope.launch { settingsStore.setAlbumSort(spec) }

    fun setArtistSort(spec: SortSpec<ArtistSort>) = viewModelScope.launch { settingsStore.setArtistSort(spec) }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val SEARCH_DEBOUNCE_MS = 150L

        /** A shelf you cannot finish scrolling is a wall; cap it and offer "see all". */
        const val SHELF_LIMIT = 20

    }
}
