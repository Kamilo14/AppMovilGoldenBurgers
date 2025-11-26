package com.example.goldenburgers.model.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO para solicitud de registro de nuevo cliente
 * Se envía al endpoint POST /api/clientes
 */
data class RegistrarClienteRequest(
    @SerializedName("idUsuario")
    val idUsuario: String,  // Firebase UID

    @SerializedName("email")
    val email: String,

    @SerializedName("nombreCliente")
    val nombreCliente: String,

    @SerializedName("telefonoCliente")
    val telefonoCliente: String?
)
