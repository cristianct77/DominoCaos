package com.udistrital.dominocaos.model

/** Usuario autenticado. */
data class UserProfile(
    val uid: String,
    val name: String,
    val email: String
)

/** Registro de una partida terminada (historial del usuario). */
data class MatchRecord(
    val id: String = "",
    val won: Boolean = false,
    val score: Int = 0,
    val round: Int = 0,
    val maxRounds: Int = 0,
    val opponents: String = "",
    val tilesPlaced: Int = 0,
    val powersUsed: Int = 0,
    val date: Long = 0L
)
