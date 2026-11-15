package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val EfoodDarkColorScheme = darkColorScheme(
    primary = EfoodYellow,
    onPrimary = EfoodBlack,
    primaryContainer = EfoodYellowContainer,
    onPrimaryContainer = EfoodYellow,
    secondary = EfoodSuccess,
    onSecondary = EfoodBlack,
    secondaryContainer = EfoodDarkGreen,
    onSecondaryContainer = EfoodWhite,
    tertiary = EfoodYellowDark,
    onTertiary = EfoodBlack,
    background = EfoodBlack,
    onBackground = EfoodWhite,
    surface = EfoodDarkGreen,
    onSurface = EfoodWhite,
    surfaceVariant = EfoodCard,
    onSurfaceVariant = EfoodLightGrey,
    surfaceContainerLowest = EfoodBlack,
    surfaceContainerLow = EfoodDarkGreen,
    surfaceContainer = EfoodCard,
    surfaceContainerHigh = EfoodCardElevated,
    surfaceContainerHighest = EfoodCardElevated,
    outline = EfoodBorder,
    outlineVariant = EfoodBorder,
    error = EfoodError,
    onError = EfoodWhite,
    errorContainer = EfoodErrorContainer,
    onErrorContainer = EfoodError
)

private val EfoodShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun EfoodTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = false
                    isAppearanceLightNavigationBars = false
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = EfoodDarkColorScheme,
        typography = Typography,
        shapes = EfoodShapes,
        content = content
    )
}
