package app.plainly.music.data.db

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StatsDao {

    @Query("SELECT contentKey FROM song_stats WHERE isFavourite = 1")
    fun observeFavourites(): Flow<List<Long>>

    @Query("SELECT * FROM song_stats WHERE contentKey = :contentKey")
    fun observe(contentKey: Long): Flow<SongStatsEntity?>

    /**
     * Upserts rather than updates: a song has no stats row until the first time
     * it is favourited or played, which keeps the table proportional to what the
     * user has actually touched instead of to the size of their library.
     */
    @Query(
        """
        INSERT INTO song_stats (contentKey, isFavourite, playCount, lastPlayedAtMs)
        VALUES (:contentKey, :favourite, 0, NULL)
        ON CONFLICT(contentKey) DO UPDATE SET isFavourite = :favourite
        """,
    )
    suspend fun setFavourite(contentKey: Long, favourite: Boolean)

    @Query(
        """
        INSERT INTO song_stats (contentKey, isFavourite, playCount, lastPlayedAtMs)
        VALUES (:contentKey, 0, 1, :atMs)
        ON CONFLICT(contentKey) DO UPDATE SET
            playCount = playCount + 1,
            lastPlayedAtMs = :atMs
        """,
    )
    suspend fun recordPlay(contentKey: Long, atMs: Long)

    @Query("SELECT contentKey FROM song_stats ORDER BY playCount DESC LIMIT :limit")
    suspend fun mostPlayed(limit: Int): List<Long>

    @Query("SELECT contentKey FROM song_stats WHERE lastPlayedAtMs IS NOT NULL ORDER BY lastPlayedAtMs DESC LIMIT :limit")
    suspend fun recentlyPlayed(limit: Int): List<Long>
}
