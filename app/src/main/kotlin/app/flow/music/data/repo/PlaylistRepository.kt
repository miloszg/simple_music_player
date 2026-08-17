package app.flow.music.data.repo

import app.flow.music.data.db.PlaylistDao
import app.flow.music.data.db.PlaylistEntity
import app.flow.music.di.ApplicationScope
import app.flow.music.domain.LibraryIndex
import app.flow.music.domain.model.Playlist
import app.flow.music.domain.model.Song
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Playlists, joined against the live library.
 *
 * The database stores nothing but names and ordered `contentKey`s. Everything
 * displayable — track count, total duration, the cover to show — is derived by
 * resolving those keys against [LibraryIndex] here.
 *
 * That indirection is deliberate. A playlist entry survives its file being
 * deleted: the row stays, it just stops resolving, and if the file comes back
 * the entry lights up again in the right position. Storing denormalised titles
 * and durations would mean a playlist slowly filling with stale ghosts.
 */
@Singleton
class PlaylistRepository @Inject constructor(
    private val dao: PlaylistDao,
    library: LibraryRepository,
    @param:ApplicationScope scope: CoroutineScope,
) {
    /** Membership keyed by playlist, in position order. */
    private val membersByPlaylist: Flow<Map<Long, List<Long>>> =
        dao.observeAllMembers().map { rows ->
            rows.groupBy({ it.playlistId }, { it.contentKey })
        }

    val playlists: StateFlow<List<Playlist>> =
        combine(dao.observeAll(), membersByPlaylist, library.index) { entities, members, index ->
            entities.map { entity ->
                entity.toPlaylist(index, members[entity.id].orEmpty())
            }
        }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    /** The songs of one playlist, in order, skipping entries whose file is gone. */
    fun songsOf(playlistId: Long, index: LibraryIndex): Flow<List<Song>> =
        dao.observeMembers(playlistId).map(index::songs)

    fun playlist(playlistId: Long): Flow<Playlist?> =
        playlists.map { list -> list.firstOrNull { it.id == playlistId } }

    suspend fun create(name: String, contentKeys: List<Long> = emptyList(), nowMs: Long): Long {
        val id = dao.insertPlaylist(
            PlaylistEntity(name = name.trim(), createdAtMs = nowMs, updatedAtMs = nowMs),
        )
        if (contentKeys.isNotEmpty()) dao.append(id, contentKeys, nowMs)
        return id
    }

    suspend fun rename(playlistId: Long, name: String, nowMs: Long) =
        dao.rename(playlistId, name.trim(), nowMs)

    suspend fun delete(playlistId: Long) = dao.delete(playlistId)

    /** Appends, allowing duplicates — adding a track twice is a legitimate thing to want. */
    suspend fun add(playlistId: Long, contentKeys: List<Long>, nowMs: Long) =
        dao.append(playlistId, contentKeys, nowMs)

    suspend fun removeAt(playlistId: Long, position: Int, nowMs: Long) {
        val keys = dao.members(playlistId).toMutableList()
        if (position !in keys.indices) return
        keys.removeAt(position)
        dao.replaceMembers(playlistId, keys, nowMs)
    }

    suspend fun move(playlistId: Long, from: Int, to: Int, nowMs: Long) {
        val keys = dao.members(playlistId).toMutableList()
        if (from !in keys.indices || to !in keys.indices) return
        keys.add(to, keys.removeAt(from))
        dao.replaceMembers(playlistId, keys, nowMs)
    }

    private fun PlaylistEntity.toPlaylist(index: LibraryIndex, keys: List<Long>): Playlist {
        val songs = index.songs(keys)
        return Playlist(
            id = id,
            name = name,
            // Counts the songs that actually resolve, so a playlist does not
            // claim 40 tracks when 12 of the files have been deleted.
            songCount = songs.size,
            durationMs = songs.sumOf { it.durationMs },
            artworkSongId = songs.firstOrNull()?.mediaStoreId,
            createdAtMs = createdAtMs,
            updatedAtMs = updatedAtMs,
        )
    }
}
