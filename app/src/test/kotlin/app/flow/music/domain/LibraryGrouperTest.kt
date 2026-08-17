package app.flow.music.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryGrouperTest {

    @Test
    fun `two bands with the same album title stay separate`() {
        val index = LibraryGrouper.group(
            listOf(
                scannedSong("a", artist = "Deftones", album = "Adrenaline", path = "Music/Deftones/"),
                scannedSong("b", artist = "Zomby", album = "Adrenaline", path = "Music/Zomby/"),
            ),
        )
        assertEquals(2, index.albums.size)
        assertNotEquals(index.songs[0].albumKey, index.songs[1].albumKey)
    }

    @Test
    fun `album artist tag holds a compilation together`() {
        val index = LibraryGrouper.group(
            listOf(
                scannedSong("a", artist = "Portishead", album = "Now 12", albumArtist = "Various Artists"),
                scannedSong("b", artist = "Massive Attack", album = "Now 12", albumArtist = "Various Artists"),
                scannedSong("c", artist = "Tricky", album = "Now 12", albumArtist = "Various Artists"),
            ),
        )
        assertEquals(1, index.albums.size)
        assertEquals("Various Artists", index.albums.single().albumArtist)
        assertEquals(3, index.albums.single().songCount)
    }

    @Test
    fun `untagged compilation in one folder is rescued`() {
        val index = LibraryGrouper.group(
            listOf(
                scannedSong("a", artist = "Portishead", album = "Trip Hop 97", path = "Music/TripHop97/"),
                scannedSong("b", artist = "Massive Attack", album = "Trip Hop 97", path = "Music/TripHop97/"),
            ),
        )
        assertEquals(1, index.albums.size)
        assertEquals(LibraryGrouper.VARIOUS_ARTISTS, index.albums.single().albumArtist)
    }

    @Test
    fun `same album name in different folders is not merged`() {
        // Two unrelated "Greatest Hits" must not fuse just because neither is
        // tagged with an album artist.
        val index = LibraryGrouper.group(
            listOf(
                scannedSong("a", artist = "Queen", album = "Greatest Hits", path = "Music/Queen/"),
                scannedSong("b", artist = "Abba", album = "Greatest Hits", path = "Music/Abba/"),
            ),
        )
        assertEquals(2, index.albums.size)
    }

    @Test
    fun `single artist album in one folder is credited to that artist not Various`() {
        val index = LibraryGrouper.group(
            listOf(
                scannedSong("a", artist = "Slint", album = "Spiderland", path = "Music/Slint/"),
                scannedSong("b", artist = "Slint", album = "Spiderland", path = "Music/Slint/"),
            ),
        )
        assertEquals("Slint", index.albums.single().albumArtist)
    }

    @Test
    fun `MediaStore unknown placeholder is treated as untagged`() {
        val index = LibraryGrouper.group(
            listOf(scannedSong("a", artist = "<unknown>", album = "<unknown>")),
        )
        assertEquals(LibraryGrouper.UNKNOWN_ALBUM, index.albums.single().name)
        assertEquals(LibraryGrouper.UNKNOWN_ARTIST, index.artists.single().name)
    }

    @Test
    fun `accents and case do not split an artist`() {
        val index = LibraryGrouper.group(
            listOf(
                scannedSong("a", artist = "Motörhead", album = "X", path = "Music/M1/"),
                scannedSong("b", artist = "motorhead", album = "Y", path = "Music/M2/"),
            ),
        )
        assertEquals(1, index.artists.size)
        assertEquals(2, index.artists.single().albumCount)
    }

    @Test
    fun `album year is the earliest tagged year`() {
        val index = LibraryGrouper.group(
            listOf(
                scannedSong("a", album = "Loveless", albumArtist = "My Bloody Valentine", year = 2012),
                scannedSong("b", album = "Loveless", albumArtist = "My Bloody Valentine", year = 1991),
            ),
        )
        assertEquals(1991, index.albums.single().year)
    }

    @Test
    fun `zero year counts as untagged`() {
        val index = LibraryGrouper.group(
            listOf(scannedSong("a", album = "X", albumArtist = "Y", year = 0)),
        )
        assertNull(index.albums.single().year)
    }

    @Test
    fun `album artwork comes from the first track not an arbitrary one`() {
        val songs = listOf(
            scannedSong("last", album = "A", albumArtist = "B", track = 9, fileName = "9.mp3"),
            scannedSong("first", album = "A", albumArtist = "B", track = 1, fileName = "1.mp3"),
        )
        val index = LibraryGrouper.group(songs)
        val firstTrack = index.songs.single { it.title == "first" }
        assertEquals(firstTrack.mediaStoreId, index.albums.single().artworkSongId)
    }

    @Test
    fun `multi disc albums order discs before tracks`() {
        val index = LibraryGrouper.group(
            listOf(
                scannedSong("d2t1", album = "A", albumArtist = "B", disc = 2, track = 1, fileName = "21.mp3"),
                scannedSong("d1t2", album = "A", albumArtist = "B", disc = 1, track = 2, fileName = "12.mp3"),
                scannedSong("d1t1", album = "A", albumArtist = "B", disc = 1, track = 1, fileName = "11.mp3"),
            ),
        )
        val ordered = index.songsOfAlbum(index.albums.single().key).map { it.title }
        assertEquals(listOf("d1t1", "d1t2", "d2t1"), ordered)
    }

    @Test
    fun `folders are derived from the relative path`() {
        val index = LibraryGrouper.group(
            listOf(
                scannedSong("a", path = "Music/Rock/"),
                scannedSong("b", path = "Music/Rock/", fileName = "b.mp3"),
                scannedSong("c", path = "Download/"),
            ),
        )
        assertEquals(2, index.folders.size)
        assertEquals(2, index.folder("Music/Rock/")!!.songCount)
        assertEquals("Rock", index.folder("Music/Rock/")!!.name)
    }

    @Test
    fun `empty library produces an empty index`() {
        val index = LibraryGrouper.group(emptyList())
        assertTrue(index.isEmpty)
        assertTrue(index.albums.isEmpty())
        assertTrue(index.artists.isEmpty())
        assertTrue(index.folders.isEmpty())
    }
}
