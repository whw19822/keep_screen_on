package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val DarkColorScheme =
  darkColorScheme(
    primary = AmberGlow,
    onPrimary = Color(0xFF261600),
    primaryContainer = AmberDeep,
    onPrimaryContainer = AmberSoftText,
    secondary = EmeraldSignal,
    onSecondary = Color(0xFF002B1D),
    secondaryContainer = EmeraldContainerDark,
    onSecondaryContainer = EmeraldTextDark,
    tertiary = SlateTextSecondaryDark,
    background = ObsidianBg,
    onBackground = SlateTextPrimaryDark,
    surface = ObsidianSurface,
    onSurface = SlateTextPrimaryDark,
    surfaceVariant = ObsidianSurfaceContainer,
    onSurfaceVariant = SlateTextSecondaryDark,
    surfaceContainerLowest = ObsidianBg,
    surfaceContainerLow = ObsidianSurfaceLow,
    surfaceContainer = ObsidianSurfaceContainer,
    surfaceContainerHigh = ObsidianSurfaceHigh,
    outline = ObsidianOutline,
    outlineVariant = ObsidianOutline.copy(alpha = 0.6f),
    error = ErrorDark,
    errorContainer = ErrorContainerDark,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = AmberPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = AmberContainerLight,
    onPrimaryContainer = AmberTextLight,
    secondary = TealSignalLight,
    onSecondary = Color.White,
    secondaryContainer = TealContainerLight,
    onSecondaryContainer = TealTextLight,
    tertiary = SlateTextSecondaryLight,
    background = AlabasterBg,
    onBackground = SlateTextPrimaryLight,
    surface = AlabasterSurface,
    onSurface = SlateTextPrimaryLight,
    surfaceVariant = AlabasterSurfaceContainer,
    onSurfaceVariant = SlateTextSecondaryLight,
    surfaceContainerLowest = AlabasterSurface,
    surfaceContainerLow = AlabasterSurfaceLow,
    surfaceContainer = AlabasterSurfaceContainer,
    surfaceContainerHigh = AlabasterSurfaceHigh,
    outline = AlabasterOutline,
    outlineVariant = AlabasterOutline.copy(alpha = 0.7f),
    error = ErrorLight,
    errorContainer = ErrorContainerLight,
  )

private val AppShapes =
  Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
  )

@Composable
fun ScreenKeepTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as? Activity)?.window
      if (window != null) {
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
      }
    }
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    shapes = AppShapes,
    content = content,
  )
}
