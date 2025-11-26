package com.example.goldenburgers.model.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO para actualizar el perfil de un cliente
 */
data class ActualizarPerfilClienteRequest(
    @SerializedName("nombreCliente")
    val nombreCliente: String,

    @SerializedName("telefonoCliente")
    val telefonoCliente: String?
)
