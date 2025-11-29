package com.example.goldenburgers.model.data

data class Producto(
    val idProducto: Long?,
    val idCategoria: Long,
    val nombreProducto: String,
    val descripcion: String?,
    val precioBase: Double,
    val imagenUrl: String?,
    val disponible: Int
)