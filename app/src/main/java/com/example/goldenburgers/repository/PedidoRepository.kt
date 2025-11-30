package com.example.goldenburgers.repository

import com.example.goldenburgers.model.SessionManager
import com.example.goldenburgers.model.dto.PedidoDTO
import com.example.goldenburgers.service.AuthenticatedRetrofitClient
import com.example.goldenburgers.service.PedidoApiService
import kotlinx.coroutines.flow.first

/**
 * Repositorio para gestionar todas las operaciones relacionadas con los Pedidos.
 */
class PedidoRepository(
    private val sessionManager: SessionManager
) {

    // [CORREGIDO] Lógica más estricta. Lanza una excepción si el token no está disponible.
    private suspend fun getPedidoApiService(): PedidoApiService {
        val token = sessionManager.authTokenFlow.first()
            ?: throw IllegalStateException("Token de autenticación no disponible. No se puede realizar la operación.")
        
        if (token.isBlank()) {
            throw IllegalStateException("Token de autenticación está vacío. No se puede realizar la operación.")
        }

        return AuthenticatedRetrofitClient(token).pedidoService
    }

    /**
     * Envía un nuevo pedido completo al backend.
     */
    suspend fun crearPedidoCompleto(pedido: PedidoDTO): Result<PedidoDTO> {
        return try {
            val api = getPedidoApiService()
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
            val api = getPedidoApiService()
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
            val api = getPedidoApiService()
            val pedidoActualizado = api.cambiarEstadoPedido(idPedido, idEstado)
            Result.success(pedidoActualizado)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}