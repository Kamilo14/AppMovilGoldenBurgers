package com.example.goldenburgers.model.data

data class Venta(
        val idVenta: Long?,
        val idPedido: Long,
        val totalVenta: Double,
        val fechaVenta: String
    )
