package com.example.goldenburgers.repository

import com.example.goldenburgers.model.SessionManager
import com.example.goldenburgers.model.data.Ciudad
import com.example.goldenburgers.model.data.Cliente
import com.example.goldenburgers.model.data.DireccionCliente
import com.example.goldenburgers.model.dto.ActualizarPerfilClienteRequest
import com.example.goldenburgers.model.dto.CrearDireccionRequest
import com.example.goldenburgers.model.mapper.toDomain
import com.example.goldenburgers.service.AuthenticatedRetrofitClient
import com.example.goldenburgers.service.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Repository para gestión de clientes y direcciones
 * Ahora recibe SessionManager para manejar la autenticación
 */
class ClienteRepository(
    private val sessionManager: SessionManager
) {

    // Helper para obtener el servicio correcto (con o sin token)
    private suspend fun getApiService(): com.example.goldenburgers.service.GestionUsuarioApiService {
        val token = sessionManager.authTokenFlow.first()
        return if (!token.isNullOrBlank()) {
            AuthenticatedRetrofitClient(token).gestionUsuarioService
        } else {
            RetrofitClient.gestionUsuarioService
        }
    }

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
            val api = getApiService()
            val response = api.obtenerClientePorFirebaseUid(firebaseUid)

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
            val api = getApiService()
            val response = api.obtenerClientePorId(idCliente)

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
     * Actualizar propio perfil del cliente autenticado
     *
     * @param nombreCliente Nombre del cliente
     * @param email Email del cliente
     * @param telefonoCliente Teléfono del cliente (opcional)
     * @return Cliente actualizado
     */
    suspend fun actualizarPerfil(
        nombreCliente: String,
        email: String,
        telefonoCliente: String?
    ): Result<Cliente> {
        return try {
            val request = ActualizarPerfilClienteRequest(
                nombreCliente = nombreCliente,
                email = email,
                telefonoCliente = telefonoCliente
            )

            val api = getApiService()
            val response = api.actualizarPerfil(request)

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
     * Crear nueva dirección para un cliente
     *
     * @param idCliente ID del cliente (requerido por backend)
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

            val api = getApiService()
            val response = api.crearDireccion(request)

            if (response.isSuccessful && response.body() != null) {
                val direccionCliente = response.body()!!.toDomain()
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
            val api = getApiService()
            val response = api.obtenerDirecciones(idCliente)

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
     * Actualizar una dirección existente
     *
     * @param idDireccion ID de la dirección a actualizar
     * @param idCiudad ID de la nueva ciudad
     * @param direccion Nueva dirección completa
     * @param alias Nuevo alias (opcional)
     * @return Dirección actualizada
     */
    suspend fun actualizarDireccion(
        idDireccion: Long,
        idCiudad: Long,
        direccion: String,
        alias: String?
    ): Result<DireccionCliente> {
        return try {
            // Obtenemos el ID del cliente actual del estado o pasamos 0 si no lo tenemos (debería tenerse)
            val currentIdCliente = _currentCliente.value?.idCliente ?: 0L
            
            val request = CrearDireccionRequest(
                idCliente = currentIdCliente, // Necesario pasar idCliente aunque sea actualizar
                idCiudad = idCiudad,
                direccion = direccion,
                alias = alias
            )

            val api = getApiService()
            val response = api.actualizarDireccion(idDireccion, request)

            if (response.isSuccessful && response.body() != null) {
                val direccionCliente = response.body()!!.toDomain()
                Result.success(direccionCliente)
            } else {
                Result.failure(Exception("Error al actualizar dirección: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Eliminar una dirección
     *
     * @param idDireccion ID de la dirección a eliminar
     */
    suspend fun eliminarDireccion(idDireccion: Long): Result<Unit> {
        return try {
            val api = getApiService()
            val response = api.eliminarDireccion(idDireccion)

            if (response.isSuccessful) {
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
            // Este endpoint suele ser público, pero por consistencia usamos getApiService()
            // Si es público y no hay token, usa el cliente público. Si hay token, usa el autenticado.
            val api = getApiService()
            val response = api.obtenerCiudades()

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
