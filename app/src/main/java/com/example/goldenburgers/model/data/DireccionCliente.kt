package com.example.goldenburgers.model.data

/**
 * Modelo de datos para Dirección de Cliente
 */
data class DireccionCliente(
    val idDireccion: Long,
    val idCliente: Long,
    val ciudad: Ciudad,
    val direccion: String,
    val alias: String? = null
)
