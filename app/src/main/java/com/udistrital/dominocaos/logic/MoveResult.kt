package com.udistrital.dominocaos.logic

import com.udistrital.dominocaos.model.GameState

/** Errores de validación de un movimiento. */
enum class MoveError {
    GAME_NOT_ACTIVE,
    NOT_YOUR_TURN,
    TILE_NOT_IN_HAND,
    INVALID_MOVE,
    POWER_UNAVAILABLE,
    POWER_ALREADY_USED,
    NEEDS_TILE,
    MUST_PLAY,
    POOL_EMPTY,
    POOL_NOT_EMPTY,
    ROOM_FULL,
    NOT_ENOUGH_PLAYERS,
    ONLY_HOST
}

/** Resultado de aplicar una acción sobre el estado del juego. */
sealed class MoveResult {
    data class Ok(val state: GameState) : MoveResult()
    data class Invalid(val error: MoveError) : MoveResult()
}
