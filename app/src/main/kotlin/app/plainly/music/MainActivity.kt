package app.plainly.music

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.plainly.music.ui.PermissionGate
import app.plainly.music.ui.PlainlyAppScreen
import app.plainly.music.ui.settings.SettingsViewModel
import app.plainly.music.ui.theme.PlainlyTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Must precede setContent: from targetSdk 35 the system draws behind the
        // bars whether we ask or not, so we opt in explicitly and handle insets.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()

            PlainlyTheme(
                themeMode = settings.themeMode,
                dynamicColor = settings.dynamicColour,
                pureBlack = settings.pureBlack,
            ) {
                PermissionGate {
                    PlainlyAppScreen()
                }
            }
        }
    }
}
