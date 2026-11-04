package com.udistrital.dominocaos.data

import com.udistrital.dominocaos.data.mapper.FirebaseMapper
import com.udistrital.dominocaos.logic.EventGenerator
import com.udistrital.dominocaos.logic.GameEngine
import com.udistrital.dominocaos.logic.MoveResult
import com.udistrital.dominocaos.model.ChaosEvent
import com.udistrital.dominocaos.model.MatchRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.random.Random

class FirebaseMapperTest {

    private val engine = GameEngine(Random(11), EventGenerator(Random(11), 0.0))

    @Test
    fun gameState_survivesRoundTrip() {
        val room = engine.createRoom("QW12", "a", "Ana", 2, 10, 123L)
        val joined = (engine.addPlayer(room, "b", "Beto") as MoveResult.Ok).state
        val started = (engine.startGame(joined, "a") as MoveResult.Ok).state
            .copy(lastEvent = ChaosEvent.GIRO_CAOTICO, eventCounter = 2)

        val restored = FirebaseMapper.toGameState(FirebaseMapper.toMap(started))
        assertEquals(started, restored)
    }

    @Test
    fun missingLists_areReadAsEmpty() {
        // Firebase elimina las listas vacías: el mapper debe tolerarlo
        val map = mapOf("code" to "ABCD", "status" to "WAITING", "maxPlayers" to 3L)
        val state = FirebaseMapper.toGameState(map)!!
        assertEquals(3, state.maxPlayers)
        assertEquals(0, state.board.size)
        assertEquals(0, state.players.size)
    }

    @Test
    fun invalidMap_returnsNull() {
        assertNull(FirebaseMapper.toGameState(null))
        assertNull(FirebaseMapper.toGameState(mapOf("status" to "WAITING")))
    }

    @Test
    fun matchRecord_survivesRoundTrip() {
        val record = MatchRecord("id1", true, 120, 10, 10, "Beto", 12, 2, 999L)
        assertEquals(record, FirebaseMapper.toMatchRecord("id1", FirebaseMapper.recordToMap(record)))
    }
}
