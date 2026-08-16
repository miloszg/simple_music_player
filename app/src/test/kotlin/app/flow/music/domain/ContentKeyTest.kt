package app.flow.music.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ContentKeyTest {

    @Test
    fun `same path yields same key`() {
        assertEquals(
            ContentKey.of("Music/Rock/", "01 Ace of Spades.mp3"),
            ContentKey.of("Music/Rock/", "01 Ace of Spades.mp3"),
        )
    }

    @Test
    fun `key survives a MediaStore id reassignment`() {
        // The whole point: nothing about the id feeds into the key, so a full
        // media rescan cannot orphan a playlist entry.
        val before = ContentKey.of("Music/", "track.mp3")
        val after = ContentKey.of("Music/", "track.mp3")
        assertEquals(before, after)
    }

    @Test
    fun `slash placement does not matter`() {
        val canonical = ContentKey.canonicalPath("Music/Rock/", "a.mp3")
        assertEquals(canonical, ContentKey.canonicalPath("/Music/Rock", "a.mp3"))
        assertEquals(canonical, ContentKey.canonicalPath("Music/Rock", "a.mp3"))
        assertEquals(canonical, ContentKey.canonicalPath("\\Music\\Rock\\", "a.mp3"))
    }

    @Test
    fun `case differences do not matter`() {
        assertEquals(
            ContentKey.of("Music/Rock/", "Track.MP3"),
            ContentKey.of("music/rock/", "track.mp3"),
        )
    }

    @Test
    fun `decomposed and composed unicode agree`() {
        // "Bjork" with a combining diaeresis vs. the precomposed character.
        // Different volumes hand back different normalisations of the same file.
        val decomposed = "Björk.mp3"
        val composed = "Björk.mp3"
        assertEquals(ContentKey.of("Music/", decomposed), ContentKey.of("Music/", composed))
    }

    @Test
    fun `different directories are different songs`() {
        assertNotEquals(
            ContentKey.of("Music/Rock/", "track.mp3"),
            ContentKey.of("Music/Jazz/", "track.mp3"),
        )
    }

    @Test
    fun `root directory is handled`() {
        assertEquals("track.mp3", ContentKey.canonicalPath(null, "track.mp3"))
        assertEquals("track.mp3", ContentKey.canonicalPath("", "track.mp3"))
    }

    @Test
    fun `key is never zero`() {
        // 0 is reserved as "unset" in the schema.
        val keys = (0..2000).map { ContentKey.of("Music/", "track$it.mp3") }
        assert(keys.none { it == 0L })
    }

    @Test
    fun `keys do not collide across a realistic library`() {
        val keys = buildList {
            for (dir in 0..99) {
                for (track in 0..199) add(ContentKey.of("Music/Album$dir/", "$track title.mp3"))
            }
        }
        assertEquals(keys.size, keys.toSet().size)
    }
}
