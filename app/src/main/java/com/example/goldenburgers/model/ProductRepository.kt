package com.example.goldenburgers.model

import android.util.Log
import com.example.goldenburgers.model.data.Producto
import com.example.goldenburgers.model.dto.ProductoDTO
import com.example.goldenburgers.service.AuthenticatedRetrofitClient
import com.example.goldenburgers.service.RetrofitClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

/**
 * Abstrae el origen de los datos.
 * Ahora obtiene los productos desde la API en lugar de la base de datos local.
 */
class ProductRepository(
    private val sessionManager: SessionManager // Inyectamos SessionManager para obtener el token
) {

    // --- Operaciones de Productos ---

    // Ahora obtenemos los productos de la API
    val allProducts: Flow<List<Producto>> = flow {
        try {
            // 1. Intentamos obtener el token de sesión
            val token = sessionManager.authTokenFlow.first()
            
            Log.d("ProductRepository", "Token disponible: ${token != null}")

            // 2. Elegimos el servicio (Autenticado vs Público)
            val service = if (!token.isNullOrBlank()) {
                Log.d("ProductRepository", "Usando cliente autenticado")
                AuthenticatedRetrofitClient(token).catalogoService
            } else {
                Log.d("ProductRepository", "Usando cliente público")
                RetrofitClient.catalogoService
            }

            // 3. Hacemos la llamada a la API
            val productosDTO = service.getProductos()
            
            // Mapeamos de DTO a modelo de dominio (data/Producto.kt)
            val productos = productosDTO.map { dto ->

                // DEBUG: Imprimir URL de la imagen para depuración
                Log.d("ProductRepository", "Producto: ${dto.nombre}, URL Imagen Original: ${dto.imagen}")

                // --- CORRECCIÓN DE URL PARA EMULADOR ---
                val fixedUrl = dto.imagen?.replace("localhost", "10.0.2.2")
                    ?.replace("http://127.0.0.1", "http://10.0.2.2")

                Log.d("ProductRepository", "Producto: ${dto.nombre}, URL Imagen Fix: $fixedUrl")

                Producto(
                    idProducto = dto.id, // Long?
                    idCategoria = dto.idCategoria ?: 0, // Usamos el ID directo del DTO
                    nombreProducto = dto.nombre,
                    descripcion = dto.descripcion,
                    precioBase = dto.precio,
                    imagenUrl = fixedUrl, // Usamos la URL corregida
                    disponible = if (dto.disponible) 1 else 0, // Convertir Boolean a Int
                    categoria = dto.categoria ?: "Sin Categoría" // Asignamos el nombre de la categoría
                )
            }
            emit(productos)
        } catch (e: Exception) {
            Log.e("ProductRepository", "Error obteniendo productos", e)
            e.printStackTrace()
            emit(emptyList()) // En caso de error, emitimos lista vacía para no romper la UI
        }
    }
    
    // Nota: Para favoritos, como es algo local del usuario, quizás quieras seguir usando Room
    // o una llamada autenticada a la API si el backend soporta favoritos.
    // Por ahora lo dejamos vacío o puedes adaptarlo para devolver Flow<List<Producto>>
    // Si ya no usas Room para productos, esto quizás deba cambiar.
     val favoriteProducts: Flow<List<Producto>> = flow { emit(emptyList()) } 

    /**
     * Actualizar el estado de favorito de un producto
     */
    suspend fun updateFavorite(productId: Int, isFavorite: Boolean) {
       // Pendiente: Implementar lógica local o remota
    }
}
