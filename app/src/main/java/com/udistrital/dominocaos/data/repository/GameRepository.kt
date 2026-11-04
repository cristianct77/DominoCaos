package com.udistrital.dominocaos.data.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.udistrital.dominocaos.data.FirebaseConfig
import com.udistrital.dominocaos.data.mapper.FirebaseMapper
import com.udistrital.dominocaos.logic.MoveError
import com.udistrital.dominocaos.logic.MoveResult
import com.udistrital.dominocaos.logic.RoomCodeGenerator
import com.udistrital.dominocaos.model.GameState
import com.udistrital.dominocaos.model.GameStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume

/**
 * Sincronización de las salas en tiempo real con Firebase Realtime Database.
 *
 * Estructura:
 *   /rooms/{code} -> GameState (ver [FirebaseMapper])
 *
 * Todas las jugadas se escriben con transacciones para evitar que dos
 * dispositivos sobrescriban el estado al mismo tiempo.
 */
class GameRepository(
    database: FirebaseDatabase = FirebaseConfig.database()
) {

    private val rooms = database.reference.child("rooms")

    /** Genera un código libre y guarda la sala. */
    suspend fun createRoom(build: (code: String) -> GameState): Result<GameState> = runCatching {
        var code: String
        do {
            code = RoomCodeGenerator.generate()
        } while (rooms.child(code).get().await().exists())
        val state = build(code)
        rooms.child(code).setValue(FirebaseMapper.toMap(state)).await()
        state
    }

    /** Escucha en tiempo real los cambios de una sala. */
    fun observeRoom(code: String): Flow<GameState?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(FirebaseMapper.toGameState(snapshot.value as? Map<*, *>))
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        rooms.child(code).addValueEventListener(listener)
        awaitClose { rooms.child(code).removeEventListener(listener) }
    }

    /** Escucha las salas que están esperando jugadores. */
    fun observeOpenRooms(): Flow<List<GameState>> = callbackFlow {
        val query = rooms.orderByChild("status").equalTo(GameStatus.WAITING.name)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children
                    .mapNotNull { FirebaseMapper.toGameState(it.value as? Map<*, *>) }
                    .filter { !it.isFull }
                    .sortedByDescending { it.createdAt }
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    /**
     * Aplica [action] sobre el estado actual de la sala dentro de una
     * transacción. Si la acción es inválida la transacción se aborta.
     */
    suspend fun update(code: String, action: (GameState) -> MoveResult): MoveResult =
        suspendCancellableCoroutine { continuation ->
            var outcome: MoveResult = MoveResult.Invalid(MoveError.GAME_NOT_ACTIVE)
            rooms.child(code).runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val current = FirebaseMapper.toGameState(currentData.value as? Map<*, *>)
                        ?: return Transaction.success(currentData) // aún sin datos locales: reintenta
                    outcome = action(current)
                    val ok = outcome as? MoveResult.Ok ?: return Transaction.abort()
                    currentData.value = FirebaseMapper.toMap(ok.state)
                    return Transaction.success(currentData)
                }

                override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                    if (!continuation.isActive) return
                    continuation.resume(
                        if (error != null) MoveResult.Invalid(MoveError.GAME_NOT_ACTIVE) else outcome
                    )
                }
            })
        }

    suspend fun deleteRoom(code: String) {
        rooms.child(code).removeValue().await()
    }
}
