package app.plainly.music.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class SortersTest {

    private val songs = LibraryGrouper.group(
        listOf(
            scannedSong("The Rat", artist = "Walkmen", durationMs = 100, dateAddedSec = 3, fileName = "1.mp3"),
            scannedSong("Anthems", artist = "Broken Social Scene", durationMs = 300, dateAddedSec = 1, fileName = "2.mp3"),
            scannedSong("Bombs", artist = "Faithless", durationMs = 100, dateAddedSec = 2, fileName = "3.mp3"),
        ),
    ).songs

    @Test
    fun `title sort ignores a leading article`() {
        val sorted = songs.sortedWith(Sorters.songs(SortSpec(SongSort.Title))).map { it.title }
        assertEquals(listOf("Anthems", "Bombs", "The Rat"), sorted)
    }

    @Test
    fun `descending reverses the primary key`() {
        val sorted = songs.sortedWith(Sorters.songs(SortSpec(SongSort.Title, descending = true))).map { it.title }
        assertEquals(listOf("The Rat", "Bombs", "Anthems"), sorted)
    }

    @Test
    fun `ties break by title so the order is stable`() {
        // "The Rat" and "Bombs" are both 100ms. Without the tiebreak the list
        // reshuffles between recompositions.
        val sorted = songs.sortedWith(Sorters.songs(SortSpec(SongSort.Duration))).map { it.title }
        assertEquals(listOf("Bombs", "The Rat", "Anthems"), sorted)
    }

    @Test
    fun `descending does not reverse the tiebreak`() {
        // Longest first, but the two equal-length tracks stay A-to-Z.
        val sorted = songs.sortedWith(Sorters.songs(SortSpec(SongSort.Duration, descending = true))).map { it.title }
        assertEquals(listOf("Anthems", "Bombs", "The Rat"), sorted)
    }

    @Test
    fun `date added sort is chronological`() {
        val sorted = songs.sortedWith(Sorters.songs(SortSpec(SongSort.DateAdded))).map { it.title }
        assertEquals(listOf("Anthems", "Bombs", "The Rat"), sorted)
    }
}
