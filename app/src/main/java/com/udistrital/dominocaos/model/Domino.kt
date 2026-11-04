package com.udistrital.dominocaos.model

/**
 * Ficha de dominó. [left] y [right] representan los valores en el orden
 * en que se muestran (izquierda -> derecha) sobre el tablero.
 */
data class Domino(
    val left: Int,
    val right: Int
) {

    /** Una ficha es doble cuando ambos lados tienen el mismo valor. */
    val isDouble: Boolean get() = left == right

    /** Puntos totales de la ficha (suma de los dos lados). */
    val points: Int get() = left + right

    /** Identificador único sin importar la orientación: "2|5" == "5|2". */
    val key: String get() = "${minOf(left, right)}|${maxOf(left, right)}"

    /** Indica si alguno de los lados tiene el valor [value]. */
    fun has(value: Int): Boolean = left == value || right == value

    /** Devuelve la misma ficha girada. */
    fun flipped(): Domino = Domino(right, left)

    /** Compara dos fichas sin importar su orientación. */
    fun sameAs(other: Domino): Boolean = key == other.key

    companion object {
        const val MAX_VALUE = 6

        /** Convierte un texto "a|b" (formato guardado en Firebase) en ficha. */
        fun fromKey(text: String): Domino? {
            val parts = text.split("|")
            if (parts.size != 2) return null
            val a = parts[0].toIntOrNull() ?: return null
            val b = parts[1].toIntOrNull() ?: return null
            if (a !in 0..MAX_VALUE || b !in 0..MAX_VALUE) return null
            return Domino(a, b)
        }
    }
}

/** Ficha colocada en el tablero. [wild] = colocada con el poder Comodín. */
data class BoardTile(
    val domino: Domino,
    val wild: Boolean = false
)

/** Extremo del tablero donde se coloca una ficha. */
enum class Side { LEFT, RIGHT }
