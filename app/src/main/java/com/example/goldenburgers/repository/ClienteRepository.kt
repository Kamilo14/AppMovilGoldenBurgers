package com.example.goldenburgers.repository

import com.example.goldenburgers.model.data.Ciudad
import com.example.goldenburgers.model.data.Cliente
import com.example.goldenburgers.model.data.DireccionCliente
import com.example.goldenburgers.model.dto.ActualizarPerfilClienteRequest
import com.example.goldenburgers.model.dto.CrearDireccionRequest
import com.example.goldenburgers.model.mapper.toDomain
import com.example.goldenburgers.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository para gestión de clientes y direcciones
 */
class ClienteRepository {

    private val gestionUsuarioApi = RetrofitClient.gestionUsuarioService

    // Estado del cliente actual (caché local)
    private val _currentCliente = MutableStateFlow<Cliente?>(null)
    val currentCliente: StateFlow<Cliente?> = _currentCliente.asStateFlow()

    /**
     * Obtener cliente por Firebase UID
     *
     * @param firebaseUid UID de Firebase del usuario
     * @return Cliente con toda su información
     */
    suspend fun obtenerClientePorFirebaseUid(firebaseUid: String): Result<Cliente> {
        return try {
            val response = gestionUsuarioApi.obtenerClientePorFirebaseUid(firebaseUid)

            if (response.isSuccessful && response.body() != null) {
                val cliente = response.body()!!.toDomain()
                _currentCliente.value = cliente
                Result.success(cliente)
            } else {
                Result.failure(Exception("Cliente no encontrado: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtener cliente por ID
     *
     * @param idCliente ID del cliente
     * @return Cliente con toda su información
     */
    suspend fun obtenerClientePorId(idCliente: Long): Result<Cliente> {
        return try {
            val response = gestionUsuarioApi.obtenerClientePorId(idCliente)

            if (response.isSuccessful && response.body() != null) {
                val cliente = response.body()!!.toDomain()
                _currentCliente.value = cliente
                Result.success(cliente)
            } else {
                Result.failure(Exception("Cliente no encontrado: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Actualizar perfil del cliente
     *
     * @param idCliente ID del cliente
     * @param nombreCliente Nombre del cliente
     * @param telefonoCliente Teléfono del cliente (opcional)
     * @return Cliente actualizado
     */
    suspend fun actualizarPerfil(
        idCliente: Long,
        nombreCliente: String,
        telefonoCliente: String?
    ): Result<Cliente> {
        return try {
            val request = ActualizarPerfilClienteRequest(
                nombreCliente = nombreCliente,
                telefonoCliente = telefonoCliente
            )

            val response = gestionUsuarioApi.actualizarPerfilCliente(idCliente, request)

            if (response.isSuccessful && response.body() != null) {
                val cliente = response.body()!!.toDomain()
                _currentCliente.value = cliente
                Result.success(cliente)
            } else {
                Result.failure(Exception("Error al actualizar perfil: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Crear nueva dirección para el cliente
     *
     * @param idCliente ID del cliente
     * @param idCiudad ID de la ciudad
     * @param direccion Dirección completa
     * @param alias Alias de la dirección (opcional)
     * @return Dirección creada
     */
    suspend fun crearDireccion(
        idCliente: Long,
        idCiudad: Long,
        direccion: String,
        alias: String?
    ): Result<DireccionCliente> {
        return try {
            val request = CrearDireccionRequest(
                idCliente = idCliente,
                idCiudad = idCiudad,
                direccion = direccion,
                alias = alias
            )

            val response = gestionUsuarioApi.crearDireccion(idCliente, request)

            if (response.isSuccessful && response.body() != null) {
                val direccionCliente = response.body()!!.toDomain()
                // Actualizar cliente en caché con la nueva dirección
                obtenerClientePorId(idCliente)
                Result.success(direccionCliente)
            } else {
                Result.failure(Exception("Error al crear dirección: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtener direcciones de un cliente
     *
     * @param idCliente ID del cliente
     * @return Lista de direcciones
     */
    suspend fun obtenerDirecciones(idCliente: Long): Result<List<DireccionCliente>> {
        return try {
            val response = gestionUsuarioApi.obtenerDirecciones(idCliente)

            if (response.isSuccessful && response.body() != null) {
                val direcciones = response.body()!!.map { it.toDomain() }
                Result.success(direcciones)
            } else {
                Result.failure(Exception("Error al obtener direcciones: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Eliminar una dirección
     *
     * @param idCliente ID del cliente
     * @param idDireccion ID de la dirección a eliminar
     */
    suspend fun eliminarDireccion(idCliente: Long, idDireccion: Long): Result<Unit> {
        return try {
            val response = gestionUsuarioApi.eliminarDireccion(idCliente, idDireccion)

            if (response.isSuccessful) {
                // Actualizar cliente en caché
                obtenerClientePorId(idCliente)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error al eliminar dirección: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtener lista de ciudades disponibles
     *
     * @return Lista de ciudades
     */
    suspend fun obtenerCiudades(): Result<List<Ciudad>> {
        return try {
            val response = gestionUsuarioApi.obtenerCiudades()

            if (response.isSuccessful && response.body() != null) {
                val ciudades = response.body()!!.map { it.toDomain() }
                Result.success(ciudades)
            } else {
                Result.failure(Exception("Error al obtener ciudades: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Limpiar caché del cliente actual (al cerrar sesión)
     */
    fun clearCurrentCliente() {
        _currentCliente.value = null
    }
}
