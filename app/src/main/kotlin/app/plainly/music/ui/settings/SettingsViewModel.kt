package app.plainly.music.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.plainly.music.data.prefs.Settings
import app.plainly.music.data.prefs.SettingsStore
import app.plainly.music.data.repo.LibraryRepository
import app.plainly.music.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsStore: SettingsStore,
    private val library: LibraryRepository,
) : ViewModel() {

    /**
     * Eagerly started and seeded with the defaults: this drives the app theme,
     * so a `WhileSubscribed` flow would flash the fallback palette on every
     * configuration change while DataStore re-reads from disk.
     */
    val settings: StateFlow<Settings> =
        settingsStore.settings.stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

    val syncState: StateFlow<LibraryRepository.SyncState> = library.syncState

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { settingsStore.setThemeMode(mode) }

    fun setDynamicColour(enabled: Boolean) = viewModelScope.launch {
        settingsStore.setDynamicColour(enabled)
    }

    fun setPureBlack(enabled: Boolean) = viewModelScope.launch {
        settingsStore.setPureBlack(enabled)
    }

    fun setMinDurationSec(seconds: Int) = viewModelScope.launch {
        settingsStore.setMinDurationSec(seconds)
    }

    fun setExcludedFolders(folders: Set<String>) = viewModelScope.launch {
        settingsStore.setExcludedFolders(folders)
    }

    fun rescan() = viewModelScope.launch { library.rescan() }
}
