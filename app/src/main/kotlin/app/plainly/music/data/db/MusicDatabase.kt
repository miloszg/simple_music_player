package app.plainly.music.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        SongEntity::class,
        SongStatsEntity::class,
        PlaylistEntity::class,
        PlaylistSongEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun statsDao(): StatsDao
    abstract fun playlistDao(): PlaylistDao

    companion object {
        const val NAME = "plainly-music.db"
    }
}
