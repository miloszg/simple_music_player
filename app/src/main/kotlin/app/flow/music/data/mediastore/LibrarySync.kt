package app.flow.music.data.mediastore

import app.flow.music.data.db.SongDao
import app.flow.music.data.db.SongEntity
import app.flow.music.di.IoDispatcher
import app.flow.music.domain.model.ScannedSong
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Brings the Room mirror back in line with MediaStore.
 *
 * Cheap by design: a fingerprint pass reads four columns for every row and, in
 * the common case where nothing changed, stops there. Only the rows whose
 * modification time or MediaStore id moved get read in full. On a library of a
 * few thousand tracks a no-op sync is a single cursor and no writes, which is
 * what makes it safe to run on every app start and every content-observer ping.
 */
@Singleton
class LibrarySync @Inject constructor(
    private val scanner: MediaStoreScanner,
    private val songDao: SongDao,
    @param:IoDispatcher private val io: CoroutineDispatcher,
) {
    /** Serialises syncs; a manual rescan racing an observer ping would double-write. */
    private val mutex = Mutex()

    data class Outcome(val updated: Int, val removed: Int) {
        val changedAnything: Boolean get() = updated > 0 || removed > 0
    }

    suspend fun sync(): Outcome = mutex.withLock {
        withContext(io) {
            val diff = SyncDiff.compute(
                current = scanner.scanFingerprints(),
                stored = songDao.fingerprints(),
            )
            if (diff.isNoOp) return@withContext Outcome(0, 0)

            val rows = scanner.scanFull(diff.idsToRead).map(ScannedSong::toEntity)
            songDao.applySync(upserts = rows, deletions = diff.keysToDelete)
            Outcome(updated = rows.size, removed = diff.keysToDelete.size)
        }
    }

    /**
     * Throws away the mirror and rebuilds it. For the "rescan library" button,
     * and for the case where a user has retagged files without the modification
     * time changing — which some tag editors manage to do.
     */
    suspend fun fullRescan(): Outcome = mutex.withLock {
        withContext(io) {
            val rows = scanner.scanFull().map(ScannedSong::toEntity)
            val stale = songDao.fingerprints().map { it.contentKey } - rows.map { it.contentKey }.toSet()
            songDao.applySync(upserts = rows, deletions = stale)
            Outcome(updated = rows.size, removed = stale.size)
        }
    }
}

internal fun ScannedSong.toEntity() = SongEntity(
    contentKey = contentKey,
    mediaStoreId = mediaStoreId,
    title = title,
    artist = artist,
    albumArtist = albumArtist,
    album = album,
    durationMs = durationMs,
    trackNumber = trackNumber,
    discNumber = discNumber,
    year = year,
    dateAddedSec = dateAddedSec,
    dateModifiedSec = dateModifiedSec,
    sizeBytes = sizeBytes,
    mimeType = mimeType,
    relativePath = relativePath,
    displayName = displayName,
)

internal fun SongEntity.toScanned() = ScannedSong(
    contentKey = contentKey,
    mediaStoreId = mediaStoreId,
    title = title,
    artist = artist,
    albumArtist = albumArtist,
    album = album,
    durationMs = durationMs,
    trackNumber = trackNumber,
    discNumber = discNumber,
    year = year,
    dateAddedSec = dateAddedSec,
    dateModifiedSec = dateModifiedSec,
    sizeBytes = sizeBytes,
    mimeType = mimeType,
    relativePath = relativePath,
    displayName = displayName,
)
