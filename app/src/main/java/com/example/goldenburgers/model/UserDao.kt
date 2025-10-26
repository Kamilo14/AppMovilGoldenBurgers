package com.example.goldenburgers.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

/**
 * Este es el DAO (Data Access Object) para la entidad User.
 * Es una interfaz que define todas las operaciones de base de datos que se puede hacer
 * con la tabla 'users'.
 */
@Dao
interface UserDao {

    /**
     * Esta función me permite insertar un nuevo usuario en la tabla.
     * La anotación `@Insert` le dice a Room que esta es una operación de inserción.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: User)

    /**
     * Buscar un usuario en la tabla usando su email como referencia.
     * Devuelve un `User?` (nulable), porque puede que no encuentre a ningún usuario con ese email.
     */
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    /**
     * Esta función permite actualizar los datos de un usuario que ya existe.
     */
    @Update
    suspend fun updateUser(user: User)
}
