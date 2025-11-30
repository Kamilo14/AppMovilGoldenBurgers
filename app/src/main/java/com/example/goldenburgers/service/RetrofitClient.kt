package com.example.goldenburgers.service

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Cliente Retrofit para comunicación con el backend a través del API Gateway
 * Todas las peticiones pasan por el API Gateway
 */
object RetrofitClient {

    /**
     * URL base del API Gateway
     * Todas las peticiones se enrutan a través del gateway
     */
    private const val API_GATEWAY_BASE_URL = "http://161.153.219.128:8080/api/"

    /**
     * Interceptor para logging de requests/responses
     * Útil para debugging - muestra toda la información de las peticiones HTTP
     */
    val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    /**
     * Cliente HTTP básico con configuración de timeouts y logging
     */
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Instancia de Retrofit para API Gateway
     * Todas las peticiones usan esta misma instancia
     */
    private val retrofit = Retrofit.Builder()
        .baseUrl(API_GATEWAY_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    /**
     * Servicio API Gateway (autenticación)
     */
    val apiGatewayService: ApiGatewayService by lazy {
        retrofit.create(ApiGatewayService::class.java)
    }

    /**
     * Servicio Gestión de Usuarios (clientes, direcciones, ciudades)
     * Todas las peticiones pasan por el API Gateway
     */
    val gestionUsuarioService: GestionUsuarioApiService by lazy {
        retrofit.create(GestionUsuarioApiService::class.java)
    }

    val catalogoService: CatalogoApiService by lazy {
        retrofit.create(CatalogoApiService::class.java)
    }
    
    val ventaService: VentaApiService by lazy {
        retrofit.create(VentaApiService::class.java)
    }

    val pedidoService: PedidoApiService by lazy {
        retrofit.create(PedidoApiService::class.java)
    }
}

/**
 * Cliente Retrofit con autenticación (incluye JWT token en headers)
 * Usar este cliente cuando el usuario ya está autenticado
 */
class AuthenticatedRetrofitClient(private val token: String) {

    /**
     * Interceptor que agrega el JWT token a todas las requests
     */
    private val authInterceptor = okhttp3.Interceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .build()
        chain.proceed(request)
    }

    /**
     * Cliente HTTP con autenticación
     */
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(RetrofitClient.loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("http://161.153.219.128:8080/api/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    /**
     * Servicio Gestión de Usuarios autenticado
     */
    val gestionUsuarioService: GestionUsuarioApiService by lazy {
        retrofit.create(GestionUsuarioApiService::class.java)
    }

    val catalogoService: CatalogoApiService by lazy {
        retrofit.create(CatalogoApiService::class.java)
    }

    val ventaService: VentaApiService by lazy {
        retrofit.create(VentaApiService::class.java)
    }

    val pedidoService: PedidoApiService by lazy {
        retrofit.create(PedidoApiService::class.java)
    }
}
