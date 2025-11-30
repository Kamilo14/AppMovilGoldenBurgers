package com.example.goldenburgers.repository

import com.example.goldenburgers.model.data.Ciudad
import com.example.goldenburgers.model.data.Cliente
import com.example.goldenburgers.model.data.DireccionCliente
import com.example.goldenburgers.model.dto.ActualizarPerfilClienteRequest
import com.example.goldenburgers.model.dto.CrearDireccionRequest
import com.example.goldenburgers.model.mapper.toDomain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * [CORREGIDO] El repositorio ahora depende de ClienteNetworkSource para obtener el servicio de API.
 */
class ClienteRepository(
    private val networkSource: ClienteNetworkSource
) {

    private val _currentCliente = MutableStateFlow<Cliente?>(null)
    val currentCliente: StateFlow<Cliente?> = _currentCliente.asStateFlow()

    suspend fun obtenerClientePorFirebaseUid(firebaseUid: String): Result<Cliente> {
        return try {
            val api = networkSource.getService()
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

    suspend fun obtenerClientePorId(idCliente: Long): Result<Cliente> {
        return try {
            val api = networkSource.getService()
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

    suspend fun actualizarPerfil(
        nombreCliente: String,
        email: String,
        telefonoCliente: String?
    ): Result<Cliente> {
        return try {
            val request = ActualizarPerfilClienteRequest(nombreCliente, email, telefonoCliente)
            val api = networkSource.getService()
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

    suspend fun crearDireccion(
        idCliente: Long,
        idCiudad: Long,
        direccion: String,
        alias: String?
    ): Result<DireccionCliente> {
        return try {
            val request = CrearDireccionRequest(idCliente, idCiudad, direccion, alias)
            val api = networkSource.getService()
            val response = api.crearDireccion(request)

            if (response.isSuccessful && response.body() != null) {
                val direccionCliente = response.body()!!.toDomain()
                _currentCliente.update { it?.copy(direcciones = it.direcciones + direccionCliente) }
                Result.success(direccionCliente)
            } else {
                Result.failure(Exception("Error al crear dirección: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun obtenerDirecciones(idCliente: Long): Result<List<DireccionCliente>> {
        return try {
            val api = networkSource.getService()
            val response = api.obtenerDirecciones(idCliente)

            if (response.isSuccessful && response.body() != null) {
                val direcciones = response.body()!!.map { it.toDomain() }
                _currentCliente.update { it?.copy(direcciones = direcciones) }
                Result.success(direcciones)
            } else {
                Result.failure(Exception("Error al obtener direcciones: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun actualizarDireccion(
        idDireccion: Long,
        idCiudad: Long,
        direccion: String,
        alias: String?
    ): Result<DireccionCliente> {
        return try {
            val currentIdCliente = _currentCliente.value?.idCliente ?: return Result.failure(Exception("Cliente no encontrado"))
            val request = CrearDireccionRequest(currentIdCliente, idCiudad, direccion, alias)
            val api = networkSource.getService()
            val response = api.actualizarDireccion(idDireccion, request)

            if (response.isSuccessful && response.body() != null) {
                val direccionActualizada = response.body()!!.toDomain()
                _currentCliente.update { clienteActual ->
                    clienteActual?.copy(direcciones = clienteActual.direcciones.map {
                        if (it.idDireccion == idDireccion) direccionActualizada else it
                    })
                }
                Result.success(direccionActualizada)
            } else {
                Result.failure(Exception("Error al actualizar dirección: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun eliminarDireccion(idDireccion: Long): Result<Unit> {
        return try {
            val api = networkSource.getService()
            val response = api.eliminarDireccion(idDireccion)

            if (response.isSuccessful) {
                _currentCliente.update { clienteActual ->
                    clienteActual?.copy(
                        direcciones = clienteActual.direcciones.filterNot { it.idDireccion == idDireccion }
                    )
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error al eliminar dirección: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun obtenerCiudades(): Result<List<Ciudad>> {
        return try {
            val api = networkSource.getService()
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

    fun clearCurrentCliente() {
        _currentCliente.value = null
    }
}