package com.udistrital.dominocaos.data.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.udistrital.dominocaos.data.FirebaseConfig
import com.udistrital.dominocaos.data.mapper.FirebaseMapper
import com.udistrital.dominocaos.model.MatchRecord
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/** Historial de partidas: /users/{uid}/history/{id}. */
class HistoryRepository(
    database: FirebaseDatabase = FirebaseConfig.database()
) {

    private val users = database.reference.child("users")

    /**
     * Guarda el resultado de una partida. Se usa [matchKey] como id para
     * no duplicar el registro si la pantalla se recompone.
     */
    suspend fun save(uid: String, matchKey: String, record: MatchRecord) {
        users.child(uid).child("history").child(matchKey)
            .setValue(FirebaseMapper.recordToMap(record))
            .await()
    }

    fun observe(uid: String): Flow<List<MatchRecord>> = callbackFlow {
        val ref = users.child(uid).child("history")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children
                    .mapNotNull { FirebaseMapper.toMatchRecord(it.key ?: "", it.value as? Map<*, *>) }
                    .sortedByDescending { it.date }
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }
}
