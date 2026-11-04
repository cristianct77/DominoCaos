package com.udistrital.dominocaos.model

/**
 * Jugador dentro de una partida.
 *
 * @property id uid de Firebase Auth.
 * @property hand fichas en la mano.
 * @property powers usos restantes de cada poder.
 * @property powersUsed total de poderes usados (criterio de desempate).
 */
data class Player(
    val id: String,
    val name: String,
    val hand: List<Domino> = emptyList(),
    val score: Int = 0,
    val powers: Map<PowerType, Int> = PowerType.initialUses(),
    val powersUsed: Int = 0,
    val tilesPlaced: Int = 0,
    val eventsReceived: Int = 0
) {
    /** Usos restantes de un poder. */
    fun usesOf(power: PowerType): Int = powers[power] ?: 0

    /** Suma de puntos que quedan en la mano. */
    val handPoints: Int get() = hand.sumOf { it.points }
}
