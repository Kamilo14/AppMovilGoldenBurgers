package com.example.goldenburgers.model.data

data class Devolucion(
    val idDevolucion: Long?,
    val idVenta: Long,
    val montoDevuelto: Double,
    val motivo: String?,
    val fechaDevolucion: String // ISO 8601
)