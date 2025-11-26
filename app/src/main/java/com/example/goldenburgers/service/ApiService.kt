package com.example.goldenburgers.service

import com.example.goldenburgers.model.dto.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Servicio API para gestión de usuarios y clientes
 * Endpoints del microservicio GESTIONUSUARIO
 */
interface GestionUsuarioApiService {

    // --- Endpoints de Clientes ---

    /**
     * Registrar un nuevo cliente
     * POST /api/clientes
     */
    @POST("api/clientes")
    suspend fun registrarCliente(
        @Body request: RegistrarClienteRequest
    ): Response<ClienteDTO>

    /**
     * Obtener cliente por ID
     * GET /api/clientes/{id}
     */
    @GET("api/clientes/{id}")
    suspend fun obtenerClientePorId(
        @Path("id") idCliente: Long
    ): Response<ClienteDTO>

    /**
     * Obtener cliente por Firebase UID
     * GET /api/clientes/usuario/{firebaseUid}
     */
    @GET("api/clientes/usuario/{firebaseUid}")
    suspend fun obtenerClientePorFirebaseUid(
        @Path("firebaseUid") firebaseUid: String
    ): Response<ClienteDTO>

    /**
     * Actualizar propio perfil de cliente (nombre/teléfono)
     * PUT /api/clientes/perfil
     */
    @PUT("api/clientes/perfil")
    suspend fun actualizarPerfil(
        @Body request: ActualizarPerfilClienteRequest
    ): Response<ClienteDTO>

    // --- Endpoints de Direcciones ---

    /**
     * Crear nueva dirección para el cliente autenticado
     * POST /api/clientes/direcciones
     */
    @POST("api/clientes/direcciones")
    suspend fun crearDireccion(
        @Body request: CrearDireccionRequest
    ): Response<DireccionClienteDTO>

    /**
     * Obtener direcciones de un cliente
     * GET /api/clientes/{idCliente}/direcciones
     */
    @GET("api/clientes/{idCliente}/direcciones")
    suspend fun obtenerDirecciones(
        @Path("idCliente") idCliente: Long
    ): Response<List<DireccionClienteDTO>>

    /**
     * Actualizar una dirección existente
     * PUT /api/clientes/direcciones/{id}
     */
    @PUT("api/clientes/direcciones/{id}")
    suspend fun actualizarDireccion(
        @Path("id") idDireccion: Long,
        @Body request: CrearDireccionRequest
    ): Response<DireccionClienteDTO>

    /**
     * Eliminar una dirección
     * DELETE /api/clientes/direcciones/{id}
     */
    @DELETE("api/clientes/direcciones/{id}")
    suspend fun eliminarDireccion(
        @Path("id") idDireccion: Long
    ): Response<Unit>

    // --- Endpoints de Ciudades ---

    /**
     * Obtener todas las ciudades disponibles
     * GET /api/ciudades
     */
    @GET("api/ciudades")
    suspend fun obtenerCiudades(): Response<List<CiudadDTO>>
}

/**
 * Servicio API para autenticación
 * Endpoints del API Gateway
 */
interface ApiGatewayService {

    /**
     * Login de usuario
     * POST /api/auth/login
     */
    @POST("api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    /**
     * Refresh token
     * POST /api/auth/refresh
     */
    @POST("api/auth/refresh")
    suspend fun refreshToken(
        @Body refreshToken: String
    ): Response<LoginResponse>
}
