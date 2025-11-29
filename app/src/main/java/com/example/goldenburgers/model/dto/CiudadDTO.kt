package com.example.goldenburgers.model.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO para Ciudad - usado en comunicación con API
 */
data class CiudadDTO(
    @SerializedName("idCiudad")
    val idCiudad: Long,

    @SerializedName("nombreCiudad")
    val nombreCiudad: String
)
