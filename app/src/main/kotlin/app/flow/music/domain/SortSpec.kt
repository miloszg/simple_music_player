package app.flow.music.domain

import app.flow.music.domain.model.Album
import app.flow.music.domain.model.Artist
import app.flow.music.domain.model.Song

enum class SongSort(val label: String) {
    Title("Title"),
    Artist("Artist"),
    Album("Album"),
    DateAdded("Date added"),
    Duration("Duration"),
}

enum class AlbumSort(val label: String) {
    Title("Title"),
    Artist("Artist"),
    Year("Year"),
    SongCount("Track count"),
}

enum class ArtistSort(val label: String) {
    Name("Name"),
    AlbumCount("Album count"),
    SongCount("Track count"),
}

data class SortSpec<T : Enum<T>>(val by: T, val descending: Boolean = false)

/**
 * Comparators for the library lists.
 *
 * Every one of these ends with a title tiebreak so the order is total: without
 * it, sorting 400 tracks by "Date added" on a library imported in one go leaves
 * the list shuffling between recompositions.
 */
object Sorters {

    fun songs(spec: SortSpec<SongSort>): Comparator<Song> {
        val base: Comparator<Song> = when (spec.by) {
            SongSort.Title -> compareBy { it.title.foldForSort() }
            SongSort.Artist -> compareBy({ it.artist.foldForSort() }, { it.album.foldForSort() }, { it.discTrackOrder })
            SongSort.Album -> compareBy({ it.album.foldForSort() }, { it.discTrackOrder })
            SongSort.DateAdded -> compareBy { it.dateAddedSec }
            SongSort.Duration -> compareBy { it.durationMs }
        }
        return base.thenTitle(spec.descending) { it.title }
    }

    fun albums(spec: SortSpec<AlbumSort>): Comparator<Album> {
        val base: Comparator<Album> = when (spec.by) {
            AlbumSort.Title -> compareBy { it.name.foldForSort() }
            AlbumSort.Artist -> compareBy({ it.albumArtist.foldForSort() }, { it.year ?: 0 })
            AlbumSort.Year -> compareBy { it.year ?: 0 }
            AlbumSort.SongCount -> compareBy { it.songCount }
        }
        return base.thenTitle(spec.descending) { it.name }
    }

    fun artists(spec: SortSpec<ArtistSort>): Comparator<Artist> {
        val base: Comparator<Artist> = when (spec.by) {
            ArtistSort.Name -> compareBy { it.name.foldForSort() }
            ArtistSort.AlbumCount -> compareBy { it.albumCount }
            ArtistSort.SongCount -> compareBy { it.songCount }
        }
        return base.thenTitle(spec.descending) { it.name }
    }

    /**
     * Applies the direction to the primary key only, then breaks ties by title
     * ascending. Reversing the tiebreak too would make "Duration, descending"
     * order equal-length tracks Z-to-A, which reads as a bug.
     */
    private inline fun <T> Comparator<T>.thenTitle(
        descending: Boolean,
        crossinline title: (T) -> String,
    ): Comparator<T> {
        val primary = if (descending) reversed() else this
        return primary.thenBy { title(it).foldForSort() }
    }
}
