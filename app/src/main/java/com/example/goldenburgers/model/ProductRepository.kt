package com.example.goldenburgers.model

import android.util.Log
import com.example.goldenburgers.model.data.Producto
import com.example.goldenburgers.repository.FavoritesRepository
import com.example.goldenburgers.service.AuthenticatedRetrofitClient
import com.example.goldenburgers.service.RetrofitClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map // [CORREGIDO] Importación añadida

class ProductRepository(
    private val sessionManager: SessionManager,
    private val favoritesRepository: FavoritesRepository
) {

    val allProducts: Flow<List<Producto>> = flow {
        try {
            val token = sessionManager.authTokenFlow.first()
            val service = if (!token.isNullOrBlank()) {
                AuthenticatedRetrofitClient(token).catalogoService
            } else {
                RetrofitClient.catalogoService
            }
            val productosDTO = service.getProductos()
            val productos = productosDTO.map { dto ->
                val fixedUrl = dto.imagen?.replace("localhost", "10.0.2.2")?.replace("http://127.0.0.1", "http://10.0.2.2")
                Producto(
                    idProducto = dto.id,
                    idCategoria = dto.idCategoria ?: 0,
                    nombreProducto = dto.nombre,
                    descripcion = dto.descripcion,
                    precioBase = dto.precio,
                    imagenUrl = fixedUrl,
                    disponible = if (dto.disponible) 1 else 0,
                    categoria = dto.categoria ?: "Sin Categoría"
                )
            }
            emit(productos)
        } catch (e: Exception) {
            Log.e("ProductRepository", "Error obteniendo productos", e)
            emit(emptyList())
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val favoriteProducts: Flow<List<Producto>> = allProducts.flatMapLatest { products ->
        favoritesRepository.favoriteProductIds.map { favoriteIds ->
            products.filter { it.idProducto in favoriteIds }
        }
    }

    suspend fun addFavorite(productId: Long) {
        favoritesRepository.addFavorite(productId)
    }

    suspend fun removeFavorite(productId: Long) {
        favoritesRepository.removeFavorite(productId)
    }
}