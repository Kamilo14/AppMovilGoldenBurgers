package com.example.goldenburgers.service

import retrofit2.http.*
import com.example.goldenburgers.model.dto.PedidoDTO


interface PedidoApiService {
    @GET("pedidos")
    suspend fun getPedidos(): List<PedidoDTO>

    @GET("pedidos/{id}")
    suspend fun getPedido(@Path("id") id: Long): PedidoDTO

    @GET("pedidos/cliente/{idCliente}")
    suspend fun getPedidosPorCliente(@Path("idCliente") idCliente: Long): List<PedidoDTO>

    @POST("pedidos/completo")
    suspend fun crearPedidoCompleto(@Body pedido: PedidoDTO): PedidoDTO

    @PUT("pedidos/cambiar-estado/{idPedido}/estado/{idEstado}")
    suspend fun cambiarEstadoPedido(
        @Path("idPedido") idPedido: Long,
        @Path("idEstado") idEstado: Long
    ): PedidoDTO

}

