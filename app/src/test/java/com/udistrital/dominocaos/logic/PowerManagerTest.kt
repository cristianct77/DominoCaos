package com.udistrital.dominocaos.logic

import com.udistrital.dominocaos.model.BoardTile
import com.udistrital.dominocaos.model.Domino
import com.udistrital.dominocaos.model.GameState
import com.udistrital.dominocaos.model.GameStatus
import com.udistrital.dominocaos.model.Player
import com.udistrital.dominocaos.model.PowerType
import com.udistrital.dominocaos.model.Side
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PowerManagerTest {

    /** Motor sin eventos aleatorios para que las pruebas sean deterministas. */
    private val engine = GameEngine(Random(7), EventGenerator(Random(7), probability = 0.0))

    private fun state(
        handA: List<Domino>,
        handB: List<Domino>,
        handC: List<Domino>? = null,
        board: List<BoardTile> = listOf(BoardTile(Domino(6, 6)))
    ): GameState {
        val players = mutableListOf(
            Player("a", "Ana", handA),
            Player("b", "Beto", handB)
        )
        if (handC != null) players.add(Player("c", "Caro", handC))
        return GameState(
            code = "TEST", hostId = "a", status = GameStatus.PLAYING,
            maxPlayers = players.size, players = players, board = board,
            pool = listOf(Domino(0, 0)), maxRounds = 10
        )
    }

    private fun ok(result: MoveResult): GameState = (result as MoveResult.Ok).state

    @Test
    fun comodin_allowsPlacingNonMatchingTile() {
        val s = state(listOf(Domino(1, 2), Domino(3, 4)), listOf(Domino(0, 5)))
        assertEquals(
            MoveResult.Invalid(MoveError.INVALID_MOVE),
            engine.playTile(s, "a", Domino(1, 2), Side.RIGHT)
        )
        val withWild = ok(engine.usePower(s, "a", PowerType.COMODIN, null))
        val played = ok(engine.playTile(withWild, "a", Domino(1, 2), Side.RIGHT))
        assertEquals(2, played.board.size)
        assertTrue(played.board.last().wild)
        assertEquals(0, played.players[0].usesOf(PowerType.COMODIN))
        assertEquals(1, played.players[0].powersUsed)
    }

    @Test
    fun salto_nextPlayerLosesTurn() {
        val s = state(listOf(Domino(6, 1), Domino(2, 2)), listOf(Domino(6, 3)), listOf(Domino(6, 4)))
        val skip = ok(engine.usePower(s, "a", PowerType.SALTO, null))
        val after = ok(engine.playTile(skip, "a", Domino(6, 1), Side.RIGHT))
        assertEquals("c", after.currentPlayer?.id) // Beto pierde su turno
    }

    @Test
    fun inversion_changesTurnDirection() {
        val s = state(listOf(Domino(6, 1), Domino(2, 2)), listOf(Domino(6, 3)), listOf(Domino(6, 4)))
        val reversed = ok(engine.usePower(s, "a", PowerType.INVERSION, null))
        assertEquals(-1, reversed.direction)
        val after = ok(engine.playTile(reversed, "a", Domino(6, 1), Side.RIGHT))
        assertEquals("c", after.currentPlayer?.id)
    }

    @Test
    fun intercambio_swapsTileWithOpponent() {
        val s = state(listOf(Domino(1, 2), Domino(3, 4)), listOf(Domino(5, 5)))
        val swapped = ok(engine.usePower(s, "a", PowerType.INTERCAMBIO, Domino(1, 2)))
        assertTrue(swapped.players[0].hand.any { it.sameAs(Domino(5, 5)) })
        assertTrue(swapped.players[1].hand.any { it.sameAs(Domino(1, 2)) })
        assertEquals("a", swapped.currentPlayer?.id) // el turno sigue
    }

    @Test
    fun intercambio_requiresSelectedTile() {
        val s = state(listOf(Domino(1, 2)), listOf(Domino(5, 5)))
        assertEquals(
            MoveResult.Invalid(MoveError.NEEDS_TILE),
            engine.usePower(s, "a", PowerType.INTERCAMBIO, null)
        )
    }

    @Test
    fun powers_haveLimitedUses_andOnlyOnePerTurn() {
        val s = state(listOf(Domino(6, 1), Domino(2, 2)), listOf(Domino(6, 3)))
        val first = ok(engine.usePower(s, "a", PowerType.SALTO, null))
        assertEquals(
            MoveResult.Invalid(MoveError.POWER_ALREADY_USED),
            engine.usePower(first, "a", PowerType.INVERSION, null)
        )
        val noUses = s.copy(
            players = s.players.map { if (it.id == "a") it.copy(powers = it.powers + (PowerType.SALTO to 0)) else it }
        )
        assertEquals(
            MoveResult.Invalid(MoveError.POWER_UNAVAILABLE),
            engine.usePower(noUses, "a", PowerType.SALTO, null)
        )
    }

    @Test
    fun powers_cannotBeUsedOutOfTurn() {
        val s = state(listOf(Domino(6, 1)), listOf(Domino(6, 3)))
        assertEquals(
            MoveResult.Invalid(MoveError.NOT_YOUR_TURN),
            engine.usePower(s, "b", PowerType.SALTO, null)
        )
    }
}
