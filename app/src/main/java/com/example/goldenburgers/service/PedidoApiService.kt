package com.example.goldenburgers.service

import com.example.goldenburgers.model.dto.PedidoDTO
import retrofit2.Response
import retrofit2.http.*

interface PedidoApiService {
    @GET("pedidos")
    suspend fun getPedidos(): List<PedidoDTO>

    @GET("pedidos/{id}")
    suspend fun getPedido(@Path("id") id: Long): PedidoDTO

    // [CORRECCIÓN DEFINITIVA] Se devuelve un Response<T> para tener control total sobre la respuesta
    @GET("pedidos/cliente/{idCliente}")
    suspend fun getPedidosPorCliente(@Path("idCliente") idCliente: Long): Response<List<PedidoDTO>>

    @POST("pedidos/completo")
    suspend fun crearPedidoCompleto(@Body pedido: PedidoDTO): PedidoDTO

    @PUT("pedidos/cambiar-estado/{idPedido}/estado/{idEstado}")
    suspend fun cambiarEstadoPedido(
        @Path("idPedido") idPedido: Long,
        @Path("idEstado") idEstado: Long
    ): PedidoDTO

}
