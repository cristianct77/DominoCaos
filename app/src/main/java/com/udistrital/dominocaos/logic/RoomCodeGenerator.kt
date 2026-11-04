package com.udistrital.dominocaos.logic

import kotlin.random.Random

/** Genera códigos de sala cortos y fáciles de compartir (ej: "K7Q2"). */
object RoomCodeGenerator {

    /** Sin 0/O ni 1/I para evitar confusiones al dictar el código. */
    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    const val LENGTH = 4

    fun generate(random: Random = Random.Default): String =
        (1..LENGTH).map { ALPHABET[random.nextInt(ALPHABET.length)] }.joinToString("")

    fun isValid(code: String): Boolean =
        code.length == LENGTH && code.all { it in ALPHABET }

    fun normalize(input: String): String = input.trim().uppercase()
}
