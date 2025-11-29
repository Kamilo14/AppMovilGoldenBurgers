package com.example.goldenburgers.model.data

data class Boleta(
    val idBoleta: Long?,
    val idVenta: Long,
    val numeroSii: String,
    val urlDocumento: String?,
    val iva: Double = 0.19,
    val totalConIva: Double?
)
