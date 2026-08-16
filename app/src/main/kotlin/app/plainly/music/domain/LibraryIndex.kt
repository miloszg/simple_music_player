package app.plainly.music.domain

import app.plainly.music.domain.model.Album
import app.plainly.music.domain.model.Artist
import app.plainly.music.domain.model.Folder
import app.plainly.music.domain.model.Song

/**
 * The whole library, in memory, ready to read.
 *
 * Everything the UI shows — tabs, sorting, grouping, search — is a synchronous
 * operation on this object. That is the point: a 20k-track library is roughly
 * 6 MB of objects, so paying for it once at startup buys instant tab switches
 * and a search box with no perceptible latency, instead of a database round
 * trip on every keystroke and scroll.
 *
 * Immutable. A sync produces a whole new index, which the UI swaps in
 * atomically; nothing ever observes a half-updated library.
 */
data class LibraryIndex(
    val songs: List<Song>,
    val albums: List<Album>,
    val artists: List<Artist>,
    val folders: List<Folder>,
) {
    val isEmpty: Boolean get() = songs.isEmpty()

    private val songsByKey: Map<Long, Song> = songs.associateBy { it.contentKey }
    private val songsByAlbum: Map<Long, List<Song>> by lazy {
        songs.groupBy { it.albumKey }
            .mapValues { (_, tracks) -> tracks.sortedBy { it.discTrackOrder } }
    }
    private val songsByArtist: Map<Long, List<Song>> by lazy {
        songs.groupBy { it.artistKey }
    }
    private val songsByFolder: Map<String, List<Song>> by lazy {
        songs.groupBy { it.relativePath }
    }

    /**
     * Precomputed haystacks, parallel to [songs]. Folding on every keystroke
     * would mean ~80k `Normalizer` calls per character typed; folding once here
     * turns search into a plain substring scan.
     */
    private val searchHaystack: List<String> by lazy {
        songs.map { "${it.title} ${it.artist} ${it.album}".foldForSearch() }
    }

    fun song(contentKey: Long): Song? = songsByKey[contentKey]
    fun songs(contentKeys: List<Long>): List<Song> = contentKeys.mapNotNull(songsByKey::get)

    fun album(key: Long): Album? = albums.firstOrNull { it.key == key }
    fun artist(key: Long): Artist? = artists.firstOrNull { it.key == key }
    fun folder(path: String): Folder? = folders.firstOrNull { it.path == path }

    /** Album tracks in disc/track order — the order the album is meant to be heard in. */
    fun songsOfAlbum(albumKey: Long): List<Song> = songsByAlbum[albumKey].orEmpty()

    fun songsOfArtist(artistKey: Long): List<Song> = songsByArtist[artistKey].orEmpty()

    fun songsOfFolder(path: String): List<Song> = songsByFolder[path].orEmpty()

    /** Albums credited to an artist, newest-tagged first. */
    fun albumsOfArtist(artistKey: Long): List<Album> =
        albums.filter { it.artistKey == artistKey }.sortedByDescending { it.year ?: 0 }

    fun search(query: String, limitPerSection: Int = 20): SearchResults {
        val needle = query.foldForSearch()
        if (needle.isBlank()) return SearchResults.Empty

        val matchedSongs = ArrayList<Song>()
        for (i in songs.indices) {
            if (searchHaystack[i].contains(needle)) matchedSongs += songs[i]
        }

        return SearchResults(
            songs = matchedSongs.take(limitPerSection),
            albums = albums.filter {
                it.name.foldForSearch().contains(needle) ||
                    it.albumArtist.foldForSearch().contains(needle)
            }.take(limitPerSection),
            artists = artists.filter { it.name.foldForSearch().contains(needle) }
                .take(limitPerSection),
            totalSongMatches = matchedSongs.size,
        )
    }

    companion object {
        val Empty = LibraryIndex(emptyList(), emptyList(), emptyList(), emptyList())
    }
}

data class SearchResults(
    val songs: List<Song>,
    val albums: List<Album>,
    val artists: List<Artist>,
    val totalSongMatches: Int,
) {
    val isEmpty: Boolean get() = songs.isEmpty() && albums.isEmpty() && artists.isEmpty()

    companion object {
        val Empty = SearchResults(emptyList(), emptyList(), emptyList(), 0)
    }
}
