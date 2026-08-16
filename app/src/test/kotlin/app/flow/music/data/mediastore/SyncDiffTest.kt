package app.flow.music.data.mediastore

import app.flow.music.data.db.StoredFingerprint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncDiffTest {

    private fun live(key: Long, id: Long, modified: Long = 100) =
        MediaStoreScanner.Fingerprint(key, id, modified)

    private fun stored(key: Long, id: Long, modified: Long = 100) =
        StoredFingerprint(key, id, modified)

    @Test
    fun `unchanged library is a no-op`() {
        val result = SyncDiff.compute(
            current = listOf(live(1, 10), live(2, 20)),
            stored = listOf(stored(1, 10), stored(2, 20)),
        )
        assertTrue(result.isNoOp)
    }

    @Test
    fun `new file is read`() {
        val result = SyncDiff.compute(
            current = listOf(live(1, 10), live(2, 20)),
            stored = listOf(stored(1, 10)),
        )
        assertEquals(setOf(20L), result.idsToRead)
        assertTrue(result.keysToDelete.isEmpty())
    }

    @Test
    fun `deleted file is removed`() {
        val result = SyncDiff.compute(
            current = listOf(live(1, 10)),
            stored = listOf(stored(1, 10), stored(2, 20)),
        )
        assertTrue(result.idsToRead.isEmpty())
        assertEquals(listOf(2L), result.keysToDelete)
    }

    @Test
    fun `retagged file is re-read`() {
        val result = SyncDiff.compute(
            current = listOf(live(1, 10, modified = 200)),
            stored = listOf(stored(1, 10, modified = 100)),
        )
        assertEquals(setOf(10L), result.idsToRead)
    }

    @Test
    fun `reassigned MediaStore id is re-read but not deleted`() {
        // What a full media rescan does. The song is the same file at the same
        // path; only MediaStore's id moved. Re-read to refresh the id, and
        // crucially do not delete the row — that would take its playlist
        // entries and play count with it.
        val result = SyncDiff.compute(
            current = listOf(live(1, 999)),
            stored = listOf(stored(1, 10)),
        )
        assertEquals(setOf(999L), result.idsToRead)
        assertTrue(result.keysToDelete.isEmpty())
    }

    @Test
    fun `duplicate live rows for one path are read once`() {
        val result = SyncDiff.compute(
            current = listOf(live(1, 10), live(1, 11)),
            stored = emptyList(),
        )
        assertEquals(setOf(10L), result.idsToRead)
    }

    @Test
    fun `a duplicate row does not cause the original to be deleted`() {
        val result = SyncDiff.compute(
            current = listOf(live(1, 10), live(1, 11)),
            stored = listOf(stored(1, 10)),
        )
        assertTrue(result.keysToDelete.isEmpty())
        assertTrue(result.isNoOp)
    }

    @Test
    fun `first sync reads everything`() {
        val result = SyncDiff.compute(
            current = (1L..50L).map { live(it, it * 10) },
            stored = emptyList(),
        )
        assertEquals(50, result.idsToRead.size)
        assertFalse(result.isNoOp)
    }

    @Test
    fun `empty device clears the mirror`() {
        val result = SyncDiff.compute(
            current = emptyList(),
            stored = (1L..5L).map { stored(it, it * 10) },
        )
        assertEquals(5, result.keysToDelete.size)
    }
}
