package com.udistrital.dominocaos.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Paleta "Caos nocturno" de Dominó Caos
val Midnight = Color(0xFF0B1026)
val DeepIndigo = Color(0xFF1B1F4B)
val ChaosViolet = Color(0xFF3A1C5C)

val ChaosOrange = Color(0xFFFF7A3D)
val ElectricBlue = Color(0xFF2F80ED)
val NeonViolet = Color(0xFF8B5CF6)
val Mint = Color(0xFF22C55E)
val Danger = Color(0xFFEF4444)
val Gold = Color(0xFFFFD54F)

val FeltDark = Color(0xFF07343A)
val FeltLight = Color(0xFF0F5961)

val TileIvory = Color(0xFFFFF8E7)
val TileShadow = Color(0xFFD9CDB2)
val PipColor = Color(0xFF1F1B2E)

val CardGlass = Color.White.copy(alpha = .10f)
val TextSoft = Color.White.copy(alpha = .75f)

/** Fondo degradado usado en todas las pantallas. */
val ChaosBackground = Brush.verticalGradient(listOf(Midnight, DeepIndigo, ChaosViolet))

/** Paño de la mesa de juego. */
val FeltBackground = Brush.radialGradient(listOf(FeltLight, FeltDark))
