package com.udistrital.dominocaos.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import com.udistrital.dominocaos.data.FirebaseConfig
import com.udistrital.dominocaos.model.UserProfile
import kotlinx.coroutines.tasks.await

/**
 * Autenticación con Firebase Auth (correo y contraseña).
 * Al registrarse también se guarda el perfil en /users/{uid}/profile.
 */
class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val database: FirebaseDatabase = FirebaseConfig.database()
) {

    /** Usuario con sesión iniciada o null. */
    fun currentUser(): UserProfile? = auth.currentUser?.let { user ->
        UserProfile(
            uid = user.uid,
            name = user.displayName?.takeIf { it.isNotBlank() } ?: user.email?.substringBefore("@") ?: "Jugador",
            email = user.email ?: ""
        )
    }

    suspend fun login(email: String, password: String): Result<UserProfile> = runCatching {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
        currentUser() ?: error("No se pudo iniciar sesión")
    }

    suspend fun register(username: String, email: String, password: String): Result<UserProfile> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val user = result.user ?: error("No se pudo crear la cuenta")
        user.updateProfile(
            UserProfileChangeRequest.Builder().setDisplayName(username.trim()).build()
        ).await()
        database.reference.child("users").child(user.uid).child("profile")
            .setValue(mapOf("name" to username.trim(), "email" to email.trim()))
            .await()
        UserProfile(user.uid, username.trim(), email.trim())
    }

    fun logout() = auth.signOut()
}
