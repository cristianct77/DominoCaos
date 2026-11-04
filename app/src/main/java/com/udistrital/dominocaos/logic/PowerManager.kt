package com.udistrital.dominocaos.logic

import com.udistrital.dominocaos.model.ActionType
import com.udistrital.dominocaos.model.Domino
import com.udistrital.dominocaos.model.GameState
import com.udistrital.dominocaos.model.LastAction
import com.udistrital.dominocaos.model.Player
import com.udistrital.dominocaos.model.PowerType
import kotlin.random.Random

/**
 * Uso y control de los poderes especiales.
 * El poder se selecciona antes de ejecutar el movimiento y solo se
 * permite uno por turno.
 */
object PowerManager {

    fun canUse(state: GameState, player: Player, power: PowerType): Boolean =
        !state.powerUsedThisTurn && player.usesOf(power) > 0

    /**
     * Aplica [power] para el jugador en turno.
     * @param selectedTile ficha propia (solo requerida por INTERCAMBIO).
     */
    fun apply(
        state: GameState,
        playerId: String,
        power: PowerType,
        selectedTile: Domino?,
        random: Random
    ): MoveResult {
        val index = state.indexOf(playerId)
        val player = state.players.getOrNull(index)
            ?: return MoveResult.Invalid(MoveError.NOT_YOUR_TURN)
        if (state.powerUsedThisTurn) return MoveResult.Invalid(MoveError.POWER_ALREADY_USED)
        if (player.usesOf(power) <= 0) return MoveResult.Invalid(MoveError.POWER_UNAVAILABLE)

        val consumed = player.copy(
            powers = player.powers + (power to player.usesOf(power) - 1),
            powersUsed = player.powersUsed + 1
        )
        var players = state.players.toMutableList().also { it[index] = consumed }
        var newState = state.copy(
            powerUsedThisTurn = true,
            lastAction = LastAction(ActionType.POWER, player.name, power = power)
        )

        newState = when (power) {
            PowerType.COMODIN -> newState.copy(wildActive = true)
            PowerType.SALTO -> newState.copy(skipPending = true)
            PowerType.INVERSION -> newState.copy(direction = -state.direction)
            PowerType.INTERCAMBIO -> {
                val tile = selectedTile ?: return MoveResult.Invalid(MoveError.NEEDS_TILE)
                if (consumed.hand.none { it.sameAs(tile) }) {
                    return MoveResult.Invalid(MoveError.TILE_NOT_IN_HAND)
                }
                val opponentIndex = TurnManager.next(index, state.direction, state.players.size)
                val opponent = players[opponentIndex]
                if (opponent.hand.isEmpty()) return MoveResult.Invalid(MoveError.POWER_UNAVAILABLE)
                val stolen = opponent.hand.random(random)

                players[index] = consumed.copy(
                    hand = consumed.hand.filterNot { it.sameAs(tile) } + stolen
                )
                players[opponentIndex] = opponent.copy(
                    hand = opponent.hand.filterNot { it.sameAs(stolen) } + tile
                )
                newState
            }
        }
        return MoveResult.Ok(newState.copy(players = players))
    }
}
