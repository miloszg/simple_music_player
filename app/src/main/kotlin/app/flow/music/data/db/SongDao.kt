package app.flow.music.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {

    @Query("SELECT * FROM songs")
    suspend fun all(): List<SongEntity>

    /**
     * Emits on every write to the table, so the in-memory index can rebuild
     * itself without the sync having to call back into the repository.
     */
    @Query("SELECT * FROM songs")
    fun observeAll(): Flow<List<SongEntity>>

    @Query("SELECT contentKey, mediaStoreId, dateModifiedSec FROM songs")
    suspend fun fingerprints(): List<StoredFingerprint>

    @Upsert
    suspend fun upsert(songs: List<SongEntity>)

    @Query("DELETE FROM songs WHERE contentKey IN (:contentKeys)")
    suspend fun deleteByKeys(contentKeys: List<Long>)

    @Query("DELETE FROM songs")
    suspend fun clear()

    /**
     * Applies a whole sync diff atomically. Interrupted halfway, an unwrapped
     * version would leave the mirror describing a library that never existed —
     * and the index is rebuilt from a table observer, so the UI would show it.
     */
    @Transaction
    suspend fun applySync(upserts: List<SongEntity>, deletions: List<Long>) {
        // Chunked because both statements bind one variable per key and SQLite
        // caps a statement at SQLITE_MAX_VARIABLE_NUMBER.
        upserts.chunked(SQLITE_VARIABLE_LIMIT).forEach { upsert(it) }
        deletions.chunked(SQLITE_VARIABLE_LIMIT).forEach { deleteByKeys(it) }
    }

    companion object {
        /** Conservative floor for SQLite's `SQLITE_MAX_VARIABLE_NUMBER`. */
        const val SQLITE_VARIABLE_LIMIT = 500
    }
}

data class StoredFingerprint(
    val contentKey: Long,
    val mediaStoreId: Long,
    val dateModifiedSec: Long,
)
