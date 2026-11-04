package com.udistrital.dominocaos.model

/**
 * Poderes especiales disponibles en Dominó Caos.
 * [initialUses] = número de usos con los que inicia cada jugador.
 */
enum class PowerType(val initialUses: Int) {
    /** Permite colocar una ficha sin cumplir uno de los extremos. */
    COMODIN(1),

    /** El siguiente jugador pierde su turno. */
    SALTO(1),

    /** Intercambia una ficha propia con una ficha aleatoria del oponente. */
    INTERCAMBIO(1),

    /** Cambia el sentido de los turnos. */
    INVERSION(1);

    companion object {
        /** Mapa de usos iniciales para un jugador nuevo. */
        fun initialUses(): Map<PowerType, Int> = entries.associateWith { it.initialUses }
    }
}
