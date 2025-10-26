package com.example.goldenburgers.model // Asegúrate que el paquete coincida


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.goldenburgers.model.Producto
import kotlinx.coroutines.flow.Flow


/**
 * Data Access Object (DAO) para la entidad Producto.
 * Define los métodos para interactuar con la tabla 'productos' en la base de datos.
 */
@Dao
interface ProductDao {


    /**
     * Obtiene todos los productos de la tabla, ordenados por ID de forma ascendente.
     * @return Un Flow que emite la lista de productos cada vez que cambian los datos.
     */
    @Query("SELECT * FROM productos ORDER BY id ASC")
    fun getAllProducts(): Flow<List<Producto>>


    /**
     * Obtiene solo los productos que han sido marcados como favoritos (es_favorito = 1).
     * @return Un Flow que emite la lista de productos favoritos cada vez que cambian los datos.
     */
    @Query("SELECT * FROM productos WHERE es_favorito = 1 ORDER BY id ASC")
    fun getFavoriteProducts(): Flow<List<Producto>>


    /**
     * Inserta una lista de productos en la base de datos.
     * Si un producto con la misma clave primaria (id) ya existe, será reemplazado.
     * Esta función es 'suspend' porque puede ser una operación larga y debe ejecutarse en segundo plano.
     * @param products La lista de productos a insertar.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<Producto>)


    /**
     * Actualiza el estado de 'es_favorito' de un producto específico, identificado por su ID.
     * Esta función es 'suspend' porque es una operación de escritura en la base de datos.
     * @param productId El ID del producto a actualizar.
     * @param isFavorite El nuevo estado de favorito (true o false).
     */
    @Query("UPDATE productos SET es_favorito = :isFavorite WHERE id = :productId")
    suspend fun updateFavorite(productId: Int, isFavorite: Boolean)


    /**
     * Cuenta el número total de productos en la tabla.
     * Se utiliza para verificar si la base de datos necesita ser poblada inicialmente.
     * Esta función es 'suspend' porque accede a la base de datos.
     * @return El número total de productos en la tabla.
     */
    @Query("SELECT COUNT(*) FROM productos")
    suspend fun getProductCount(): Int
}






