package app.flow.music.di

import android.content.ContentResolver
import android.content.Context
import androidx.room.Room
import app.flow.music.data.db.MusicDatabase
import app.flow.music.data.db.PlaylistDao
import app.flow.music.data.db.SongDao
import app.flow.music.data.db.StatsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Qualifier @Retention(AnnotationRetention.BINARY) annotation class IoDispatcher

@Qualifier @Retention(AnnotationRetention.BINARY) annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): MusicDatabase =
        Room.databaseBuilder(context, MusicDatabase::class.java, MusicDatabase.NAME)
            // The songs table is a disposable mirror of MediaStore and the user
            // tables are tiny, so a failed migration should never block launch —
            // worst case we rescan and the user loses nothing they care about.
            // Revisit if playlists ever stop being cheap to protect.
            .build()

    @Provides fun songDao(db: MusicDatabase): SongDao = db.songDao()

    @Provides fun statsDao(db: MusicDatabase): StatsDao = db.statsDao()

    @Provides fun playlistDao(db: MusicDatabase): PlaylistDao = db.playlistDao()

    @Provides
    fun contentResolver(@ApplicationContext context: Context): ContentResolver =
        context.contentResolver

    @Provides
    @IoDispatcher
    fun ioDispatcher(): CoroutineDispatcher = Dispatchers.IO

    /**
     * Lives as long as the process. Used for work that must outlive any one
     * screen — the library sync, and the flows the whole app subscribes to.
     * A [SupervisorJob] so one failed collector cannot take the others down.
     */
    @Provides
    @Singleton
    @ApplicationScope
    fun applicationScope(@IoDispatcher io: CoroutineDispatcher): CoroutineScope =
        CoroutineScope(SupervisorJob() + io)
}
