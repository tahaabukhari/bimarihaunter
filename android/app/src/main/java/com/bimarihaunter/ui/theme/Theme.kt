package com.bimarihaunter.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = LimeGreen,
    onPrimary = MidnightBlack,
    primaryContainer = CharcoalGrey,
    onPrimaryContainer = LimeGreen,
    secondary = CharcoalGrey,
    onSecondary = OffWhite,
    secondaryContainer = CharcoalGrey,
    onSecondaryContainer = OffWhite,
    tertiary = TealInfo,
    onTertiary = MidnightBlack,
    tertiaryContainer = CharcoalGrey,
    onTertiaryContainer = TealInfo,
    background = MidnightBlack,
    onBackground = OffWhite,
    surface = MidnightBlack,
    onSurface = OffWhite,
    surfaceVariant = CharcoalGrey,
    onSurfaceVariant = MediumGrey,
    outline = MediumGrey,
    outlineVariant = CharcoalGrey,
    error = EmberRed,
    onError = OffWhite,
    errorContainer = EmberRed,
    onErrorContainer = OffWhite,
    inverseSurface = OffWhite,
    inverseOnSurface = MidnightBlack,
    inversePrimary = LimeGreen
)

private val LightColorScheme = lightColorScheme(
    primary = LightLimeGreen,
    onPrimary = DarkGrey,
    secondary = LightGrey,
    onSecondary = DarkGrey,
    tertiary = TealInfo,
    onTertiary = PureWhite,
    background = PureWhite,
    onBackground = DarkGrey,
    surface = PureWhite,
    onSurface = DarkGrey,
    surfaceVariant = LightGrey,
    onSurfaceVariant = LightMediumGrey,
    error = EmberRed,
    onError = PureWhite
)

@Composable
fun BimarihaunterTheme(
    darkTheme: Boolean = true, // Dark theme is default
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    // Edge-to-edge: only configure appearance, not colors
    // enableEdgeToEdge() in MainActivity makes bars transparent
    // Scaffold handles the insets and fills behind system bars
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Ensure we're drawing behind system bars
            WindowCompat.setDecorFitsSystemWindows(window, false)
            // Set icon appearance (light/dark icons on the transparent bars)
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = BimarihaunterShapes,
        content = content
    )
}
