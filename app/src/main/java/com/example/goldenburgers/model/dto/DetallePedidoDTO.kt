package com.example.goldenburgers.model.dto

import com.google.gson.annotations.SerializedName

data class DetallePedidoDTO(
    @SerializedName("idDetalle") val idDetalle: Long?,
    @SerializedName("idPedido") val idPedido: Long,
    @SerializedName("idProducto") val idProducto: Long,
    @SerializedName("cantidad") val cantidad: Int,
    @SerializedName("precioUnitario") val precioUnitario: Double,
    @SerializedName("subtotalLinea") val subtotalLinea: Double
)
