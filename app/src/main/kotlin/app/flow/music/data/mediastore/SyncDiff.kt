package app.flow.music.data.mediastore

import app.flow.music.data.db.StoredFingerprint

/**
 * Decides what a sync has to do, by comparing what MediaStore reports against
 * what the mirror already holds.
 *
 * Kept as a pure function so the interesting cases — a file edited in place, a
 * file whose MediaStore id was reassigned by a rescan, two rows claiming the
 * same path — are testable without a device.
 */
object SyncDiff {

    data class Result(
        /** MediaStore ids that need a full re-read. */
        val idsToRead: Set<Long>,
        /** Content keys no longer present on the device. */
        val keysToDelete: List<Long>,
    ) {
        val isNoOp: Boolean get() = idsToRead.isEmpty() && keysToDelete.isEmpty()
    }

    fun compute(
        current: List<MediaStoreScanner.Fingerprint>,
        stored: List<StoredFingerprint>,
    ): Result {
        val storedByKey = stored.associateBy { it.contentKey }
        val idsToRead = HashSet<Long>()
        val seenKeys = HashSet<Long>(current.size)

        for (row in current) {
            // Two live rows hashing to the same key means MediaStore is showing
            // the same path twice — it happens on volumes that get mounted at
            // two paths. First one wins; re-reading both would just have them
            // overwrite each other.
            if (!seenKeys.add(row.contentKey)) continue

            val known = storedByKey[row.contentKey]
            val unchanged = known != null &&
                known.dateModifiedSec == row.dateModifiedSec &&
                known.mediaStoreId == row.mediaStoreId
            if (!unchanged) idsToRead += row.mediaStoreId
        }

        val keysToDelete = stored.mapNotNull { it.contentKey.takeIf { key -> key !in seenKeys } }

        return Result(idsToRead, keysToDelete)
    }
}
