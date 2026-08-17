package app.flow.music.domain

import app.flow.music.domain.model.Album
import app.flow.music.domain.model.Artist
import app.flow.music.domain.model.Folder
import app.flow.music.domain.model.Song

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
 *
 * Every derived structure below is `by lazy`, so an index that is built and
 * immediately replaced — which happens repeatedly during the first scan —
 * costs only the constructor.
 */
data class LibraryIndex(
    val songs: List<Song>,
    val albums: List<Album>,
    val artists: List<Artist>,
    val folders: List<Folder>,
) {
    val isEmpty: Boolean get() = songs.isEmpty()

    private val songsByKey: Map<Long, Song> by lazy { songs.associateBy { it.contentKey } }
    private val albumsByKey: Map<Long, Album> by lazy { albums.associateBy { it.key } }
    private val artistsByKey: Map<Long, Artist> by lazy { artists.associateBy { it.key } }
    private val foldersByPath: Map<String, Folder> by lazy { folders.associateBy { it.path } }

    private val songsByAlbum: Map<Long, List<Song>> by lazy {
        songs.groupBy { it.albumKey }
            .mapValues { (_, tracks) -> tracks.sortedBy { it.discTrackOrder } }
    }
    private val songsByArtist: Map<Long, List<Song>> by lazy { songs.groupBy { it.artistKey } }
    private val songsByFolder: Map<String, List<Song>> by lazy { songs.groupBy { it.relativePath } }
    private val albumsByArtist: Map<Long, List<Album>> by lazy {
        albums.groupBy { it.artistKey }
            .mapValues { (_, list) -> list.sortedByDescending { it.year ?: 0 } }
    }

    /**
     * Folded haystacks, parallel to their source lists.
     *
     * Folding inside the search filter would mean a `Normalizer` pass and a
     * fresh string for every song, album and artist on *every keystroke*. Doing
     * it once here turns search into a plain substring scan over prebuilt data.
     */
    private val songHaystack: List<String> by lazy {
        songs.map { "${it.title} ${it.artist} ${it.album}".foldForSearch() }
    }
    private val albumHaystack: List<String> by lazy {
        albums.map { "${it.name} ${it.albumArtist}".foldForSearch() }
    }
    private val artistHaystack: List<String> by lazy {
        artists.map { it.name.foldForSearch() }
    }

    fun song(contentKey: Long): Song? = songsByKey[contentKey]

    /** Resolves in the order given, dropping keys whose file is gone. */
    fun songs(contentKeys: List<Long>): List<Song> = contentKeys.mapNotNull(songsByKey::get)

    fun album(key: Long): Album? = albumsByKey[key]
    fun artist(key: Long): Artist? = artistsByKey[key]
    fun folder(path: String): Folder? = foldersByPath[path]

    /** Album tracks in disc/track order — the order the album is meant to be heard in. */
    fun songsOfAlbum(albumKey: Long): List<Song> = songsByAlbum[albumKey].orEmpty()

    fun songsOfArtist(artistKey: Long): List<Song> = songsByArtist[artistKey].orEmpty()

    fun songsOfFolder(path: String): List<Song> = songsByFolder[path].orEmpty()

    /** Albums credited to an artist, newest-tagged first. */
    fun albumsOfArtist(artistKey: Long): List<Album> = albumsByArtist[artistKey].orEmpty()

    fun search(query: String, limitPerSection: Int = 20): SearchResults {
        val needle = query.foldForSearch()
        if (needle.isBlank()) return SearchResults.Empty

        val matchedSongs = ArrayList<Song>()
        for (i in songs.indices) {
            if (songHaystack[i].contains(needle)) matchedSongs += songs[i]
        }
        val matchedAlbums = ArrayList<Album>()
        for (i in albums.indices) {
            if (albumHaystack[i].contains(needle)) matchedAlbums += albums[i]
        }
        val matchedArtists = ArrayList<Artist>()
        for (i in artists.indices) {
            if (artistHaystack[i].contains(needle)) matchedArtists += artists[i]
        }

        return SearchResults(
            songs = matchedSongs.take(limitPerSection),
            albums = matchedAlbums.take(limitPerSection),
            artists = matchedArtists.take(limitPerSection),
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
