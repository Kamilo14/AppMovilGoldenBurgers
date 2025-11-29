package com.example.goldenburgers.model.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO para respuesta de login desde API Gateway
 */
data class LoginResponse(
    @SerializedName("internalToken")
    val internalToken: String,

    @SerializedName("user")
    val user: UserInfoDTO,

    @SerializedName("expiresIn")
    val expiresIn: Long
)

/**
 * Información del usuario en la respuesta de login
 */
data class UserInfoDTO(
    @SerializedName("uid")
    val uid: String,

    @SerializedName("email")
    val email: String,

    @SerializedName("nombre")
    val nombre: String?,

    @SerializedName("rolId")
    val rolId: Int?,

    @SerializedName("rolNombre")
    val rolNombre: String?
)
