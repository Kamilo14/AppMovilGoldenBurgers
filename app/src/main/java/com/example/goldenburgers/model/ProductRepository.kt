package com.example.goldenburgers.model

import kotlinx.coroutines.flow.Flow

/**
 * El Repositorio es una clase clave en mi arquitectura MVVM.
 * Su trabajo es ser la ÚNICA fuente de verdad para los datos de la aplicación.
 * Abstrae el origen de los datos (en este caso, los DAOs de Room) para que los ViewModels
 * no necesiten saber si los datos vienen de una base de datos, una API en internet, etc.
 */
class ProductRepository(
    // Recibo los DAOs a través del constructor. Esto es un principio de Inyección de Dependencias
    // que hace que mi código sea más modular y fácil de testear.
    private val productDao: ProductDao,
    private val userDao: UserDao
) {

    // --- Operaciones de Productos ---

    // Expongo un Flow con la lista de todos los productos. Los ViewModels pueden "escuchar"
    // este Flow y la UI se actualizará automáticamente cuando los datos cambien en la BD.
    val allProducts: Flow<List<Producto>> = productDao.getAllProducts()

    // Hago lo mismo para los productos favoritos. Es una consulta diferente que también se expone como un Flow.
    val favoriteProducts: Flow<List<Producto>> = productDao.getFavoriteProducts()

    /**
     * Esta función actualiza el estado de favorito de un producto.
     * La marco como `suspend` porque es una operación de base de datos que no debe correr en el hilo principal.
     */
    suspend fun updateFavorite(productId: Int, isFavorite: Boolean) {
        // Simplemente le paso la orden al DAO correspondiente.
        productDao.updateFavorite(productId, isFavorite)
    }

    // --- Operaciones de Usuarios ---

    /**
     * Registra un nuevo usuario en la base de datos.
     * El ViewModel del registro llamará a esta función.
     */
    suspend fun registerUser(user: User) {
        // Delego la operación de inserción al UserDao.
        userDao.insertUser(user)
    }

    /**
     * Busca un usuario por su email. Será fundamental para la lógica del Login.
     * Devuelve un objeto User si lo encuentra, o null si no existe.
     */
    suspend fun findUserByEmail(email: String): User? {
        return userDao.getUserByEmail(email)
    }

    /**
     * Actualiza los datos de un usuario existente en la base de datos.
     * El ViewModel de edición de perfil usará esta función.
     */
    suspend fun updateUser(user: User) {
        // El UserDao se encarga de encontrar al usuario por su ID (clave primaria) y actualizarlo.
        userDao.updateUser(user)
    }
}
