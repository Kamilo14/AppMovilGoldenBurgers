package com.example.goldenburgers.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

/**
 * Este es el DAO (Data Access Object) para mi entidad User.
 * Es una interfaz que define todas las operaciones de base de datos que puedo hacer
 * con la tabla 'users'. Room se encarga de generar el código necesario para implementar estas
 * funciones, yo solo tengo que definirlas con sus anotaciones.
 */
@Dao
interface UserDao {

    /**
     * Esta función me permite insertar un nuevo usuario en la tabla.
     * La anotación `@Insert` le dice a Room que esta es una operación de inserción.
     * `onConflict = OnConflictStrategy.ABORT` es una regla de seguridad muy importante:
     * si intento insertar un usuario con un email que ya existe (gracias al índice único
     * que definí en la entidad User), la operación se cancelará y lanzará una excepción.
     * Esto previene que haya usuarios duplicados.
     * La marco como `suspend` porque las operaciones de base de datos deben ser asíncronas
     * para no bloquear el hilo principal de la aplicación.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: User)

    /**
     * Esta es la función que usaré para el login. Busca un usuario en la tabla
     * usando su email como referencia.
     * La anotación `@Query` me permite escribir sentencias SQL directamente.
     * ":email" es un parámetro que se reemplazará por el valor que le pase a la función.
     * `LIMIT 1` es una optimización para que la búsqueda se detenga en cuanto encuentre
     * al primer usuario, ya que sé que los emails son únicos.
     * Devuelve un `User?` (nulable), porque puede que no encuentre a ningún usuario con ese email.
     */
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    /**
     * Esta función me permite actualizar los datos de un usuario que ya existe.
     * La anotación `@Update` es muy conveniente. Room automáticamente busca al usuario
     * por su clave primaria (`id`) y actualiza todas las demás columnas con los
     * valores del objeto `user` que le paso.
     * La usaré en la pantalla de "Editar Perfil".
     */
    @Update
    suspend fun updateUser(user: User)
}
