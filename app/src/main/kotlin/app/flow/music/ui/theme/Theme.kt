package app.flow.music.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/** How the app decides between the light and dark palette. */
enum class ThemeMode { System, Light, Dark }

private val LocalFlowPalette: ProvidableCompositionLocal<FlowPalette> =
    staticCompositionLocalOf { FlowDarkPalette }

/**
 * The brand palette, reachable anywhere as `Flow.colors`.
 *
 * FLOW is not a Material design. Its surfaces are `bg`/`bar`, its rules are
 * fixed-opacity whites, and its accent has exactly two stops — none of which
 * map cleanly onto Material's tonal roles. Screens read this directly and
 * treat [MaterialTheme] only as the thing that keeps stock components
 * (sheets, sliders, ripples) from defaulting to purple.
 */
object Flow {
    val colors: FlowPalette
        @Composable @ReadOnlyComposable get() = LocalFlowPalette.current
}

@Composable
fun FlowTheme(
    themeMode: ThemeMode = ThemeMode.System,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val palette = if (dark) FlowDarkPalette else FlowLightPalette

    CompositionLocalProvider(LocalFlowPalette provides palette) {
        MaterialTheme(
            colorScheme = palette.toColorScheme(dark),
            typography = FlowTypography,
            shapes = FlowShapes,
            content = content,
        )
    }
}
