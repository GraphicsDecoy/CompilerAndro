package com.emergent.codeide.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColors = darkColorScheme(
    primary = VsAccent,
    onPrimary = VsText,
    background = VsBg,
    onBackground = VsText,
    surface = VsPanel,
    onSurface = VsText,
    surfaceVariant = VsSidebar,
    onSurfaceVariant = VsText,
    outline = VsBorder,
)

@Composable
fun CodeIDETheme(
    darkTheme: Boolean = true, // VS Code default dark
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = VsBg.toArgb()
            window.navigationBarColor = VsBg.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
