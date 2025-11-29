package com.example.goldenburgers.model.data

data class Pedido(
    val idPedido: Long?,
    val idCliente: Long,
    val idEstadoPedido: Long,
    val idMetodoPago: Long,
    val idTipoEntrega: Long,
    val idDireccionEntrega: Long?,
    val montoSubtotal: Double,
    val montoEnvio: Double,
    val montoTotal: Double,
    val fechaPedido: String?, // ISO 8601
    val notaCliente: String?,
    val detalles: List<DetallePedido>
)