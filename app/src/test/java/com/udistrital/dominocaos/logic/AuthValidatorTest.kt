package com.udistrital.dominocaos.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class AuthValidatorTest {

    @Test
    fun login_requiresValidEmail() {
        assertEquals(AuthError.EMPTY_FIELDS, AuthValidator.validateLogin("", "123456"))
        assertEquals(AuthError.INVALID_EMAIL, AuthValidator.validateLogin("correo", "123456"))
        assertNull(AuthValidator.validateLogin("ana@udistrital.edu.co", "123456"))
    }

    @Test
    fun register_validatesAllFields() {
        assertEquals(AuthError.SHORT_USERNAME, AuthValidator.validateRegister("an", "a@b.co", "123456", "123456"))
        assertEquals(AuthError.SHORT_PASSWORD, AuthValidator.validateRegister("ana", "a@b.co", "123", "123"))
        assertEquals(AuthError.PASSWORDS_DONT_MATCH, AuthValidator.validateRegister("ana", "a@b.co", "123456", "654321"))
        assertNull(AuthValidator.validateRegister("ana", "a@b.co", "123456", "123456"))
    }

    @Test
    fun roomCode_isValidAndNormalized() {
        val code = RoomCodeGenerator.generate(Random(5))
        assertTrue(RoomCodeGenerator.isValid(code))
        assertEquals("K7Q2", RoomCodeGenerator.normalize("  k7q2 "))
    }
}
