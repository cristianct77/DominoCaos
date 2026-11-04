package com.udistrital.dominocaos.logic

import com.udistrital.dominocaos.model.ActionType
import com.udistrital.dominocaos.model.Domino
import com.udistrital.dominocaos.model.FinishReason
import com.udistrital.dominocaos.model.GameState
import com.udistrital.dominocaos.model.GameStatus
import com.udistrital.dominocaos.model.LastAction
import com.udistrital.dominocaos.model.Player
import com.udistrital.dominocaos.model.PowerType
import com.udistrital.dominocaos.model.Side
import kotlin.random.Random

/**
 * Motor de Dominó Caos. Orquesta reglas, poderes, eventos, turnos,
 * puntaje y fin de partida. Todas las funciones son puras: reciben un
 * [GameState] y devuelven uno nuevo, por eso la lógica está separada de
 * la interfaz y de Firebase.
 */
class GameEngine(
    private val random: Random = Random.Default,
    private val events: EventGenerator = EventGenerator(random)
) {

    // ---------------------------------------------------------------- Sala

    /** Crea una sala nueva con el anfitrión como primer jugador. */
    fun createRoom(
        code: String,
        hostId: String,
        hostName: String,
        maxPlayers: Int,
        maxRounds: Int,
        now: Long
    ): GameState = GameState(
        code = code,
        hostId = hostId,
        maxPlayers = maxPlayers.coerceIn(MIN_PLAYERS, MAX_PLAYERS),
        maxRounds = maxRounds.coerceAtLeast(1),
        players = listOf(Player(id = hostId, name = hostName)),
        createdAt = now
    )

    /** Agrega un jugador a la sala de espera. */
    fun addPlayer(state: GameState, id: String, name: String): MoveResult {
        if (state.playerById(id) != null) return MoveResult.Ok(state)
        if (state.status != GameStatus.WAITING) return MoveResult.Invalid(MoveError.GAME_NOT_ACTIVE)
        if (state.isFull) return MoveResult.Invalid(MoveError.ROOM_FULL)
        return MoveResult.Ok(state.copy(players = state.players + Player(id = id, name = name)))
    }

    /** Retira a un jugador que aún no ha empezado la partida. */
    fun removePlayer(state: GameState, id: String): GameState =
        state.copy(players = state.players.filterNot { it.id == id })

    /** Reparte fichas e inicia la partida (solo el anfitrión). */
    fun startGame(state: GameState, requesterId: String): MoveResult {
        if (requesterId != state.hostId) return MoveResult.Invalid(MoveError.ONLY_HOST)
        if (state.players.size < MIN_PLAYERS) return MoveResult.Invalid(MoveError.NOT_ENOUGH_PLAYERS)

        val (hands, pool) = DominoRules.deal(state.players.size, random)
        val players = state.players.mapIndexed { i, p ->
            Player(id = p.id, name = p.name, hand = hands[i])
        }
        // Empieza quien tenga el doble más alto (regla clásica); si nadie, el anfitrión.
        val starter = players.indices.maxByOrNull { i ->
            players[i].hand.filter { it.isDouble }.maxOfOrNull { it.left } ?: -1
        }?.takeIf { i -> players[i].hand.any { it.isDouble } } ?: 0

        return MoveResult.Ok(
            state.copy(
                status = GameStatus.PLAYING,
                players = players,
                board = emptyList(),
                pool = pool,
                currentTurn = starter,
                direction = 1,
                turnCount = 0,
                consecutivePasses = 0,
                wildActive = false,
                skipPending = false,
                doubleScoreNext = false,
                powerUsedThisTurn = false,
                lastEvent = null,
                lastAction = LastAction(ActionType.START, players[starter].name),
                winnerId = null,
                finishReason = null
            )
        )
    }

    /** Reinicia la partida con los mismos jugadores. */
    fun restart(state: GameState, requesterId: String): MoveResult =
        when (val result = startGame(state.copy(status = GameStatus.WAITING), requesterId)) {
            is MoveResult.Ok -> MoveResult.Ok(
                result.state.copy(
                    lastAction = LastAction(ActionType.RESTART, state.playerById(requesterId)?.name ?: "")
                )
            )
            is MoveResult.Invalid -> result
        }

    // ------------------------------------------------------------- Jugadas

    /** Coloca [tile] en el extremo [side]. Valida turno, mano y reglas. */
    fun playTile(state: GameState, playerId: String, tile: Domino, side: Side): MoveResult {
        validateTurn(state, playerId)?.let { return MoveResult.Invalid(it) }
        val index = state.currentTurn
        val player = state.players[index]
        val ownTile = player.hand.firstOrNull { it.sameAs(tile) }
            ?: return MoveResult.Invalid(MoveError.TILE_NOT_IN_HAND)

        val wild = state.wildActive && !DominoRules.canPlace(ownTile, state.board, side)
        if (!wild && !DominoRules.canPlace(ownTile, state.board, side)) {
            return MoveResult.Invalid(MoveError.INVALID_MOVE)
        }

        val board = DominoRules.place(state.board, ownTile, side, wild)
        val points = ScoreCalculator.pointsForPlay(ownTile, board, state.doubleScoreNext)
        val updated = player.copy(
            hand = player.hand.filterNot { it.sameAs(ownTile) },
            score = player.score + points,
            tilesPlaced = player.tilesPlaced + 1
        )
        val afterPlay = state.copy(
            players = state.players.toMutableList().also { it[index] = updated },
            board = board,
            wildActive = false,
            doubleScoreNext = false,
            consecutivePasses = 0,
            lastAction = LastAction(ActionType.PLAY, player.name, domino = ownTile)
        )

        if (updated.hand.isEmpty()) return MoveResult.Ok(finish(afterPlay, FinishReason.EMPTY_HAND, updated.id))
        return MoveResult.Ok(endTurn(afterPlay))
    }

    /** Usa un poder antes de mover. */
    fun usePower(state: GameState, playerId: String, power: PowerType, selectedTile: Domino?): MoveResult {
        validateTurn(state, playerId)?.let { return MoveResult.Invalid(it) }
        return PowerManager.apply(state, playerId, power, selectedTile, random)
    }

    /**
     * Roba una ficha del pozo. Solo se permite si el jugador no tiene
     * movimientos válidos. El turno continúa para que pueda jugarla.
     */
    fun drawTile(state: GameState, playerId: String): MoveResult {
        validateTurn(state, playerId)?.let { return MoveResult.Invalid(it) }
        val player = state.players[state.currentTurn]
        if (DominoRules.hasAnyMove(player.hand, state.board)) return MoveResult.Invalid(MoveError.MUST_PLAY)
        if (state.pool.isEmpty()) return MoveResult.Invalid(MoveError.POOL_EMPTY)

        val drawn = state.pool.first()
        val updated = player.copy(hand = player.hand + drawn)
        return MoveResult.Ok(
            state.copy(
                players = state.players.toMutableList().also { it[state.currentTurn] = updated },
                pool = state.pool.drop(1),
                lastAction = LastAction(ActionType.DRAW, player.name)
            )
        )
    }

    /** Pierde el turno: solo si no puede jugar y el pozo está vacío. */
    fun passTurn(state: GameState, playerId: String): MoveResult {
        validateTurn(state, playerId)?.let { return MoveResult.Invalid(it) }
        val player = state.players[state.currentTurn]
        if (DominoRules.hasAnyMove(player.hand, state.board)) return MoveResult.Invalid(MoveError.MUST_PLAY)
        if (state.pool.isNotEmpty()) return MoveResult.Invalid(MoveError.POOL_NOT_EMPTY)

        val passed = state.copy(
            consecutivePasses = state.consecutivePasses + 1,
            wildActive = false,
            lastAction = LastAction(ActionType.PASS, player.name)
        )
        if (passed.consecutivePasses >= passed.players.size) {
            return MoveResult.Ok(finish(passed, FinishReason.BLOCKED))
        }
        return MoveResult.Ok(endTurn(passed))
    }

    /** Un jugador abandona la partida: gana el mejor de los que quedan. */
    fun abandon(state: GameState, playerId: String): GameState {
        val leaver = state.playerById(playerId) ?: return state
        val remaining = state.players.filterNot { it.id == playerId }
        val winner = ScoreCalculator.determineWinner(remaining)
        return state.copy(
            status = GameStatus.FINISHED,
            finishReason = FinishReason.ABANDONED,
            winnerId = winner?.id,
            lastAction = LastAction(ActionType.LEAVE, leaver.name)
        )
    }

    // ------------------------------------------------------------- Turnos

    /**
     * Cierra el turno actual: avanza el turno (aplicando Salto), revisa
     * fin de partida por rondas o bloqueo y genera eventos aleatorios.
     */
    internal fun endTurn(state: GameState): GameState {
        val size = state.players.size
        val steps = if (state.skipPending) 2 else 1
        var next = state.copy(
            currentTurn = TurnManager.next(state.currentTurn, state.direction, size, steps),
            turnCount = state.turnCount + 1,
            skipPending = false,
            wildActive = false,
            powerUsedThisTurn = false
        )

        if (next.round > next.maxRounds) return finish(next, FinishReason.MAX_ROUNDS)
        if (isBlocked(next)) return finish(next, FinishReason.BLOCKED)

        events.roll()?.let { event -> next = events.apply(next, event) }
        return next
    }

    /** Nadie puede jugar y no quedan fichas en el pozo. */
    fun isBlocked(state: GameState): Boolean =
        state.board.isNotEmpty() && state.pool.isEmpty() &&
            state.players.none { DominoRules.hasAnyMove(it.hand, state.board) }

    /** Detecta el fin de la partida y calcula el ganador. */
    internal fun finish(state: GameState, reason: FinishReason, dominatorId: String? = null): GameState {
        var players = state.players
        if (reason == FinishReason.EMPTY_HAND && dominatorId != null) {
            val bonus = ScoreCalculator.dominoBonus(players, dominatorId)
            players = players.map { if (it.id == dominatorId) it.copy(score = it.score + bonus) else it }
        }
        val winner = ScoreCalculator.determineWinner(players)
        return state.copy(
            players = players,
            status = GameStatus.FINISHED,
            finishReason = reason,
            winnerId = winner?.id
        )
    }

    private fun validateTurn(state: GameState, playerId: String): MoveError? = when {
        state.status != GameStatus.PLAYING -> MoveError.GAME_NOT_ACTIVE
        !state.isTurnOf(playerId) -> MoveError.NOT_YOUR_TURN
        else -> null
    }

    companion object {
        const val MIN_PLAYERS = 2
        const val MAX_PLAYERS = 4
    }
}
