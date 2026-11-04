package com.udistrital.dominocaos.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ChaosColorScheme = darkColorScheme(
    primary = ChaosOrange,
    onPrimary = Color.White,
    secondary = NeonViolet,
    onSecondary = Color.White,
    tertiary = ElectricBlue,
    background = Midnight,
    onBackground = Color.White,
    surface = DeepIndigo,
    onSurface = Color.White,
    error = Danger
)

/** Tema oscuro fijo: el juego conserva su identidad visual en cualquier dispositivo. */
@Composable
fun DominoCaosTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ChaosColorScheme,
        typography = Typography,
        content = content
    )
}
