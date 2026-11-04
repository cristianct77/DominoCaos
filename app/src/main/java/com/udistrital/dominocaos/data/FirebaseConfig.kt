package com.udistrital.dominocaos.data

import com.google.firebase.database.FirebaseDatabase

/**
 * Configuración de Firebase Realtime Database.
 *
 * La URL se fija aquí para que la app funcione aunque el archivo
 * google-services.json se haya descargado antes de crear la base de datos.
 * Corresponde a una base creada en "Estados Unidos (us-central1)".
 * Si se creó en otra región, copiar la URL que aparece en
 * Firebase > Realtime Database > Datos.
 */
object FirebaseConfig {

    const val DATABASE_URL = "https://dominocaos-de519-default-rtdb.firebaseio.com"

    fun database(): FirebaseDatabase = FirebaseDatabase.getInstance(DATABASE_URL)
}
