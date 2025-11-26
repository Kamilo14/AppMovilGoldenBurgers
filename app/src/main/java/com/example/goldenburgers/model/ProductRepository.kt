package com.example.goldenburgers.model

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * TODO: Este es un ProductRepository temporal/stub para que compile.
 * El desarrollador encargado del catálogo debe implementar esto con el backend real.
 *
 * ProductRepository temporal para que CatalogViewModel compile.
 * Este debe ser reemplazado con la implementación real del backend de catálogo.
 */
class ProductRepository {

    // Flujo vacío de productos - retorna lista vacía
    fun getAllProducts(): Flow<List<Producto>> = flowOf(emptyList())

    // Flujo vacío de favoritos - retorna lista vacía
    fun getFavorites(): Flow<List<Producto>> = flowOf(emptyList())

    // Método stub para toggle favorito - no hace nada
    suspend fun toggleFavorite(productId: Int) {
        // TODO: Implementar con backend
    }

    // Método stub para obtener usuario - retorna null
    suspend fun getUserByEmail(email: String): User? {
        // TODO: Implementar con backend
        return null
    }
}

/**
 * TODO: Modelo temporal User para que compile.
 * Debe ser reemplazado por el modelo real del backend.
 */
data class User(
    val email: String = "",
    val fullName: String = ""
)
