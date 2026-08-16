package app.plainly.music.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/** How the app decides between the light and dark palette. */
enum class ThemeMode { System, Light, Dark }

/**
 * Whether Material You wallpaper colours are available on this device.
 * Everything below Android 12 falls back to [FallbackLightColors]/[FallbackDarkColors].
 */
val supportsDynamicColor: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@Composable
fun PlainlyTheme(
    themeMode: ThemeMode = ThemeMode.System,
    dynamicColor: Boolean = true,
    /** Pure black surfaces for OLED panels. Only meaningful in dark mode. */
    pureBlack: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val context = LocalContext.current

    val base = when {
        dynamicColor && supportsDynamicColor ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> FallbackDarkColors
        else -> FallbackLightColors
    }

    val colors = if (dark && pureBlack) base.toPureBlack() else base

    MaterialTheme(
        colorScheme = colors,
        typography = PlainlyTypography,
        shapes = PlainlyShapes,
        content = content,
    )
}

/**
 * Collapses the dark scheme's surface ramp onto true black.
 *
 * The container tones are kept *slightly* apart rather than all set to black —
 * otherwise cards, sheets and the nav bar become indistinguishable and the UI
 * reads as one undifferentiated void.
 */
private fun ColorScheme.toPureBlack(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF090909),
    surfaceContainer = Color(0xFF0F0F0F),
    surfaceContainerHigh = Color(0xFF161616),
    surfaceContainerHighest = Color(0xFF1E1E1E),
)
