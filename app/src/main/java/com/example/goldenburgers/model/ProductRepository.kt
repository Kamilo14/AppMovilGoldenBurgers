package com.example.goldenburgers.model

import kotlinx.coroutines.flow.Flow

/**
 * Abstrae el origen de los datos (en este caso, los DAOs de Room)
 */
class ProductRepository(
    private val productDao: ProductDao,
    private val userDao: UserDao
) {

    // --- Operaciones de Productos ---

    // Se expone un Flow con la lista de todos los productos.
    val allProducts: Flow<List<Producto>> = productDao.getAllProducts()

    // Se hace el mismo proceso para los productos favoritos
    val favoriteProducts: Flow<List<Producto>> = productDao.getFavoriteProducts()

    /**
     * Actualizar el estado de favorito de un producto
     */
    suspend fun updateFavorite(productId: Int, isFavorite: Boolean) {
        productDao.updateFavorite(productId, isFavorite)
    }

    // --- Operaciones de Usuarios ---

    /**
     * Registra un nuevo usuario en la base de datos.
     */
    suspend fun registerUser(user: User) {
        userDao.insertUser(user)
    }

    /**
     * Buscar un usuario por su email.
     */
    suspend fun findUserByEmail(email: String): User? {
        return userDao.getUserByEmail(email)
    }

    /**
     * Actualizar los datos de un usuario existente en la base de datos.
     */
    suspend fun updateUser(user: User) {
        // El UserDao se encarga de encontrar al usuario por su ID (clave primaria) y actualizarlo.
        userDao.updateUser(user)
    }
}
