package com.example.goldenburgers.model.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO para crear una dirección de cliente
 * Coincide con CrearDireccionCliente del backend
 */
data class CrearDireccionRequest(
    @SerializedName("idCliente")
    val idCliente: Long,

    @SerializedName("idCiudad")
    val idCiudad: Long,

    @SerializedName("direccion")
    val direccion: String,

    @SerializedName("alias")
    val alias: String?
)
