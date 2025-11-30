package com.example.goldenburgers.repository

import com.example.goldenburgers.model.SessionManager
import com.example.goldenburgers.model.dto.PedidoDTO
import com.example.goldenburgers.service.AuthenticatedRetrofitClient
import kotlinx.coroutines.flow.first

/**
 * Repositorio para gestionar todas las operaciones relacionadas con los Pedidos.
 */
class PedidoRepository(
    private val sessionManager: SessionManager
) {

    /**
     * Obtiene una instancia autenticada del servicio de pedidos.
     * Lanza una excepción si el token no está disponible, ya que todas las operaciones de pedidos
     * deberían estar autenticadas.
     */
    private suspend fun getPedidoApiService(): com.example.goldenburgers.service.PedidoApiService {
        val token = sessionManager.authTokenFlow.first()
            ?: throw IllegalStateException("El token de autenticación no está disponible.")
        return AuthenticatedRetrofitClient(token).pedidoService
    }

    /**
     * Envía un nuevo pedido completo al backend.
     * @param pedido El objeto PedidoDTO que contiene todos los datos del pedido a crear.
     * @return Un Result que contiene el PedidoDTO creado o una excepción si falla.
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
     * @param idCliente El ID del cliente cuyos pedidos se quieren obtener.
     * @return Un Result con la lista de PedidoDTOs o una excepción si falla.
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
     * @param idPedido El ID del pedido a modificar.
     * @param idEstado El ID del nuevo estado.
     * @return Un Result con el PedidoDTO actualizado o una excepción si falla.
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