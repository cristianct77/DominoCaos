package com.udistrital.dominocaos.model

/** Estado general de la sala / partida. */
enum class GameStatus { WAITING, PLAYING, FINISHED }

/** Motivo por el que terminó la partida. */
enum class FinishReason { EMPTY_HAND, BLOCKED, MAX_ROUNDS, ABANDONED }

/** Tipo de la última acción realizada (para mostrar en la interfaz). */
enum class ActionType { START, PLAY, DRAW, PASS, POWER, LEAVE, RESTART }

/** Última acción de la partida; la UI decide cómo mostrarla. */
data class LastAction(
    val type: ActionType,
    val playerName: String,
    val power: PowerType? = null,
    val domino: Domino? = null
)

/**
 * Estado completo de una partida de Dominó Caos.
 * Es inmutable: cada movimiento produce un nuevo [GameState].
 * Este mismo estado es el que se sincroniza con Firebase.
 */
data class GameState(
    val code: String = "",
    val hostId: String = "",
    val status: GameStatus = GameStatus.WAITING,
    val maxPlayers: Int = 2,
    val maxRounds: Int = 10,
    val players: List<Player> = emptyList(),
    val board: List<BoardTile> = emptyList(),
    val pool: List<Domino> = emptyList(),
    val currentTurn: Int = 0,
    /** 1 = sentido normal, -1 = sentido invertido. */
    val direction: Int = 1,
    val turnCount: Int = 0,
    val consecutivePasses: Int = 0,
    /** Poder Comodín activo para la siguiente ficha del jugador en turno. */
    val wildActive: Boolean = false,
    /** Poder Salto pendiente: el siguiente jugador pierde su turno. */
    val skipPending: Boolean = false,
    /** Evento Doble Puntaje activo para la siguiente jugada. */
    val doubleScoreNext: Boolean = false,
    /** Solo se permite un poder por turno. */
    val powerUsedThisTurn: Boolean = false,
    val lastEvent: ChaosEvent? = null,
    /** Aumenta con cada evento: permite a la UI saber cuándo mostrar el diálogo. */
    val eventCounter: Int = 0,
    val lastAction: LastAction? = null,
    val winnerId: String? = null,
    val finishReason: FinishReason? = null,
    val createdAt: Long = 0L
) {
    /** Ronda actual: una ronda = un turno por jugador. */
    val round: Int get() = if (players.isEmpty()) 1 else turnCount / players.size + 1

    val currentPlayer: Player? get() = players.getOrNull(currentTurn)

    val leftEnd: Int? get() = board.firstOrNull()?.domino?.left

    val rightEnd: Int? get() = board.lastOrNull()?.domino?.right

    val isFull: Boolean get() = players.size >= maxPlayers

    fun playerById(id: String): Player? = players.firstOrNull { it.id == id }

    fun indexOf(id: String): Int = players.indexOfFirst { it.id == id }

    fun isTurnOf(id: String): Boolean = currentPlayer?.id == id
}
