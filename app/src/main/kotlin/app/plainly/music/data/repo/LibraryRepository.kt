package app.plainly.music.data.repo

import app.plainly.music.data.db.SongDao
import app.plainly.music.data.db.StatsDao
import app.plainly.music.data.mediastore.LibrarySync
import app.plainly.music.data.mediastore.MediaStoreWatcher
import app.plainly.music.data.mediastore.toScanned
import app.plainly.music.data.prefs.Settings
import app.plainly.music.data.prefs.SettingsStore
import app.plainly.music.di.ApplicationScope
import app.plainly.music.di.IoDispatcher
import app.plainly.music.domain.LibraryGrouper
import app.plainly.music.domain.LibraryIndex
import app.plainly.music.domain.model.ScannedSong
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The library, as the UI sees it.
 *
 * Room is the durable store; [index] is the read model. Filters are applied
 * here rather than at scan time, so changing "minimum track length" or excluding
 * a folder takes effect immediately instead of requiring a rescan — and
 * un-excluding a folder brings its songs back without touching the disk.
 */
@Singleton
class LibraryRepository @Inject constructor(
    songDao: SongDao,
    settingsStore: SettingsStore,
    private val statsDao: StatsDao,
    private val sync: LibrarySync,
    private val watcher: MediaStoreWatcher,
    @param:ApplicationScope private val scope: CoroutineScope,
    @param:IoDispatcher private val io: CoroutineDispatcher,
) {

    enum class SyncState { Idle, Scanning }

    private val _syncState = MutableStateFlow(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    /**
     * Rebuilt whenever the mirror or the filter settings change. Grouping
     * happens on [io] because it is a few passes over every track in the
     * library and has no business on the main thread.
     */
    val index: StateFlow<LibraryIndex> =
        combine(songDao.observeAll(), settingsStore.settings.map(::FilterSpec).distinctUntilChanged()) { rows, filter ->
            LibraryGrouper.group(rows.map { it.toScanned() }.filter(filter::allows))
        }
            .flowOn(io)
            .stateIn(scope, SharingStarted.Eagerly, LibraryIndex.Empty)

    val favourites: StateFlow<Set<Long>> = statsDao.observeFavourites()
        .map { it.toSet() }
        .stateIn(scope, SharingStarted.Eagerly, emptySet())

    /**
     * Starts watching for library changes and does a first sync. Safe to call
     * repeatedly — [LibrarySync] serialises and the watcher ignores re-registration.
     */
    fun startWatching() {
        watcher.start()
        scope.launch {
            runSync { sync.sync() }
            watcher.changes.collect { runSync { sync.sync() } }
        }
    }

    suspend fun rescan() = runSync { sync.fullRescan() }

    suspend fun setFavourite(contentKey: Long, favourite: Boolean) =
        statsDao.setFavourite(contentKey, favourite)

    suspend fun recordPlay(contentKey: Long, atMs: Long) = statsDao.recordPlay(contentKey, atMs)

    private suspend inline fun runSync(block: () -> LibrarySync.Outcome) {
        _syncState.value = SyncState.Scanning
        try {
            block()
        } finally {
            _syncState.value = SyncState.Idle
        }
    }

    /**
     * The subset of [Settings] that affects which songs are in the library.
     * Extracted so the index only rebuilds when one of *these* changes — not
     * every time the user toggles the theme.
     */
    private data class FilterSpec(
        val minDurationMs: Long,
        val excludedFolders: Set<String>,
    ) {
        constructor(settings: Settings) : this(
            minDurationMs = settings.minDurationSec * 1_000L,
            excludedFolders = settings.excludedFolders,
        )

        fun allows(song: ScannedSong): Boolean {
            if (song.durationMs < minDurationMs) return false
            return excludedFolders.none { song.relativePath.startsWith(it, ignoreCase = true) }
        }
    }
}
