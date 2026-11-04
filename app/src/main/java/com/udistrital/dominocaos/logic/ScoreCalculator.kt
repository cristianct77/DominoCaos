package com.udistrital.dominocaos.logic

import com.udistrital.dominocaos.model.BoardTile
import com.udistrital.dominocaos.model.Domino
import com.udistrital.dominocaos.model.Player

/**
 * Cálculo de puntaje de Dominó Caos.
 *
 * - Cada ficha colocada suma sus puntos.
 * - Bono "múltiplo de 5": si la suma de los extremos del tablero es múltiplo
 *   de 5 (y mayor que 0) se suma ese valor.
 * - El evento Doble Puntaje duplica el total de la jugada.
 * - Quien se queda sin fichas suma los puntos de las manos rivales.
 */
object ScoreCalculator {

    const val TAX_POINTS = 5

    fun pointsForPlay(tile: Domino, boardAfter: List<BoardTile>, doubleScore: Boolean): Int {
        var points = tile.points + endsBonus(boardAfter)
        if (doubleScore) points *= 2
        return points
    }

    /** Bono cuando la suma de los extremos es múltiplo de 5. */
    fun endsBonus(board: List<BoardTile>): Int {
        if (board.size < 2) return 0
        val sum = board.first().domino.left + board.last().domino.right
        return if (sum > 0 && sum % 5 == 0) sum else 0
    }

    /** Puntos que recibe quien domina: suma de las manos de los rivales. */
    fun dominoBonus(players: List<Player>, winnerId: String): Int =
        players.filter { it.id != winnerId }.sumOf { it.handPoints }

    /**
     * Gana el jugador con mayor puntaje.
     * En caso de empate gana quien haya usado menos poderes.
     */
    fun determineWinner(players: List<Player>): Player? =
        players.sortedWith(
            compareByDescending<Player> { it.score }.thenBy { it.powersUsed }
        ).firstOrNull()
}
