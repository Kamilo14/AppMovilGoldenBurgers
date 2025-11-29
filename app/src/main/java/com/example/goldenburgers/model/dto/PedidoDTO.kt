package com.example.goldenburgers.model.dto

import com.google.gson.annotations.SerializedName

data class PedidoDTO(
    @SerializedName("idPedido") val idPedido: Long?,
    @SerializedName("idCliente") val idCliente: Long,
    @SerializedName("idEstadoPedido") val idEstadoPedido: Long,
    @SerializedName("idMetodoPago") val idMetodoPago: Long,
    @SerializedName("idTipoEntrega") val idTipoEntrega: Long,
    @SerializedName("idDireccionEntrega") val idDireccionEntrega: Long?,
    @SerializedName("montoSubtotal") val montoSubtotal: Double,
    @SerializedName("montoEnvio") val montoEnvio: Double,
    @SerializedName("montoTotal") val montoTotal: Double,
    @SerializedName("fechaPedido") val fechaPedido: String?,
    @SerializedName("notaCliente") val notaCliente: String?,
    @SerializedName("detalles") val detalles: List<DetallePedidoDTO>
)
