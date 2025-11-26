package com.example.goldenburgers.model.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO para solicitud de login
 * Se envía al API Gateway para autenticación
 */
data class LoginRequest(
    @SerializedName("email")
    val email: String,

    @SerializedName("password")
    val password: String
)
