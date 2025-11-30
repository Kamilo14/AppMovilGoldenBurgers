package com.example.goldenburgers.model

import com.example.goldenburgers.model.data.Producto
import com.example.goldenburgers.repository.FavoritesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class ProductRepository(
    private val networkSource: ProductNetworkSource,
    private val favoritesRepository: FavoritesRepository
) {

    // [CORRECCIÓN CLAVE] Se usa el operador .catch para manejar errores de forma segura
    val allProducts: Flow<List<Producto>> = flow {
        val productosDTO = networkSource.fetchProducts()
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
    }.catch { 
        // Si el flow anterior falla (p.ej. error de red), emitimos una lista vacía
        emit(emptyList()) 
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