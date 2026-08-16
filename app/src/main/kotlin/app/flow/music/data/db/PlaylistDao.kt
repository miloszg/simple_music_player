package app.flow.music.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Query("SELECT * FROM playlists ORDER BY updatedAtMs DESC")
    fun observeAll(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    fun observe(id: Long): Flow<PlaylistEntity?>

    @Query("SELECT playlistId, contentKey FROM playlist_songs ORDER BY playlistId, position")
    fun observeAllMembers(): Flow<List<PlaylistMember>>

    @Query("SELECT contentKey FROM playlist_songs WHERE playlistId = :playlistId ORDER BY position")
    fun observeMembers(playlistId: Long): Flow<List<Long>>

    @Query("SELECT contentKey FROM playlist_songs WHERE playlistId = :playlistId ORDER BY position")
    suspend fun members(playlistId: Long): List<Long>

    @Insert
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :name, updatedAtMs = :atMs WHERE id = :id")
    suspend fun rename(id: Long, name: String, atMs: Long)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun delete(id: Long)

    @Insert
    suspend fun insertMembers(members: List<PlaylistSongEntity>)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun clearMembers(playlistId: Long)

    @Query("UPDATE playlists SET updatedAtMs = :atMs WHERE id = :id")
    suspend fun touch(id: Long, atMs: Long)

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun nextPosition(playlistId: Long): Int

    @Transaction
    suspend fun append(playlistId: Long, contentKeys: List<Long>, atMs: Long) {
        val start = nextPosition(playlistId)
        insertMembers(
            contentKeys.mapIndexed { offset, key ->
                PlaylistSongEntity(playlistId, start + offset, key)
            },
        )
        touch(playlistId, atMs)
    }

    /**
     * Rewrites the whole membership list.
     *
     * Reorder and remove both go through here rather than shuffling individual
     * `position` values, because `(playlistId, position)` is the primary key —
     * an in-place move would collide with the row it is moving past. Playlists
     * are small enough that rewriting is cheaper than getting clever.
     */
    @Transaction
    suspend fun replaceMembers(playlistId: Long, contentKeys: List<Long>, atMs: Long) {
        clearMembers(playlistId)
        insertMembers(
            contentKeys.mapIndexed { position, key ->
                PlaylistSongEntity(playlistId, position, key)
            },
        )
        touch(playlistId, atMs)
    }
}

data class PlaylistMember(val playlistId: Long, val contentKey: Long)
