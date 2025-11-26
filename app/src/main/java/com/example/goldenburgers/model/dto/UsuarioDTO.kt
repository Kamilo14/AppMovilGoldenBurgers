package com.example.goldenburgers.model.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO para Usuario - usado en respuestas de API
 */
data class UsuarioDTO(
    @SerializedName("idUsuario")
    val idUsuario: String,  // Firebase UID

    @SerializedName("email")
    val email: String,

    @SerializedName("rol")
    val rol: RolDTO,

    @SerializedName("fechaCreacion")
    val fechaCreacion: String  // formato: "YYYY-MM-DD"
)
