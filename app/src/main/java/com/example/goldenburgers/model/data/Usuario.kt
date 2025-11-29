package com.example.goldenburgers.model.data

/**
 * Modelo de datos para Usuario (vinculado con Firebase Authentication)
 */
data class Usuario(
    val idUsuario: String,  // Firebase UID
    val email: String,
    val rol: Rol,
    val fechaCreacion: String  // formato: "YYYY-MM-DD"
)
