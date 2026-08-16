package app.plainly.music.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.plainly.music.domain.AlbumSort
import app.plainly.music.domain.ArtistSort
import app.plainly.music.domain.SongSort
import app.plainly.music.domain.SortSpec
import app.plainly.music.ui.theme.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("settings")

/** Everything the user can change, and nothing else. */
data class Settings(
    val themeMode: ThemeMode = ThemeMode.System,
    val dynamicColour: Boolean = true,
    val pureBlack: Boolean = false,
    /**
     * Clips out voice memos, sound effects and album intros. 30s catches almost
     * all of them without hiding short real tracks — punk albums exist.
     */
    val minDurationSec: Int = 30,
    /**
     * Directories excluded from the library, as MediaStore `RELATIVE_PATH`
     * prefixes. `IS_MUSIC` already drops ringtones and alarms; these are the
     * places apps dump audio that is not music.
     */
    val excludedFolders: Set<String> = DEFAULT_EXCLUDED_FOLDERS,
    val songSort: SortSpec<SongSort> = SortSpec(SongSort.Title),
    val albumSort: SortSpec<AlbumSort> = SortSpec(AlbumSort.Title),
    val artistSort: SortSpec<ArtistSort> = SortSpec(ArtistSort.Name),
) {
    companion object {
        val DEFAULT_EXCLUDED_FOLDERS = setOf("Android/", "Recordings/", "WhatsApp/")
    }
}

@Singleton
class SettingsStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    val settings: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            themeMode = prefs[Keys.THEME_MODE].toEnum(ThemeMode.System),
            dynamicColour = prefs[Keys.DYNAMIC_COLOUR] ?: true,
            pureBlack = prefs[Keys.PURE_BLACK] ?: false,
            minDurationSec = prefs[Keys.MIN_DURATION_SEC] ?: 30,
            excludedFolders = prefs[Keys.EXCLUDED_FOLDERS] ?: Settings.DEFAULT_EXCLUDED_FOLDERS,
            songSort = SortSpec(
                prefs[Keys.SONG_SORT].toEnum(SongSort.Title),
                prefs[Keys.SONG_SORT_DESC] ?: false,
            ),
            albumSort = SortSpec(
                prefs[Keys.ALBUM_SORT].toEnum(AlbumSort.Title),
                prefs[Keys.ALBUM_SORT_DESC] ?: false,
            ),
            artistSort = SortSpec(
                prefs[Keys.ARTIST_SORT].toEnum(ArtistSort.Name),
                prefs[Keys.ARTIST_SORT_DESC] ?: false,
            ),
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) = put(Keys.THEME_MODE, mode.name)
    suspend fun setDynamicColour(enabled: Boolean) = put(Keys.DYNAMIC_COLOUR, enabled)
    suspend fun setPureBlack(enabled: Boolean) = put(Keys.PURE_BLACK, enabled)
    suspend fun setMinDurationSec(seconds: Int) = put(Keys.MIN_DURATION_SEC, seconds)
    suspend fun setExcludedFolders(folders: Set<String>) = put(Keys.EXCLUDED_FOLDERS, folders)

    suspend fun setSongSort(spec: SortSpec<SongSort>) = context.dataStore.edit {
        it[Keys.SONG_SORT] = spec.by.name
        it[Keys.SONG_SORT_DESC] = spec.descending
    }

    suspend fun setAlbumSort(spec: SortSpec<AlbumSort>) = context.dataStore.edit {
        it[Keys.ALBUM_SORT] = spec.by.name
        it[Keys.ALBUM_SORT_DESC] = spec.descending
    }

    suspend fun setArtistSort(spec: SortSpec<ArtistSort>) = context.dataStore.edit {
        it[Keys.ARTIST_SORT] = spec.by.name
        it[Keys.ARTIST_SORT_DESC] = spec.descending
    }

    private suspend fun <T> put(key: Preferences.Key<T>, value: T) {
        context.dataStore.edit { it[key] = value }
    }

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOUR = booleanPreferencesKey("dynamic_colour")
        val PURE_BLACK = booleanPreferencesKey("pure_black")
        val MIN_DURATION_SEC = intPreferencesKey("min_duration_sec")
        val EXCLUDED_FOLDERS = stringSetPreferencesKey("excluded_folders")
        val SONG_SORT = stringPreferencesKey("song_sort")
        val SONG_SORT_DESC = booleanPreferencesKey("song_sort_desc")
        val ALBUM_SORT = stringPreferencesKey("album_sort")
        val ALBUM_SORT_DESC = booleanPreferencesKey("album_sort_desc")
        val ARTIST_SORT = stringPreferencesKey("artist_sort")
        val ARTIST_SORT_DESC = booleanPreferencesKey("artist_sort_desc")
    }
}

/**
 * Enum names are stored rather than ordinals, so reordering an enum doesn't
 * silently change what everyone's saved setting means. An unrecognised name
 * (a setting removed in a later version) falls back instead of crashing.
 */
private inline fun <reified T : Enum<T>> String?.toEnum(fallback: T): T =
    this?.let { name -> enumValues<T>().firstOrNull { it.name == name } } ?: fallback
