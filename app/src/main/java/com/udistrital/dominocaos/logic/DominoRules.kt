package com.udistrital.dominocaos.logic

import com.udistrital.dominocaos.model.BoardTile
import com.udistrital.dominocaos.model.Domino
import com.udistrital.dominocaos.model.Side
import kotlin.random.Random

/**
 * Reglas básicas del dominó: mazo, reparto y validación de movimientos.
 * No depende de Android, por eso se puede probar con pruebas unitarias.
 */
object DominoRules {

    const val TILES_PER_PLAYER = 7

    /** Crea las 28 fichas del dominó doble seis. */
    fun fullDeck(): List<Domino> {
        val deck = mutableListOf<Domino>()
        for (a in 0..Domino.MAX_VALUE) {
            for (b in a..Domino.MAX_VALUE) {
                deck.add(Domino(a, b))
            }
        }
        return deck
    }

    /**
     * Baraja el mazo y reparte [TILES_PER_PLAYER] fichas a cada jugador.
     * @return par (manos de cada jugador, fichas restantes para el pozo).
     */
    fun deal(playerCount: Int, random: Random): Pair<List<List<Domino>>, List<Domino>> {
        require(playerCount in 2..4) { "Se necesitan entre 2 y 4 jugadores" }
        val deck = fullDeck().shuffled(random)
        val hands = (0 until playerCount).map { i ->
            deck.subList(i * TILES_PER_PLAYER, (i + 1) * TILES_PER_PLAYER).toList()
        }
        val pool = deck.drop(playerCount * TILES_PER_PLAYER)
        return hands to pool
    }

    /** Indica si [tile] puede colocarse en el extremo [side] del tablero. */
    fun canPlace(tile: Domino, board: List<BoardTile>, side: Side): Boolean {
        if (board.isEmpty()) return true
        val end = when (side) {
            Side.LEFT -> board.first().domino.left
            Side.RIGHT -> board.last().domino.right
        }
        return tile.has(end)
    }

    /** Extremos donde [tile] puede colocarse siguiendo las reglas. */
    fun validSides(tile: Domino, board: List<BoardTile>): List<Side> =
        if (board.isEmpty()) listOf(Side.RIGHT)
        else Side.entries.filter { canPlace(tile, board, it) }

    /** Indica si alguna ficha de la mano tiene un movimiento válido. */
    fun hasAnyMove(hand: List<Domino>, board: List<BoardTile>): Boolean =
        hand.any { validSides(it, board).isNotEmpty() }

    /**
     * Coloca [tile] en el tablero devolviendo un nuevo tablero.
     * La ficha se gira para que coincida con el extremo.
     * Si [wild] es true (Comodín) se coloca sin validar el extremo.
     */
    fun place(board: List<BoardTile>, tile: Domino, side: Side, wild: Boolean = false): List<BoardTile> {
        if (board.isEmpty()) return listOf(BoardTile(tile, wild))
        return when (side) {
            Side.LEFT -> {
                val end = board.first().domino.left
                val oriented = when {
                    tile.right == end -> tile
                    tile.left == end -> tile.flipped()
                    else -> tile // comodín: se coloca tal cual
                }
                listOf(BoardTile(oriented, wild)) + board
            }

            Side.RIGHT -> {
                val end = board.last().domino.right
                val oriented = when {
                    tile.left == end -> tile
                    tile.right == end -> tile.flipped()
                    else -> tile
                }
                board + BoardTile(oriented, wild)
            }
        }
    }
}
