package com.udistrital.dominocaos.data.mapper

import com.udistrital.dominocaos.model.ActionType
import com.udistrital.dominocaos.model.BoardTile
import com.udistrital.dominocaos.model.ChaosEvent
import com.udistrital.dominocaos.model.Domino
import com.udistrital.dominocaos.model.FinishReason
import com.udistrital.dominocaos.model.GameState
import com.udistrital.dominocaos.model.GameStatus
import com.udistrital.dominocaos.model.LastAction
import com.udistrital.dominocaos.model.MatchRecord
import com.udistrital.dominocaos.model.Player
import com.udistrital.dominocaos.model.PowerType

/**
 * Convierte los modelos del juego a mapas que Firebase Realtime Database
 * puede guardar, y viceversa. Las fichas se guardan como texto "a|b" para
 * que el tablero sea legible en la consola de Firebase.
 *
 * Firebase devuelve números como Long, listas como List y elimina las
 * listas vacías, por eso la lectura es tolerante a valores nulos.
 */
object FirebaseMapper {

    // ----------------------------------------------------------- Escritura

    fun toMap(state: GameState): Map<String, Any?> = mapOf(
        "code" to state.code,
        "hostId" to state.hostId,
        "status" to state.status.name,
        "maxPlayers" to state.maxPlayers,
        "maxRounds" to state.maxRounds,
        "players" to state.players.map { playerToMap(it) },
        "board" to state.board.map { mapOf("tile" to "${it.domino.left}|${it.domino.right}", "wild" to it.wild) },
        "pool" to state.pool.map { it.key },
        "currentTurn" to state.currentTurn,
        "direction" to state.direction,
        "turnCount" to state.turnCount,
        "consecutivePasses" to state.consecutivePasses,
        "wildActive" to state.wildActive,
        "skipPending" to state.skipPending,
        "doubleScoreNext" to state.doubleScoreNext,
        "powerUsedThisTurn" to state.powerUsedThisTurn,
        "lastEvent" to state.lastEvent?.name,
        "eventCounter" to state.eventCounter,
        "lastAction" to state.lastAction?.let {
            mapOf(
                "type" to it.type.name,
                "playerName" to it.playerName,
                "power" to it.power?.name,
                "domino" to it.domino?.let { d -> "${d.left}|${d.right}" }
            )
        },
        "winnerId" to state.winnerId,
        "finishReason" to state.finishReason?.name,
        "createdAt" to state.createdAt
    )

    private fun playerToMap(player: Player): Map<String, Any?> = mapOf(
        "id" to player.id,
        "name" to player.name,
        "hand" to player.hand.map { it.key },
        "score" to player.score,
        "powers" to player.powers.mapKeys { it.key.name },
        "powersUsed" to player.powersUsed,
        "tilesPlaced" to player.tilesPlaced,
        "eventsReceived" to player.eventsReceived
    )

    fun recordToMap(record: MatchRecord): Map<String, Any?> = mapOf(
        "won" to record.won,
        "score" to record.score,
        "round" to record.round,
        "maxRounds" to record.maxRounds,
        "opponents" to record.opponents,
        "tilesPlaced" to record.tilesPlaced,
        "powersUsed" to record.powersUsed,
        "date" to record.date
    )

    // ------------------------------------------------------------- Lectura

    fun toGameState(map: Map<*, *>?): GameState? {
        if (map == null) return null
        val code = map["code"] as? String ?: return null
        return GameState(
            code = code,
            hostId = map.string("hostId"),
            status = enumOr(map["status"], GameStatus.WAITING),
            maxPlayers = map.int("maxPlayers", 2),
            maxRounds = map.int("maxRounds", 10),
            players = map.list("players").mapNotNull { toPlayer(it as? Map<*, *>) },
            board = map.list("board").mapNotNull { item ->
                val m = item as? Map<*, *> ?: return@mapNotNull null
                val domino = Domino.fromKey(m["tile"] as? String ?: "") ?: return@mapNotNull null
                BoardTile(domino, m["wild"] as? Boolean ?: false)
            },
            pool = map.list("pool").mapNotNull { Domino.fromKey(it as? String ?: "") },
            currentTurn = map.int("currentTurn"),
            direction = map.int("direction", 1),
            turnCount = map.int("turnCount"),
            consecutivePasses = map.int("consecutivePasses"),
            wildActive = map["wildActive"] as? Boolean ?: false,
            skipPending = map["skipPending"] as? Boolean ?: false,
            doubleScoreNext = map["doubleScoreNext"] as? Boolean ?: false,
            powerUsedThisTurn = map["powerUsedThisTurn"] as? Boolean ?: false,
            lastEvent = enumOrNull<ChaosEvent>(map["lastEvent"]),
            eventCounter = map.int("eventCounter"),
            lastAction = (map["lastAction"] as? Map<*, *>)?.let { a ->
                val type = enumOrNull<ActionType>(a["type"]) ?: return@let null
                LastAction(
                    type = type,
                    playerName = a.string("playerName"),
                    power = enumOrNull<PowerType>(a["power"]),
                    domino = (a["domino"] as? String)?.let { Domino.fromKey(it) }
                )
            },
            winnerId = map["winnerId"] as? String,
            finishReason = enumOrNull<FinishReason>(map["finishReason"]),
            createdAt = (map["createdAt"] as? Number)?.toLong() ?: 0L
        )
    }

    private fun toPlayer(map: Map<*, *>?): Player? {
        if (map == null) return null
        val id = map["id"] as? String ?: return null
        val powersMap = map["powers"] as? Map<*, *> ?: emptyMap<String, Any>()
        return Player(
            id = id,
            name = map.string("name"),
            hand = map.list("hand").mapNotNull { Domino.fromKey(it as? String ?: "") },
            score = map.int("score"),
            powers = PowerType.entries.associateWith { power ->
                (powersMap[power.name] as? Number)?.toInt() ?: 0
            },
            powersUsed = map.int("powersUsed"),
            tilesPlaced = map.int("tilesPlaced"),
            eventsReceived = map.int("eventsReceived")
        )
    }

    fun toMatchRecord(id: String, map: Map<*, *>?): MatchRecord? {
        if (map == null) return null
        return MatchRecord(
            id = id,
            won = map["won"] as? Boolean ?: false,
            score = map.int("score"),
            round = map.int("round"),
            maxRounds = map.int("maxRounds"),
            opponents = map.string("opponents"),
            tilesPlaced = map.int("tilesPlaced"),
            powersUsed = map.int("powersUsed"),
            date = (map["date"] as? Number)?.toLong() ?: 0L
        )
    }

    // ------------------------------------------------------------ Helpers

    private fun Map<*, *>.string(key: String): String = this[key] as? String ?: ""

    private fun Map<*, *>.int(key: String, default: Int = 0): Int =
        (this[key] as? Number)?.toInt() ?: default

    /** Firebase puede devolver una lista como List o como Map con claves "0","1"... */
    private fun Map<*, *>.list(key: String): List<Any?> = when (val value = this[key]) {
        is List<*> -> value.filterNotNull()
        is Map<*, *> -> value.entries
            .sortedBy { (it.key as? String)?.toIntOrNull() ?: 0 }
            .mapNotNull { it.value }
        else -> emptyList()
    }

    private inline fun <reified T : Enum<T>> enumOrNull(value: Any?): T? =
        (value as? String)?.let { name -> enumValues<T>().firstOrNull { it.name == name } }

    private inline fun <reified T : Enum<T>> enumOr(value: Any?, default: T): T =
        enumOrNull<T>(value) ?: default
}
