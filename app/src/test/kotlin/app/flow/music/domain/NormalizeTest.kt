package app.flow.music.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class NormalizeTest {

    @Test
    fun `folding strips diacritics and case`() {
        assertEquals("motorhead", "Motörhead".foldForSearch())
        assertEquals("bjork", "Björk".foldForSearch())
        assertEquals("sigur ros", "Sigur Rós".foldForSearch())
    }

    @Test
    fun `folding trims surrounding whitespace`() {
        assertEquals("air", "  Air  ".foldForSearch())
    }

    @Test
    fun `sort key drops a leading english article`() {
        assertEquals("beatles", "The Beatles".foldForSort())
        assertEquals("tribe called quest", "A Tribe Called Quest".foldForSort())
        assertEquals("evening with", "An Evening With".foldForSort())
    }

    @Test
    fun `sort key keeps an article that is part of the name`() {
        assertEquals("theatre of hate", "Theatre of Hate".foldForSort())
        assertEquals("answer", "Answer".foldForSort())
    }

    @Test
    fun `index bucket uses the sort key`() {
        assertEquals("B", "The Beatles".indexBucket())
        assertEquals("M", "Motörhead".indexBucket())
    }

    @Test
    fun `non letters bucket together`() {
        assertEquals("#", "65daysofstatic".indexBucket())
        assertEquals("#", "!!!".indexBucket())
        assertEquals("#", "".indexBucket())
        assertEquals("#", "東京".indexBucket())
    }
}
