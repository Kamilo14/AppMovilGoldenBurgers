package com.example.goldenburgers.network

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
     * Actualizar perfil de cliente
     * PUT /api/clientes/{id}
     */
    @PUT("api/clientes/{id}")
    suspend fun actualizarPerfilCliente(
        @Path("id") idCliente: Long,
        @Body request: ActualizarPerfilClienteRequest
    ): Response<ClienteDTO>

    // --- Endpoints de Direcciones ---

    /**
     * Crear nueva dirección para un cliente
     * POST /api/clientes/{idCliente}/direcciones
     */
    @POST("api/clientes/{idCliente}/direcciones")
    suspend fun crearDireccion(
        @Path("idCliente") idCliente: Long,
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
     * Eliminar una dirección
     * DELETE /api/clientes/{idCliente}/direcciones/{idDireccion}
     */
    @DELETE("api/clientes/{idCliente}/direcciones/{idDireccion}")
    suspend fun eliminarDireccion(
        @Path("idCliente") idCliente: Long,
        @Path("idDireccion") idDireccion: Long
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
     * POST /auth/login
     */
    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    /**
     * Refresh token
     * POST /auth/refresh
     */
    @POST("auth/refresh")
    suspend fun refreshToken(
        @Body refreshToken: String
    ): Response<LoginResponse>
}
