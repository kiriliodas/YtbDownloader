package app.ytdlclean.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = md_primary,
    onPrimary = md_onPrimary,
    primaryContainer = md_primaryContainer,
    onPrimaryContainer = md_onPrimaryContainer,
    secondary = md_secondary,
    onSecondary = md_onSecondary,
    secondaryContainer = md_secondaryContainer,
    tertiary = md_tertiary,
    onTertiary = md_onTertiary,
    background = md_background,
    onBackground = md_onBackground,
    surface = md_surface,
    onSurface = md_onSurface,
    surfaceVariant = md_surfaceVariant,
    outline = md_outline,
)

private val DarkColors = darkColorScheme(
    primary = md_primaryDark,
    onPrimary = md_onPrimaryDark,
    primaryContainer = md_primaryContainerDark,
    onPrimaryContainer = md_onPrimaryContainerDark,
    secondary = md_secondaryDark,
    background = md_backgroundDark,
    onBackground = md_onBackgroundDark,
    surface = md_surfaceDark,
    onSurface = md_onSurfaceDark,
    surfaceVariant = md_surfaceVariantDark,
    outline = md_outlineDark,
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Composable
fun YtdlTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        androidx.compose.runtime.SideEffect {
            // Guard against the view's context not being an Activity (can happen on
            // some OEM embedding scenarios) — a bad cast here would crash on launch.
            val ctx = view.context
            if (ctx is android.app.Activity) {
                val window = ctx.window
                WindowCompat.setDecorFitsSystemWindows(window, false)
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                window.statusBarColor = android.graphics.Color.TRANSPARENT
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}
