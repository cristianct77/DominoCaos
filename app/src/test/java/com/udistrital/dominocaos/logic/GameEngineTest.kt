package com.udistrital.dominocaos.logic

import com.udistrital.dominocaos.model.BoardTile
import com.udistrital.dominocaos.model.ChaosEvent
import com.udistrital.dominocaos.model.Domino
import com.udistrital.dominocaos.model.FinishReason
import com.udistrital.dominocaos.model.GameState
import com.udistrital.dominocaos.model.GameStatus
import com.udistrital.dominocaos.model.Player
import com.udistrital.dominocaos.model.Side
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameEngineTest {

    private val noEvents = EventGenerator(Random(3), probability = 0.0)
    private val engine = GameEngine(Random(3), noEvents)

    private fun ok(result: MoveResult): GameState = (result as MoveResult.Ok).state

    private fun twoPlayerRoom(): GameState {
        val room = engine.createRoom("ABCD", "a", "Ana", maxPlayers = 2, maxRounds = 10, now = 0L)
        return ok(engine.addPlayer(room, "b", "Beto"))
    }

    @Test
    fun room_acceptsPlayersUntilFull() {
        val full = twoPlayerRoom()
        assertTrue(full.isFull)
        assertEquals(MoveResult.Invalid(MoveError.ROOM_FULL), engine.addPlayer(full, "c", "Caro"))
    }

    @Test
    fun startGame_onlyHostAndDeals7Tiles() {
        val room = twoPlayerRoom()
        assertEquals(MoveResult.Invalid(MoveError.ONLY_HOST), engine.startGame(room, "b"))
        val started = ok(engine.startGame(room, "a"))
        assertEquals(GameStatus.PLAYING, started.status)
        started.players.forEach { assertEquals(7, it.hand.size) }
        assertEquals(14, started.pool.size)
    }

    @Test
    fun playTile_validatesTurnAndUpdatesScore() {
        val s = GameState(
            code = "X", hostId = "a", status = GameStatus.PLAYING,
            players = listOf(
                Player("a", "Ana", listOf(Domino(4, 1), Domino(2, 2))),
                Player("b", "Beto", listOf(Domino(0, 0), Domino(3, 3)))
            ),
            board = listOf(BoardTile(Domino(4, 6))),
            pool = listOf(Domino(5, 5))
        )
        assertEquals(MoveResult.Invalid(MoveError.NOT_YOUR_TURN), engine.playTile(s, "b", Domino(0, 0), Side.LEFT))
        val after = ok(engine.playTile(s, "a", Domino(4, 1), Side.LEFT))
        // Tablero 1|4 6 -> extremos 1 + 6 = 7 (sin bono). Puntos = 5
        assertEquals(5, after.players[0].score)
        assertEquals("b", after.currentPlayer?.id)
        assertEquals(1, after.turnCount)
    }

    @Test
    fun score_bonusWhenEndsAreMultipleOfFive() {
        val board = listOf(BoardTile(Domino(2, 3)), BoardTile(Domino(3, 3)))
        // extremos 2 + 3 = 5 -> bono 5
        assertEquals(5, ScoreCalculator.endsBonus(board))
        assertEquals((6 + 5) * 2, ScoreCalculator.pointsForPlay(Domino(3, 3), board, doubleScore = true))
    }

    @Test
    fun drawTile_onlyWhenNoValidMove() {
        val s = GameState(
            code = "X", hostId = "a", status = GameStatus.PLAYING,
            players = listOf(
                Player("a", "Ana", listOf(Domino(0, 1))),
                Player("b", "Beto", listOf(Domino(3, 3)))
            ),
            board = listOf(BoardTile(Domino(6, 6))),
            pool = listOf(Domino(6, 2))
        )
        val drawn = ok(engine.drawTile(s, "a"))
        assertEquals(2, drawn.players[0].hand.size)
        assertTrue(drawn.pool.isEmpty())
        assertEquals(MoveResult.Invalid(MoveError.MUST_PLAY), engine.drawTile(drawn, "a"))
    }

    @Test
    fun passTurn_onlyWhenPoolIsEmpty_andBlockedGameEnds() {
        val s = GameState(
            code = "X", hostId = "a", status = GameStatus.PLAYING,
            players = listOf(
                Player("a", "Ana", listOf(Domino(0, 1)), score = 10),
                Player("b", "Beto", listOf(Domino(2, 3)), score = 4)
            ),
            board = listOf(BoardTile(Domino(6, 6)))
        )
        assertEquals(MoveResult.Invalid(MoveError.POOL_NOT_EMPTY), engine.passTurn(s.copy(pool = listOf(Domino(5, 5))), "a"))
        assertTrue(engine.isBlocked(s))
        val passed = ok(engine.passTurn(s, "a"))
        assertEquals(GameStatus.FINISHED, passed.status)
        assertEquals(FinishReason.BLOCKED, passed.finishReason)
        assertEquals("a", passed.winnerId)
    }

    @Test
    fun emptyHand_endsGameAndAddsRivalPoints() {
        val s = GameState(
            code = "X", hostId = "a", status = GameStatus.PLAYING,
            players = listOf(
                Player("a", "Ana", listOf(Domino(6, 1))),
                Player("b", "Beto", listOf(Domino(5, 5), Domino(2, 0)))
            ),
            board = listOf(BoardTile(Domino(6, 6))),
            pool = listOf(Domino(4, 4))
        )
        val end = ok(engine.playTile(s, "a", Domino(6, 1), Side.RIGHT))
        assertEquals(GameStatus.FINISHED, end.status)
        assertEquals(FinishReason.EMPTY_HAND, end.finishReason)
        // 7 por la ficha + 12 de la mano de Beto
        assertEquals(19, end.players[0].score)
        assertEquals("a", end.winnerId)
    }

    @Test
    fun maxRounds_endsGame() {
        val s = GameState(
            code = "X", hostId = "a", status = GameStatus.PLAYING, maxRounds = 1,
            players = listOf(
                Player("a", "Ana", listOf(Domino(6, 1), Domino(1, 1))),
                Player("b", "Beto", listOf(Domino(6, 2), Domino(2, 2)))
            ),
            board = listOf(BoardTile(Domino(6, 6))),
            pool = listOf(Domino(4, 4))
        )
        val t1 = ok(engine.playTile(s, "a", Domino(6, 1), Side.RIGHT))
        assertEquals(GameStatus.PLAYING, t1.status)
        val t2 = ok(engine.playTile(t1, "b", Domino(6, 2), Side.LEFT))
        assertEquals(GameStatus.FINISHED, t2.status)
        assertEquals(FinishReason.MAX_ROUNDS, t2.finishReason)
    }

    @Test
    fun tie_isWonByPlayerWithFewerPowersUsed() {
        val winner = ScoreCalculator.determineWinner(
            listOf(
                Player("a", "Ana", score = 30, powersUsed = 2),
                Player("b", "Beto", score = 30, powersUsed = 1)
            )
        )
        assertEquals("b", winner?.id)
    }

    @Test
    fun abandon_givesVictoryToRemainingPlayer() {
        val started = ok(engine.startGame(twoPlayerRoom(), "a"))
        val left = engine.abandon(started, "a")
        assertEquals(GameStatus.FINISHED, left.status)
        assertEquals(FinishReason.ABANDONED, left.finishReason)
        assertEquals("b", left.winnerId)
    }

    @Test
    fun restart_dealsNewHands() {
        val started = ok(engine.startGame(twoPlayerRoom(), "a"))
        val finished = engine.abandon(started, "b")
        val restarted = ok(engine.restart(finished, "a"))
        assertEquals(GameStatus.PLAYING, restarted.status)
        assertNull(restarted.winnerId)
        restarted.players.forEach { assertEquals(0, it.score) }
    }

    @Test
    fun events_stormOfTilesGivesEveryoneATile() {
        val s = GameState(
            players = listOf(Player("a", "Ana", listOf(Domino(1, 1))), Player("b", "Beto")),
            pool = listOf(Domino(2, 2), Domino(3, 3), Domino(4, 4))
        )
        val after = EventGenerator(Random(1)).apply(s, ChaosEvent.TORMENTA_FICHAS)
        assertEquals(2, after.players[0].hand.size)
        assertEquals(1, after.players[1].hand.size)
        assertEquals(1, after.pool.size)
        assertEquals(ChaosEvent.TORMENTA_FICHAS, after.lastEvent)
        assertEquals(1, after.eventCounter)
    }

    @Test
    fun events_taxLeaderAndDoubleScore() {
        val s = GameState(players = listOf(Player("a", "Ana", score = 12), Player("b", "Beto", score = 3)))
        val taxed = EventGenerator(Random(1)).apply(s, ChaosEvent.IMPUESTO_LIDER)
        assertEquals(7, taxed.players[0].score)
        val doubled = EventGenerator(Random(1)).apply(s, ChaosEvent.DOBLE_PUNTAJE)
        assertTrue(doubled.doubleScoreNext)
    }

    @Test
    fun events_alwaysTriggerWithProbabilityOne() {
        val generator = EventGenerator(Random(9), probability = 1.0)
        assertNotNull(generator.roll())
    }
}
