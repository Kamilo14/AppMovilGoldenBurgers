package com.example.goldenburgers.model.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO para Rol - usado en comunicación con API
 */
data class RolDTO(
    @SerializedName("idRol")
    val idRol: Int,

    @SerializedName("nombreRol")
    val nombreRol: String
)
