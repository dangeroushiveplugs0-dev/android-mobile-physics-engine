package com.example.physicsengine.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF72D5FF),
    secondary = androidx.compose.ui.graphics.Color(0xFF8CF7B8),
    background = androidx.compose.ui.graphics.Color(0xFF0A1020),
    surface = androidx.compose.ui.graphics.Color(0xFF101A2B)
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content
    )
}
