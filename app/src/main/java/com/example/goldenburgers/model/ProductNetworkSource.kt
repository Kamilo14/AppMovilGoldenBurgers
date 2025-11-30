package com.example.goldenburgers.model

import com.example.goldenburgers.model.dto.ProductoDTO
import com.example.goldenburgers.service.AuthenticatedRetrofitClient
import com.example.goldenburgers.service.RetrofitClient
import kotlinx.coroutines.flow.first

/**
 * Clase dedicada exclusivamente a obtener datos de productos desde la red.
 * Esto permite que el ProductRepository sea más simple y testeable.
 */
class ProductNetworkSource(private val sessionManager: SessionManager) {

    /**
     * Obtiene los productos desde el endpoint correspondiente, usando un token
     * de autenticación si está disponible.
     */
    suspend fun fetchProducts(): List<ProductoDTO> {
        val token = sessionManager.authTokenFlow.first()
        val service = if (!token.isNullOrBlank()) {
            AuthenticatedRetrofitClient(token).catalogoService
        } else {
            RetrofitClient.catalogoService
        }
        return service.getProductos()
    }
}
