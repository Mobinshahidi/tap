package com.tap.apk.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TapColors = darkColorScheme(
    primary = Color(0xFFD57455),
    surface = Color(0xFFC3C2B7),
    background = Color(0xFF1E1E1D),
    onPrimary = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1E1E1D),
    onBackground = Color(0xFFC3C2B7),
)

@Composable
fun TapTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TapColors,
        content = content,
    )
}
