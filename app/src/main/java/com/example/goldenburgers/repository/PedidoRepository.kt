package com.example.goldenburgers.repository

import com.example.goldenburgers.model.dto.PedidoDTO

/**
 * Repositorio para gestionar todas las operaciones relacionadas con los Pedidos.
 * [CORREGIDO] Ahora depende de PedidoNetworkSource para la lógica de red.
 */
class PedidoRepository(
    private val networkSource: PedidoNetworkSource
) {

    /**
     * Envía un nuevo pedido completo al backend.
     */
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
     */
    suspend fun getPedidosPorCliente(idCliente: Long): Result<List<PedidoDTO>> {
        return try {
            val api = networkSource.getService()
            val pedidos = api.getPedidosPorCliente(idCliente)
            Result.success(pedidos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cambia el estado de un pedido existente.
     */
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