package com.example.goldenburgers.model.data

data class DetallePedido(
    val idDetalle: Long?,
    val idPedido: Long,
    val idProducto: Long,
    val cantidad: Int,
    val precioUnitario: Double,
    val subtotalLinea: Double
)