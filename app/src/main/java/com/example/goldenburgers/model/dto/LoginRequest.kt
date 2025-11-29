package com.example.goldenburgers.model.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO para solicitud de login
 * Se envía al API Gateway para autenticación con Firebase Token
 */
data class LoginRequest(
    @SerializedName("firebaseToken")
    val firebaseToken: String
)
