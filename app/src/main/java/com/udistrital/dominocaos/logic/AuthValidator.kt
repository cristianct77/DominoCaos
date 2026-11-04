package com.udistrital.dominocaos.logic

/** Errores de validación de los formularios de login y registro. */
enum class AuthError {
    EMPTY_FIELDS,
    INVALID_EMAIL,
    SHORT_PASSWORD,
    PASSWORDS_DONT_MATCH,
    SHORT_USERNAME,
    FIREBASE
}

/** Validaciones de formularios independientes de Android. */
object AuthValidator {

    const val MIN_PASSWORD = 6
    const val MIN_USERNAME = 3

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun validateLogin(email: String, password: String): AuthError? = when {
        email.isBlank() || password.isBlank() -> AuthError.EMPTY_FIELDS
        !EMAIL_REGEX.matches(email.trim()) -> AuthError.INVALID_EMAIL
        else -> null
    }

    fun validateRegister(username: String, email: String, password: String, confirm: String): AuthError? = when {
        username.isBlank() || email.isBlank() || password.isBlank() || confirm.isBlank() -> AuthError.EMPTY_FIELDS
        username.trim().length < MIN_USERNAME -> AuthError.SHORT_USERNAME
        !EMAIL_REGEX.matches(email.trim()) -> AuthError.INVALID_EMAIL
        password.length < MIN_PASSWORD -> AuthError.SHORT_PASSWORD
        password != confirm -> AuthError.PASSWORDS_DONT_MATCH
        else -> null
    }
}
