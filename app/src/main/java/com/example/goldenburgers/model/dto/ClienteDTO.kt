package com.example.goldenburgers.model.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO para Cliente - usado en respuestas de API
 * Incluye información completa del cliente con usuario y direcciones
 */
data class ClienteDTO(
    @SerializedName("idCliente")
    val idCliente: Long,

    @SerializedName("usuario")
    val usuario: UsuarioDTO,

    @SerializedName("nombreCliente")
    val nombreCliente: String,

    @SerializedName("telefonoCliente")
    val telefonoCliente: String?,

    @SerializedName("direcciones")
    val direcciones: List<DireccionClienteDTO> = emptyList()
)
