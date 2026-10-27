package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val ARDarkColorScheme = darkColorScheme(
  primary = CyanPrimary,
  onPrimary = Color.Black,
  primaryContainer = CyanPrimaryVariant,
  onPrimaryContainer = Color.White,
  secondary = AmberTorch,
  onSecondary = Color.Black,
  tertiary = CrimsonRecord,
  onTertiary = Color.White,
  background = DarkBackground,
  onBackground = TextPrimary,
  surface = DarkSurface,
  onSurface = TextPrimary,
  surfaceVariant = DarkSurfaceElevated,
  onSurfaceVariant = TextSecondary,
  outline = DarkSurfaceBorder
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Default to dark for AR camera experience
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = ARDarkColorScheme
  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
