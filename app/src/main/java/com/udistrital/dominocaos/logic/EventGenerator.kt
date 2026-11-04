package com.udistrital.dominocaos.logic

import com.udistrital.dominocaos.model.ChaosEvent
import com.udistrital.dominocaos.model.GameState
import com.udistrital.dominocaos.model.PowerType
import kotlin.random.Random

/**
 * Generación y aplicación de eventos aleatorios ("caos").
 * Al terminar cada turno existe una probabilidad [probability] de que
 * ocurra un evento que modifica fichas, turno, puntaje o pozo.
 */
class EventGenerator(
    private val random: Random = Random.Default,
    private val probability: Double = DEFAULT_PROBABILITY
) {

    /** Decide si ocurre un evento y cuál. */
    fun roll(): ChaosEvent? =
        if (random.nextDouble() < probability) ChaosEvent.entries.random(random) else null

    /** Aplica el efecto de [event] y lo registra en el estado. */
    fun apply(state: GameState, event: ChaosEvent): GameState {
        val applied = when (event) {
            ChaosEvent.TORMENTA_FICHAS -> stormOfTiles(state)
            ChaosEvent.DOBLE_PUNTAJE -> state.copy(doubleScoreNext = true)
            ChaosEvent.GIRO_CAOTICO -> state.copy(
                direction = -state.direction,
                players = state.players.map { it.copy(eventsReceived = it.eventsReceived + 1) }
            )
            ChaosEvent.RECARGA_PODER -> rechargePower(state)
            ChaosEvent.IMPUESTO_LIDER -> taxLeader(state)
        }
        return applied.copy(lastEvent = event, eventCounter = state.eventCounter + 1)
    }

    /** Todos los jugadores roban una ficha mientras haya fichas en el pozo. */
    private fun stormOfTiles(state: GameState): GameState {
        val pool = state.pool.toMutableList()
        val players = state.players.map { player ->
            if (pool.isEmpty()) player
            else player.copy(
                hand = player.hand + pool.removeAt(0),
                eventsReceived = player.eventsReceived + 1
            )
        }
        return state.copy(players = players, pool = pool)
    }

    /** El jugador en turno recupera un uso de un poder aleatorio. */
    private fun rechargePower(state: GameState): GameState {
        val index = state.currentTurn
        val player = state.players.getOrNull(index) ?: return state
        val power = PowerType.entries.random(random)
        val updated = player.copy(
            powers = player.powers + (power to player.usesOf(power) + 1),
            eventsReceived = player.eventsReceived + 1
        )
        return state.copy(players = state.players.toMutableList().also { it[index] = updated })
    }

    /** El líder del puntaje pierde [ScoreCalculator.TAX_POINTS] puntos. */
    private fun taxLeader(state: GameState): GameState {
        val leader = state.players.maxByOrNull { it.score } ?: return state
        if (leader.score <= 0) return state
        val players = state.players.map {
            if (it.id == leader.id) it.copy(
                score = (it.score - ScoreCalculator.TAX_POINTS).coerceAtLeast(0),
                eventsReceived = it.eventsReceived + 1
            ) else it
        }
        return state.copy(players = players)
    }

    companion object {
        const val DEFAULT_PROBABILITY = 0.25
    }
}
