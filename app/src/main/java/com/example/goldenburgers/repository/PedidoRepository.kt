package com.example.goldenburgers.repository

import com.example.goldenburgers.model.dto.PedidoDTO

/**
 * Repositorio para gestionar todas las operaciones relacionadas con los Pedidos.
 */
class PedidoRepository(
    private val networkSource: PedidoNetworkSource
) {

    suspend fun crearPedidoCompleto(pedido: PedidoDTO): Result<PedidoDTO> {
        return try {
            val api = networkSource.getService()
            val nuevoPedido = api.crearPedidoCompleto(pedido)
            Result.success(nuevoPedido)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtiene el historial de pedidos para un cliente específico.
     * [CORRECCIÓN DEFINITIVA] Se maneja el objeto Response completo para ser a prueba de nulos.
     */
    suspend fun getPedidosPorCliente(idCliente: Long): Result<List<PedidoDTO>> {
        return try {
            val api = networkSource.getService()
            val response = api.getPedidosPorCliente(idCliente)

            if (response.isSuccessful) {
                // Si la respuesta es exitosa, devolvemos el cuerpo o una lista vacía si el cuerpo es nulo.
                Result.success(response.body() ?: emptyList())
            } else {
                // Si la respuesta no fue exitosa (ej: 404), devolvemos una lista vacía.
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            // Para cualquier otro error de más bajo nivel (ej: sin conexión), propagamos la falla.
            Result.failure(e)
        }
    }

    suspend fun cambiarEstadoPedido(idPedido: Long, idEstado: Long): Result<PedidoDTO> {
        return try {
            val api = networkSource.getService()
            val pedidoActualizado = api.cambiarEstadoPedido(idPedido, idEstado)
            Result.success(pedidoActualizado)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}