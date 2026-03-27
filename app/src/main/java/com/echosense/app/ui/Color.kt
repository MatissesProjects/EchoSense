package com.echosense.app.ui

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

val DeepBlue = Color(0xFF0D47A1)
val AccentPurple = Color(0xFF673AB7)
val SignalGreen = Color(0xFF4CAF50)
val SignalRed = Color(0xFFF44336)
val BackgroundBlack = Color(0xFF121212)
val SurfaceDark = Color(0xFF1E1E1E)

val DarkColorScheme = darkColorScheme(
    primary = DeepBlue,
    secondary = AccentPurple,
    tertiary = SignalGreen,
    background = BackgroundBlack,
    surface = SurfaceDark,
    error = SignalRed,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)
