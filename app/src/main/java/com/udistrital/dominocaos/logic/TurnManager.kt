package com.udistrital.dominocaos.logic

/** Cálculo del siguiente turno teniendo en cuenta el sentido de juego. */
object TurnManager {

    /**
     * Índice del jugador que está [steps] posiciones después de [current]
     * siguiendo [direction] (1 normal, -1 invertido).
     */
    fun next(current: Int, direction: Int, playerCount: Int, steps: Int = 1): Int {
        if (playerCount <= 0) return 0
        val raw = current + direction * steps
        return ((raw % playerCount) + playerCount) % playerCount
    }
}
