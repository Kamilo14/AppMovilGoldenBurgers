package com.example.goldenburgers.service

import retrofit2.http.*
import com.example.goldenburgers.model.dto.PedidoDTO


interface PedidoApiService {
    @GET("api/pedidos")
    suspend fun getPedidos(): List<PedidoDTO>

    @GET("api/pedidos/{id}")
    suspend fun getPedido(@Path("id") id: Long): PedidoDTO

    @GET("api/pedidos/cliente/{idCliente}")
    suspend fun getPedidosPorCliente(@Path("idCliente") idCliente: Long): List<PedidoDTO>

    @POST("api/pedidos/completo")
    suspend fun crearPedidoCompleto(@Body pedido: PedidoDTO): PedidoDTO

    @PUT("api/pedidos/cambiar-estado/{idPedido}/estado/{idEstado}")
    suspend fun cambiarEstadoPedido(
        @Path("idPedido") idPedido: Long,
        @Path("idEstado") idEstado: Long
    ): PedidoDTO

}

