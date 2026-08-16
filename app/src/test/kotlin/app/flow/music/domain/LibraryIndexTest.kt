package app.flow.music.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryIndexTest {

    private val index = LibraryGrouper.group(
        listOf(
            scannedSong("Ace of Spades", artist = "Motörhead", album = "Ace of Spades", path = "Music/M/"),
            scannedSong("Love Me Like a Reptile", artist = "Motörhead", album = "Ace of Spades", path = "Music/M/"),
            scannedSong("Hyperballad", artist = "Björk", album = "Post", path = "Music/B/"),
            scannedSong("Army of Me", artist = "Björk", album = "Post", path = "Music/B/"),
        ),
    )

    @Test
    fun `search matches title without the accent`() {
        val results = index.search("hyperballad")
        assertEquals(1, results.songs.size)
        assertEquals("Hyperballad", results.songs.single().title)
    }

    @Test
    fun `search matches an artist typed without diacritics`() {
        val results = index.search("motorhead")
        assertEquals(2, results.songs.size)
        assertEquals(1, results.artists.size)
    }

    @Test
    fun `search matches an album`() {
        val results = index.search("post")
        assertEquals(1, results.albums.size)
        assertEquals("Post", results.albums.single().name)
    }

    @Test
    fun `blank query returns nothing rather than everything`() {
        assertTrue(index.search("").isEmpty)
        assertTrue(index.search("   ").isEmpty)
    }

    @Test
    fun `search reports the true match count even when truncated`() {
        val results = index.search("o", limitPerSection = 1)
        assertEquals(1, results.songs.size)
        assertTrue(results.totalSongMatches > 1)
    }

    @Test
    fun `lookups by key resolve`() {
        val song = index.songs.first()
        assertEquals(song, index.song(song.contentKey))
        assertEquals(2, index.songsOfAlbum(song.albumKey).size)
        assertEquals(2, index.songsOfArtist(song.artistKey).size)
    }

    @Test
    fun `unknown keys resolve to nothing rather than throwing`() {
        assertEquals(null, index.song(-1))
        assertEquals(null, index.album(-1))
        assertTrue(index.songsOfAlbum(-1).isEmpty())
        assertTrue(index.songsOfFolder("nope/").isEmpty())
    }

    @Test
    fun `songs by content key preserves the requested order`() {
        val keys = index.songs.map { it.contentKey }.reversed()
        assertEquals(keys, index.songs(keys).map { it.contentKey })
    }

    @Test
    fun `songs by content key drops keys that are gone`() {
        // A playlist referencing a deleted file must render the songs that
        // remain, not blow up or render blanks.
        val keys = listOf(index.songs.first().contentKey, -999L)
        assertEquals(1, index.songs(keys).size)
    }
}
