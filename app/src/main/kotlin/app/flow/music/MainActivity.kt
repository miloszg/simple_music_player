package app.flow.music

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.flow.music.ui.PermissionGate
import app.flow.music.ui.flow.FlowApp
import app.flow.music.ui.settings.SettingsViewModel
import app.flow.music.ui.theme.Flow
import app.flow.music.ui.theme.FlowTheme
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

            FlowTheme(themeMode = settings.themeMode) {
                // Surface, not a bare Box: it supplies LocalContentColor as well
                // as the background. Without it every unstyled label falls back
                // to black and disappears on this theme.
                Surface(Modifier.fillMaxSize(), color = Flow.colors.bg) {
                    PermissionGate {
                        FlowApp()
                    }
                }
            }
        }
    }
}
