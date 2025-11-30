package com.example.goldenburgers.repository

import com.example.goldenburgers.model.SessionManager
import com.example.goldenburgers.service.AuthenticatedRetrofitClient
import com.example.goldenburgers.service.PedidoApiService
import kotlinx.coroutines.flow.first

/**
 * Clase dedicada a obtener la instancia correcta del servicio de API para los pedidos.
 */
class PedidoNetworkSource(private val sessionManager: SessionManager) {

    /**
     * Devuelve el servicio de API de pedidos, autenticado con el token actual.
     * Lanza una excepción si el token no está disponible.
     */
    suspend fun getService(): PedidoApiService {
        val token = sessionManager.authTokenFlow.first()
            ?: throw IllegalStateException("Token de autenticación no disponible.")
        
        if (token.isBlank()) {
            throw IllegalStateException("Token de autenticación está vacío.")
        }

        return AuthenticatedRetrofitClient(token).pedidoService
    }
}
