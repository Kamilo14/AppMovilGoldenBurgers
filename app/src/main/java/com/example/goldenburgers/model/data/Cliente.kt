package com.example.goldenburgers.model.data

/**
 * Modelo de datos completo para Cliente
 * Incluye información del usuario base y datos específicos del cliente
 */
data class Cliente(
    val idCliente: Long,
    val usuario: Usuario,
    val nombreCliente: String,
    val telefonoCliente: String?,
    val direcciones: List<DireccionCliente> = emptyList()
)
