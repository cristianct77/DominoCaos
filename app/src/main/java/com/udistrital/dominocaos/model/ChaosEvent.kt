package com.udistrital.dominocaos.model

/**
 * Eventos aleatorios que alteran el estado de la partida.
 * Pueden afectar fichas, turnos, puntaje o el pozo.
 */
enum class ChaosEvent {
    /** Todos los jugadores roban una ficha del pozo. */
    TORMENTA_FICHAS,

    /** La siguiente jugada vale el doble de puntos. */
    DOBLE_PUNTAJE,

    /** Se invierte el sentido de los turnos. */
    GIRO_CAOTICO,

    /** El jugador en turno recupera un uso de un poder aleatorio. */
    RECARGA_PODER,

    /** El jugador con más puntos pierde 5 puntos. */
    IMPUESTO_LIDER
}
