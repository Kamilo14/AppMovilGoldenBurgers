package com.example.goldenburgers.repository

import com.example.goldenburgers.model.SessionManager
import com.example.goldenburgers.service.AuthenticatedRetrofitClient
import com.example.goldenburgers.service.GestionUsuarioApiService
import com.example.goldenburgers.service.RetrofitClient
import kotlinx.coroutines.flow.first

/**
 * Clase dedicada a obtener la instancia correcta del servicio de API para el cliente.
 * Esto permite que el ClienteRepository sea más simple y testeable.
 */
class ClienteNetworkSource(private val sessionManager: SessionManager) {

    /**
     * Devuelve el servicio de API, autenticado si hay un token, o el servicio
     * público si no lo hay.
     */
    suspend fun getService(): GestionUsuarioApiService {
        val token = sessionManager.authTokenFlow.first()
        return if (!token.isNullOrBlank()) {
            AuthenticatedRetrofitClient(token).gestionUsuarioService
        } else {
            // Esto probablemente debería ser un error o una instancia no autenticada,
            // pero mantenemos la lógica original por ahora.
            RetrofitClient.gestionUsuarioService
        }
    }
}
