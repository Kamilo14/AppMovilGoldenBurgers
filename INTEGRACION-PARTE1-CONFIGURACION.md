# GUÍA DE INTEGRACIÓN - PARTE 1: CONFIGURACIÓN BASE

## Golden Burgers - App Android + Backend Microservicios

---

## ÍNDICE PARTE 1

1. [Resumen del Proyecto](#1-resumen-del-proyecto)
2. [Diferencias Críticas App vs Backend](#2-diferencias-críticas-app-vs-backend)
3. [Paso 1: Agregar Dependencias](#paso-1-agregar-dependencias)
4. [Paso 2: Configurar Retrofit](#paso-2-configurar-retrofit)
5. [Paso 3: Crear ApiService Interface](#paso-3-crear-apiservice-interface)
6. [Paso 4: Configurar Firebase en la App](#paso-4-configurar-firebase-en-la-app)
7. [Paso 5: Crear DTOs para Backend](#paso-5-crear-dtos-para-backend)
8. [Paso 6: Adaptar Modelo User Local](#paso-6-adaptar-modelo-user-local)
9. [Paso 7: Crear Modelo de Favoritos Locales](#paso-7-crear-modelo-de-favoritos-locales)

---

## 1. RESUMEN DEL PROYECTO

### Arquitectura Backend (Java 21 + Spring Boot)

| Microservicio      | Puerto | Función                             |
| ------------------ | ------ | ------------------------------------ |
| API Gateway        | 8080   | Punto de entrada, autenticación JWT |
| Gestión Usuario   | 8081   | Usuarios, clientes, direcciones      |
| Gestión Venta     | 8082   | Ventas, boletas, dashboard           |
| Gestión Pedido    | 8083   | Pedidos, pagos, Mercado Pago         |
| Gestión Catálogo | 8084   | Productos, categorías               |
| Gestión Contacto  | 8085   | Mensajes de contacto                 |

### Base de Datos

- **Oracle Autonomous Database** (Cloud)
- Usuario: `GOLDENBURGERSDB`

### App Android Actual

- **Arquitectura**: MVVM con Jetpack Compose
- **BD Local**: Room (SQLite)
- **Autenticación actual**: Local (sin Firebase)
- **Red**: No implementada

---

## 2. DIFERENCIAS CRÍTICAS APP VS BACKEND

### Tabla Comparativa

| Aspecto        | App Actual                       | Backend                   | Acción Requerida           |
| -------------- | -------------------------------- | ------------------------- | --------------------------- |
| Autenticación | Room local con password          | Firebase Auth + JWT       | Implementar Firebase Auth   |
| ID Usuario     | `Int` autoincrement            | `String` Firebase UID   | Cambiar tipo de dato        |
| Dirección     | 5 campos separados               | 1 campo concatenado       | Concatenar al enviar        |
| Productos      | Drawables locales (R.drawable)   | URLs de Firebase Storage  | Usar Coil con URLs          |
| Favoritos      | Campo `esFavorito` en Producto | No existe en backend      | Mantener local por usuario  |
| Foto perfil    | URI local                        | No existe en backend      | Mantener persistencia local |
| Pedidos        | No existe                        | Completo con Mercado Pago | Implementar desde cero      |
| Historial      | No existe                        | GET /pedidos/cliente/{id} | Implementar pantalla        |
| Cliente Red    | No existe Retrofit               | Requiere HTTP client      | Implementar Retrofit        |

### Mapeo de Campos: Usuario

| Campo App                                 | Campo Backend               | Notas                                    |
| ----------------------------------------- | --------------------------- | ---------------------------------------- |
| `id: Int`                               | `idCliente: Long`         | Backend usa Long                         |
| —                                        | `idUsuario: String`       | **NUEVO**: Firebase UID            |
| `email: String`                         | `email: String`           | Igual                                    |
| `password: String`                      | —                          | **ELIMINAR**: Firebase maneja auth |
| `fullName: String`                      | `nombreCliente: String`   | Renombrar                                |
| `phoneNumber: String`                   | `telefonoCliente: String` | 9 dígitos, opcional                     |
| `gender: String`                        | —                          | **SOLO LOCAL**                     |
| `birthDate: String`                     | —                          | **SOLO LOCAL**                     |
| `street, number, city, region, commune` | `direccion: String`       | **CONCATENAR**                     |
| `profileImageUri: String?`              | —                          | **SOLO LOCAL**                     |

### Mapeo de Campos: Producto

| Campo App                 | Campo Backend           | Notas                                   |
| ------------------------- | ----------------------- | --------------------------------------- |
| `id: Int`               | `id: Long`            | Cambiar a Long                          |
| `nombre: String`        | `nombre: String`      | Igual                                   |
| `descripcion: String`   | `descripcion: String` | Igual                                   |
| `precio: Double`        | `precio: BigDecimal`  | Compatible                              |
| `imagenReferencia: Int` | `imagen: String`      | **CAMBIAR** a URL                 |
| `categoria: String`     | `categoria: String`   | Igual                                   |
| `esFavorito: Boolean`   | —                      | **SOLO LOCAL**                    |
| —                        | `disponible: Boolean` | **NUEVO**: Filtrar no disponibles |

---

## PASO 1: AGREGAR DEPENDENCIAS

### Archivo: `app/build.gradle.kts`

Agregar las siguientes dependencias:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-kapt")
    // NUEVO: Plugin de Google Services para Firebase
    id("com.google.gms.google-services")
}

android {
    // ... configuración existente ...

    defaultConfig {
        // ... configuración existente ...

        // NUEVO: URL base del backend (cambiar por IP de tu VM Oracle)
        buildConfigField("String", "BASE_URL", "\"http://TU_IP_VM:8080/\"")
    }

    buildFeatures {
        compose = true
        // NUEVO: Habilitar BuildConfig
        buildConfig = true
    }
}

dependencies {
    // ============================================
    // DEPENDENCIAS EXISTENTES (mantener)
    // ============================================

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // ViewModel y Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Coil (ya existe)
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Location
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // ============================================
    // NUEVAS DEPENDENCIAS PARA INTEGRACIÓN
    // ============================================

    // Firebase BOM (Bill of Materials) - gestiona versiones
    implementation(platform("com.google.firebase:firebase-bom:33.6.0"))

    // Firebase Authentication
    implementation("com.google.firebase:firebase-auth-ktx")

    // Retrofit para llamadas HTTP
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // OkHttp para logging e interceptors
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Gson para serialización JSON
    implementation("com.google.code.gson:gson:2.10.1")

    // Coroutines para operaciones asíncronas (probablemente ya existe)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
}
```

### Archivo: `build.gradle.kts` (nivel proyecto)

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    // NUEVO: Plugin de Google Services
    id("com.google.gms.google-services") version "4.4.2" apply false
}
```

### Archivo: `google-services.json`

1. Ir a [Firebase Console](https://console.firebase.google.com/)
2. Seleccionar proyecto `goldenburgers-60680` (el que ya usan en el backend)
3. Configuración del proyecto → Agregar app → Android
4. Nombre del paquete: `com.example.goldenburgers`
5. Descargar `google-services.json`
6. Colocar en: `app/google-services.json`

---

## PASO 2: CONFIGURAR RETROFIT

### Crear estructura de carpetas

```
app/src/main/java/com/example/goldenburgers/
├── data/
│   ├── remote/
│   │   ├── ApiClient.kt
│   │   ├── ApiService.kt
│   │   └── AuthInterceptor.kt
│   ├── local/
│   │   └── ... (archivos existentes de Room)
│   └── repository/
│       └── ... (repositories)
```

### Archivo: `data/remote/AuthInterceptor.kt`

```kotlin
package com.example.goldenburgers.data.remote

import com.example.goldenburgers.model.SessionManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor que agrega el token JWT a todas las peticiones HTTP.
 *
 * Funciona así:
 * 1. Antes de cada request, obtiene el token guardado en DataStore
 * 2. Si existe token, lo agrega al header "Authorization: Bearer {token}"
 * 3. La petición continúa con el header agregado
 */
class AuthInterceptor(
    private val sessionManager: SessionManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Obtener token de forma bloqueante (necesario en interceptor)
        val token = runBlocking {
            sessionManager.jwtTokenFlow.first()
        }

        // Si no hay token, continuar sin modificar
        if (token.isNullOrEmpty()) {
            return chain.proceed(originalRequest)
        }

        // Agregar header de autorización
        val newRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $token")
            .header("Content-Type", "application/json")
            .build()

        return chain.proceed(newRequest)
    }
}
```

### Archivo: `data/remote/ApiClient.kt`

```kotlin
package com.example.goldenburgers.data.remote

import com.example.goldenburgers.BuildConfig
import com.example.goldenburgers.model.SessionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton que configura y provee la instancia de Retrofit.
 *
 * Configuración:
 * - Base URL desde BuildConfig (configurable por ambiente)
 * - Interceptor de autenticación (agrega JWT)
 * - Logging para debug
 * - Timeouts de 30 segundos
 */
object ApiClient {

    // URL base del API Gateway
    // IMPORTANTE: Cambiar en build.gradle.kts por la IP de tu VM Oracle
    private const val BASE_URL = BuildConfig.BASE_URL

    private var retrofit: Retrofit? = null
    private var apiService: ApiService? = null

    /**
     * Obtiene la instancia de ApiService.
     *
     * @param sessionManager Para obtener el token JWT
     * @return ApiService configurado
     */
    fun getApiService(sessionManager: SessionManager): ApiService {
        if (apiService == null) {
            retrofit = buildRetrofit(sessionManager)
            apiService = retrofit!!.create(ApiService::class.java)
        }
        return apiService!!
    }

    private fun buildRetrofit(sessionManager: SessionManager): Retrofit {
        val client = buildOkHttpClient(sessionManager)

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private fun buildOkHttpClient(sessionManager: SessionManager): OkHttpClient {
        // Interceptor de logging (solo en debug)
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        // Interceptor de autenticación
        val authInterceptor = AuthInterceptor(sessionManager)

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Resetea el cliente (útil al cerrar sesión).
     */
    fun reset() {
        retrofit = null
        apiService = null
    }
}
```

---

## PASO 3: CREAR APISERVICE INTERFACE

### Archivo: `data/remote/ApiService.kt`

```kotlin
package com.example.goldenburgers.data.remote

import com.example.goldenburgers.data.remote.dto.request.*
import com.example.goldenburgers.data.remote.dto.response.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Interface que define todos los endpoints del backend.
 * Retrofit genera la implementación automáticamente.
 *
 * Base URL: http://{IP_VM}:8080 (API Gateway)
 */
interface ApiService {

    // ============================================
    // AUTENTICACIÓN (API Gateway)
    // ============================================

    /**
     * Login con token de Firebase.
     * El backend valida el token de Firebase y retorna un JWT interno.
     *
     * @param request Contiene el Firebase ID Token
     * @return JWT interno + datos del usuario
     */
    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<AuthResponse>

    /**
     * Renovar token JWT cuando está por expirar.
     */
    @POST("auth/refresh")
    suspend fun refreshToken(): Response<AuthResponse>

    // ============================================
    // CLIENTES (Microservicio Usuario - Puerto 8081)
    // ============================================

    /**
     * Registrar nuevo cliente después de crear cuenta en Firebase.
     *
     * @param request Datos del cliente (idUsuario = Firebase UID)
     */
    @POST("api/clientes")
    suspend fun registrarCliente(
        @Body request: RegistrarClienteRequest
    ): Response<ClienteResponse>

    /**
     * Obtener cliente por Firebase UID.
     * Usar después del login para cargar datos.
     */
    @GET("api/clientes/usuario/{idUsuario}")
    suspend fun getClientePorUsuario(
        @Path("idUsuario") firebaseUid: String
    ): Response<ClienteResponse>

    /**
     * Obtener cliente por ID interno.
     */
    @GET("api/clientes/{id}")
    suspend fun getClientePorId(
        @Path("id") idCliente: Long
    ): Response<ClienteResponse>

    /**
     * Actualizar perfil del cliente autenticado.
     */
    @PUT("api/clientes/perfil")
    suspend fun actualizarPerfil(
        @Body request: ActualizarPerfilRequest
    ): Response<ClienteResponse>

    // ============================================
    // DIRECCIONES (Microservicio Usuario)
    // ============================================

    /**
     * Obtener todas las direcciones de un cliente.
     */
    @GET("api/clientes/{idCliente}/direcciones")
    suspend fun getDirecciones(
        @Path("idCliente") idCliente: Long
    ): Response<List<DireccionResponse>>

    /**
     * Agregar nueva dirección.
     */
    @POST("api/clientes/direcciones")
    suspend fun agregarDireccion(
        @Body request: CrearDireccionRequest
    ): Response<DireccionResponse>

    /**
     * Actualizar dirección existente.
     */
    @PUT("api/clientes/direcciones/{idDireccion}")
    suspend fun actualizarDireccion(
        @Path("idDireccion") idDireccion: Long,
        @Body request: ActualizarDireccionRequest
    ): Response<DireccionResponse>

    /**
     * Eliminar dirección.
     */
    @DELETE("api/clientes/direcciones/{idDireccion}")
    suspend fun eliminarDireccion(
        @Path("idDireccion") idDireccion: Long
    ): Response<Unit>

    // ============================================
    // CIUDADES (Microservicio Usuario)
    // ============================================

    /**
     * Obtener lista de ciudades disponibles.
     * Necesario para crear direcciones (requiere idCiudad).
     */
    @GET("api/ciudades")
    suspend fun getCiudades(): Response<List<CiudadResponse>>

    /**
     * Buscar ciudad por nombre.
     */
    @GET("api/ciudades/nombre/{nombre}")
    suspend fun getCiudadPorNombre(
        @Path("nombre") nombre: String
    ): Response<CiudadResponse>

    // ============================================
    // CATÁLOGO (Microservicio Catálogo - Puerto 8084)
    // ============================================

    /**
     * Obtener todos los productos disponibles.
     * Solo retorna productos con disponible=true.
     */
    @GET("api/catalogo/productos")
    suspend fun getProductos(): Response<List<ProductoResponse>>

    /**
     * Obtener producto por ID.
     */
    @GET("api/catalogo/productos/{id}")
    suspend fun getProductoPorId(
        @Path("id") idProducto: Long
    ): Response<ProductoResponse>

    /**
     * Obtener productos por categoría.
     */
    @GET("api/catalogo/productos/categoria/{idCategoria}")
    suspend fun getProductosPorCategoria(
        @Path("idCategoria") idCategoria: Long
    ): Response<List<ProductoResponse>>

    /**
     * Buscar productos por nombre.
     */
    @GET("api/catalogo/productos/buscar")
    suspend fun buscarProductos(
        @Query("nombre") nombre: String
    ): Response<List<ProductoResponse>>

    /**
     * Obtener todas las categorías.
     */
    @GET("api/catalogo/categorias")
    suspend fun getCategorias(): Response<List<CategoriaResponse>>

    // ============================================
    // PEDIDOS (Microservicio Pedido - Puerto 8083)
    // ============================================

    /**
     * Crear pedido completo con detalles.
     * Este es el endpoint principal para checkout.
     */
    @POST("api/pedidos/completo")
    suspend fun crearPedido(
        @Body request: CrearPedidoRequest
    ): Response<PedidoResponse>

    /**
     * Obtener pedidos de un cliente (historial).
     */
    @GET("api/pedidos/cliente/{idCliente}")
    suspend fun getPedidosCliente(
        @Path("idCliente") idCliente: Long
    ): Response<List<PedidoResponse>>

    /**
     * Obtener detalle de un pedido específico.
     */
    @GET("api/pedidos/{id}")
    suspend fun getPedidoPorId(
        @Path("id") idPedido: Long
    ): Response<PedidoResponse>

    /**
     * Cancelar pedido (solo si está pendiente).
     */
    @DELETE("api/pedidos/{id}")
    suspend fun cancelarPedido(
        @Path("id") idPedido: Long
    ): Response<Unit>

    // ============================================
    // PAGOS (Microservicio Pedido)
    // ============================================

    /**
     * Crear preferencia de pago en Mercado Pago.
     * Retorna URL para redirigir al usuario a pagar.
     */
    @POST("api/pagos/crear-preferencia")
    suspend fun crearPreferenciaPago(
        @Body request: CrearPagoRequest
    ): Response<PagoResponse>

    /**
     * Obtener estado de un pago.
     */
    @GET("api/pagos/pedido/{idPedido}")
    suspend fun getPagosPorPedido(
        @Path("idPedido") idPedido: Long
    ): Response<List<PagoResponse>>

    // ============================================
    // TIPOS Y MÉTODOS (Para selects en UI)
    // ============================================

    /**
     * Obtener tipos de entrega disponibles.
     * Ejemplo: Delivery, Retiro en local
     */
    @GET("api/pedidos/tipos-entrega")
    suspend fun getTiposEntrega(): Response<List<TipoEntregaResponse>>

    /**
     * Obtener métodos de pago disponibles.
     * Ejemplo: Mercado Pago, Efectivo
     */
    @GET("api/pedidos/metodos-pago")
    suspend fun getMetodosPago(): Response<List<MetodoPagoResponse>>
}
```

---

## PASO 4: CONFIGURAR FIREBASE EN LA APP

### Archivo: `GoldenBurgersApplication.kt` (NUEVO)

Crear clase Application para inicializar Firebase:

```kotlin
package com.example.goldenburgers

import android.app.Application
import com.google.firebase.FirebaseApp

/**
 * Clase Application personalizada.
 * Se ejecuta antes que cualquier Activity.
 *
 * Inicializa Firebase y otras librerías globales.
 */
class GoldenBurgersApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Inicializar Firebase
        FirebaseApp.initializeApp(this)
    }
}
```

### Actualizar: `AndroidManifest.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <!-- Permisos existentes -->
    <uses-feature android:name="android.hardware.camera" android:required="false" />
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

    <!-- NUEVO: Permiso de Internet para llamadas HTTP -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <application
        android:name=".GoldenBurgersApplication"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.GoldenBurgers"
        android:usesCleartextTraffic="true"
        tools:targetApi="31">

        <!-- usesCleartextTraffic="true" permite HTTP sin SSL (desarrollo) -->
        <!-- IMPORTANTE: En producción usar HTTPS y quitar esta línea -->

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.GoldenBurgers">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- FileProvider existente para cámara -->
        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.fileprovider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths" />
        </provider>

    </application>
</manifest>
```

### Actualizar: `model/SessionManager.kt`

Agregar almacenamiento del JWT:

```kotlin
package com.example.goldenburgers.model

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// DataStore para sesión
private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "session_prefs"
)

/**
 * Gestiona la sesión del usuario.
 *
 * Almacena:
 * - Email del usuario (existente)
 * - Firebase UID (NUEVO)
 * - JWT Token (NUEVO)
 * - ID del cliente en backend (NUEVO)
 */
class SessionManager(private val context: Context) {

    companion object {
        // Claves existentes
        private val LOGGED_IN_USER_EMAIL = stringPreferencesKey("logged_in_user_email")

        // NUEVAS claves
        private val FIREBASE_UID = stringPreferencesKey("firebase_uid")
        private val JWT_TOKEN = stringPreferencesKey("jwt_token")
        private val CLIENT_ID = longPreferencesKey("client_id")
        private val CLIENT_NAME = stringPreferencesKey("client_name")
    }

    // ============================================
    // FLOWS (Para observar cambios reactivamente)
    // ============================================

    val loggedInUserEmailFlow: Flow<String?> = context.sessionDataStore.data
        .map { preferences -> preferences[LOGGED_IN_USER_EMAIL] }

    val firebaseUidFlow: Flow<String?> = context.sessionDataStore.data
        .map { preferences -> preferences[FIREBASE_UID] }

    val jwtTokenFlow: Flow<String?> = context.sessionDataStore.data
        .map { preferences -> preferences[JWT_TOKEN] }

    val clientIdFlow: Flow<Long?> = context.sessionDataStore.data
        .map { preferences -> preferences[CLIENT_ID] }

    val clientNameFlow: Flow<String?> = context.sessionDataStore.data
        .map { preferences -> preferences[CLIENT_NAME] }

    // ============================================
    // MÉTODOS DE ESCRITURA
    // ============================================

    /**
     * Guarda la sesión completa después del login exitoso.
     */
    suspend fun saveSession(
        email: String,
        firebaseUid: String,
        jwtToken: String,
        clientId: Long,
        clientName: String
    ) {
        context.sessionDataStore.edit { preferences ->
            preferences[LOGGED_IN_USER_EMAIL] = email
            preferences[FIREBASE_UID] = firebaseUid
            preferences[JWT_TOKEN] = jwtToken
            preferences[CLIENT_ID] = clientId
            preferences[CLIENT_NAME] = clientName
        }
    }

    /**
     * Actualiza solo el JWT (para refresh token).
     */
    suspend fun updateJwtToken(newToken: String) {
        context.sessionDataStore.edit { preferences ->
            preferences[JWT_TOKEN] = newToken
        }
    }

    /**
     * Guarda sesión básica (compatibilidad con código existente).
     */
    suspend fun saveUserSession(email: String) {
        context.sessionDataStore.edit { preferences ->
            preferences[LOGGED_IN_USER_EMAIL] = email
        }
    }

    /**
     * Limpia toda la sesión (logout).
     */
    suspend fun clearUserSession() {
        context.sessionDataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
```

---

## PASO 5: CREAR DTOs PARA BACKEND

### Estructura de carpetas para DTOs:

```
data/remote/dto/
├── request/
│   ├── LoginRequest.kt
│   ├── RegistrarClienteRequest.kt
│   ├── ActualizarPerfilRequest.kt
│   ├── CrearDireccionRequest.kt
│   ├── ActualizarDireccionRequest.kt
│   ├── CrearPedidoRequest.kt
│   └── CrearPagoRequest.kt
└── response/
    ├── AuthResponse.kt
    ├── ClienteResponse.kt
    ├── DireccionResponse.kt
    ├── CiudadResponse.kt
    ├── ProductoResponse.kt
    ├── CategoriaResponse.kt
    ├── PedidoResponse.kt
    ├── DetallePedidoResponse.kt
    ├── PagoResponse.kt
    ├── TipoEntregaResponse.kt
    └── MetodoPagoResponse.kt
```

### Archivo: `dto/request/LoginRequest.kt`

```kotlin
package com.example.goldenburgers.data.remote.dto.request

import com.google.gson.annotations.SerializedName

/**
 * Request para login en el API Gateway.
 * Envía el token de Firebase para obtener JWT interno.
 */
data class LoginRequest(
    @SerializedName("firebaseToken")
    val firebaseToken: String
)
```

### Archivo: `dto/request/RegistrarClienteRequest.kt`

```kotlin
package com.example.goldenburgers.data.remote.dto.request

import com.google.gson.annotations.SerializedName

/**
 * Request para registrar nuevo cliente.
 * Se llama después de crear cuenta en Firebase Auth.
 *
 * IMPORTANTE:
 * - idUsuario es el Firebase UID
 * - telefonoCliente debe ser 9 dígitos (opcional)
 */
data class RegistrarClienteRequest(
    @SerializedName("idUsuario")
    val idUsuario: String,  // Firebase UID

    @SerializedName("email")
    val email: String,

    @SerializedName("nombreCliente")
    val nombreCliente: String,

    @SerializedName("telefonoCliente")
    val telefonoCliente: String? = null  // Opcional, 9 dígitos
)
```

### Archivo: `dto/request/ActualizarPerfilRequest.kt`

```kotlin
package com.example.goldenburgers.data.remote.dto.request

import com.google.gson.annotations.SerializedName

/**
 * Request para actualizar perfil del cliente.
 */
data class ActualizarPerfilRequest(
    @SerializedName("nombreCliente")
    val nombreCliente: String,

    @SerializedName("email")
    val email: String? = null,  // Solo si cambia

    @SerializedName("telefonoCliente")
    val telefonoCliente: String? = null
)
```

### Archivo: `dto/request/CrearDireccionRequest.kt`

```kotlin
package com.example.goldenburgers.data.remote.dto.request

import com.google.gson.annotations.SerializedName

/**
 * Request para crear nueva dirección.
 *
 * IMPORTANTE: El campo 'direccion' es texto concatenado.
 * Concatenar así: "$calle $numero, $comuna, $ciudad, $region"
 */
data class CrearDireccionRequest(
    @SerializedName("idCliente")
    val idCliente: Long,

    @SerializedName("idCiudad")
    val idCiudad: Long,  // Obtener de GET /api/ciudades

    @SerializedName("direccion")
    val direccion: String,  // Texto concatenado

    @SerializedName("alias")
    val alias: String? = null  // "Casa", "Trabajo", etc.
)
```

### Archivo: `dto/request/ActualizarDireccionRequest.kt`

```kotlin
package com.example.goldenburgers.data.remote.dto.request

import com.google.gson.annotations.SerializedName

/**
 * Request para actualizar dirección existente.
 */
data class ActualizarDireccionRequest(
    @SerializedName("idCiudad")
    val idCiudad: Long,

    @SerializedName("direccion")
    val direccion: String,

    @SerializedName("alias")
    val alias: String? = null
)
```

### Archivo: `dto/request/CrearPedidoRequest.kt`

```kotlin
package com.example.goldenburgers.data.remote.dto.request

import com.google.gson.annotations.SerializedName

/**
 * Request para crear pedido completo con detalles.
 * Este es el request principal del checkout.
 */
data class CrearPedidoRequest(
    @SerializedName("idCliente")
    val idCliente: Long,

    @SerializedName("idEstadoPedido")
    val idEstadoPedido: Long = 1,  // 1 = Pendiente de Pago

    @SerializedName("idMetodoPago")
    val idMetodoPago: Long,  // 1 = Mercado Pago, etc.

    @SerializedName("idTipoEntrega")
    val idTipoEntrega: Long,  // 1 = Delivery, 2 = Retiro

    @SerializedName("idDireccionEntrega")
    val idDireccionEntrega: Long? = null,  // Requerido si delivery

    @SerializedName("montoSubtotal")
    val montoSubtotal: Double,

    @SerializedName("montoEnvio")
    val montoEnvio: Double = 0.0,

    @SerializedName("montoTotal")
    val montoTotal: Double,

    @SerializedName("notasCliente")
    val notasCliente: String? = null,  // "Sin cebolla", etc.

    @SerializedName("detalles")
    val detalles: List<DetallePedidoRequest>
)

/**
 * Detalle de cada producto en el pedido.
 */
data class DetallePedidoRequest(
    @SerializedName("idProducto")
    val idProducto: Long,

    @SerializedName("cantidad")
    val cantidad: Int,

    @SerializedName("precioUnitario")
    val precioUnitario: Double,

    @SerializedName("subtotalLinea")
    val subtotalLinea: Double  // cantidad * precioUnitario
)
```

### Archivo: `dto/request/CrearPagoRequest.kt`

```kotlin
package com.example.goldenburgers.data.remote.dto.request

import com.google.gson.annotations.SerializedName

/**
 * Request para crear preferencia de pago en Mercado Pago.
 */
data class CrearPagoRequest(
    @SerializedName("idPedido")
    val idPedido: Long,

    @SerializedName("montoPago")
    val montoPago: Double,

    @SerializedName("descripcion")
    val descripcion: String,  // "Pago pedido #123 - Golden Burgers"

    @SerializedName("email")
    val email: String  // Email del cliente
)
```

### Archivo: `dto/response/AuthResponse.kt`

```kotlin
package com.example.goldenburgers.data.remote.dto.response

import com.google.gson.annotations.SerializedName

/**
 * Response del login.
 * Contiene el JWT interno y datos básicos del usuario.
 */
data class AuthResponse(
    @SerializedName("token")
    val token: String,

    @SerializedName("tokenType")
    val tokenType: String = "Bearer",

    @SerializedName("expiresIn")
    val expiresIn: Long? = null,  // Segundos hasta expiración

    @SerializedName("user")
    val user: UserBasicResponse? = null
)

data class UserBasicResponse(
    @SerializedName("idUsuario")
    val idUsuario: String,

    @SerializedName("email")
    val email: String,

    @SerializedName("rol")
    val rol: String
)
```

### Archivo: `dto/response/ClienteResponse.kt`

```kotlin
package com.example.goldenburgers.data.remote.dto.response

import com.google.gson.annotations.SerializedName

/**
 * Response con datos completos del cliente.
 */
data class ClienteResponse(
    @SerializedName("idCliente")
    val idCliente: Long,

    @SerializedName("usuario")
    val usuario: UsuarioResponse,

    @SerializedName("nombreCliente")
    val nombreCliente: String,

    @SerializedName("telefonoCliente")
    val telefonoCliente: String?,

    @SerializedName("direcciones")
    val direcciones: List<DireccionResponse>?
)

data class UsuarioResponse(
    @SerializedName("idUsuario")
    val idUsuario: String,  // Firebase UID

    @SerializedName("email")
    val email: String,

    @SerializedName("rol")
    val rol: RolResponse,

    @SerializedName("fechaCreacion")
    val fechaCreacion: String?
)

data class RolResponse(
    @SerializedName("idRol")
    val idRol: Long,

    @SerializedName("nombreRol")
    val nombreRol: String
)
```

### Archivo: `dto/response/DireccionResponse.kt`

```kotlin
package com.example.goldenburgers.data.remote.dto.response

import com.google.gson.annotations.SerializedName

/**
 * Response de dirección.
 */
data class DireccionResponse(
    @SerializedName("idDireccion")
    val idDireccion: Long,

    @SerializedName("idCliente")
    val idCliente: Long,

    @SerializedName("ciudad")
    val ciudad: CiudadResponse,

    @SerializedName("direccion")
    val direccion: String,  // Texto concatenado

    @SerializedName("alias")
    val alias: String?
)
```

### Archivo: `dto/response/CiudadResponse.kt`

```kotlin
package com.example.goldenburgers.data.remote.dto.response

import com.google.gson.annotations.SerializedName

/**
 * Response de ciudad.
 */
data class CiudadResponse(
    @SerializedName("idCiudad")
    val idCiudad: Long,

    @SerializedName("nombreCiudad")
    val nombreCiudad: String
)
```

### Archivo: `dto/response/ProductoResponse.kt`

```kotlin
package com.example.goldenburgers.data.remote.dto.response

import com.google.gson.annotations.SerializedName

/**
 * Response de producto del catálogo.
 *
 * NOTA: 'imagen' es una URL de Firebase Storage, no un drawable.
 */
data class ProductoResponse(
    @SerializedName("id")
    val id: Long,

    @SerializedName("nombre")
    val nombre: String,

    @SerializedName("descripcion")
    val descripcion: String?,

    @SerializedName("precio")
    val precio: Double,

    @SerializedName("idCategoria")
    val idCategoria: Long,

    @SerializedName("categoria")
    val categoria: String,

    @SerializedName("imagen")
    val imagen: String?,  // URL de Firebase Storage

    @SerializedName("disponible")
    val disponible: Boolean
)
```

### Archivo: `dto/response/CategoriaResponse.kt`

```kotlin
package com.example.goldenburgers.data.remote.dto.response

import com.google.gson.annotations.SerializedName

/**
 * Response de categoría de productos.
 */
data class CategoriaResponse(
    @SerializedName("id")
    val id: Long,

    @SerializedName("nombre")
    val nombre: String,

    @SerializedName("descripcion")
    val descripcion: String?
)
```

### Archivo: `dto/response/PedidoResponse.kt`

```kotlin
package com.example.goldenburgers.data.remote.dto.response

import com.google.gson.annotations.SerializedName

/**
 * Response de pedido.
 */
data class PedidoResponse(
    @SerializedName("idPedido")
    val idPedido: Long,

    @SerializedName("idCliente")
    val idCliente: Long,

    @SerializedName("estadoPedido")
    val estadoPedido: EstadoPedidoResponse?,

    @SerializedName("metodoPago")
    val metodoPago: MetodoPagoResponse?,

    @SerializedName("tipoEntrega")
    val tipoEntrega: TipoEntregaResponse?,

    @SerializedName("direccionEntrega")
    val direccionEntrega: DireccionResponse?,

    @SerializedName("montoSubtotal")
    val montoSubtotal: Double,

    @SerializedName("montoEnvio")
    val montoEnvio: Double,

    @SerializedName("montoTotal")
    val montoTotal: Double,

    @SerializedName("fechaPedido")
    val fechaPedido: String,

    @SerializedName("notaCliente")
    val notaCliente: String?,

    @SerializedName("detalles")
    val detalles: List<DetallePedidoResponse>?
)

data class EstadoPedidoResponse(
    @SerializedName("idEstadoPedido")
    val idEstadoPedido: Long,

    @SerializedName("nombreEstado")
    val nombreEstado: String
)
// Estados: 1=Pendiente, 2=Pagado, 3=En preparación, 4=En camino, 5=Entregado, 6=Cancelado
```

### Archivo: `dto/response/DetallePedidoResponse.kt`

```kotlin
package com.example.goldenburgers.data.remote.dto.response

import com.google.gson.annotations.SerializedName

/**
 * Response de detalle de pedido (línea de producto).
 */
data class DetallePedidoResponse(
    @SerializedName("idDetalle")
    val idDetalle: Long,

    @SerializedName("idPedido")
    val idPedido: Long,

    @SerializedName("idProducto")
    val idProducto: Long,

    @SerializedName("nombreProducto")
    val nombreProducto: String?,

    @SerializedName("cantidad")
    val cantidad: Int,

    @SerializedName("precioUnitario")
    val precioUnitario: Double,

    @SerializedName("subtotalLinea")
    val subtotalLinea: Double
)
```

### Archivo: `dto/response/PagoResponse.kt`

```kotlin
package com.example.goldenburgers.data.remote.dto.response

import com.google.gson.annotations.SerializedName

/**
 * Response de pago con URL de Mercado Pago.
 */
data class PagoResponse(
    @SerializedName("idPago")
    val idPago: Long,

    @SerializedName("idPedido")
    val idPedido: Long,

    @SerializedName("montoPago")
    val montoPago: Double,

    @SerializedName("estadoPago")
    val estadoPago: String,  // PENDIENTE, PAGADO, RECHAZADO, CANCELADO

    @SerializedName("metodoPago")
    val metodoPago: String?,

    @SerializedName("idPreferenciaMpos")
    val idPreferenciaMpos: String?,

    @SerializedName("urlPago")
    val urlPago: String?,  // URL de Mercado Pago para pagar

    @SerializedName("fechaPago")
    val fechaPago: String?,

    @SerializedName("fechaCreacion")
    val fechaCreacion: String?
)
```

### Archivo: `dto/response/TipoEntregaResponse.kt`

```kotlin
package com.example.goldenburgers.data.remote.dto.response

import com.google.gson.annotations.SerializedName

/**
 * Response de tipo de entrega.
 */
data class TipoEntregaResponse(
    @SerializedName("idTipoEntrega")
    val idTipoEntrega: Long,

    @SerializedName("nombreTipoEntrega")
    val nombreTipoEntrega: String  // "Delivery", "Retiro en local"
)
```

### Archivo: `dto/response/MetodoPagoResponse.kt`

```kotlin
package com.example.goldenburgers.data.remote.dto.response

import com.google.gson.annotations.SerializedName

/**
 * Response de método de pago.
 */
data class MetodoPagoResponse(
    @SerializedName("idMetodoPago")
    val idMetodoPago: Long,

    @SerializedName("nombreMetodoPago")
    val nombreMetodoPago: String  // "Mercado Pago", "Efectivo"
)
```

---

## PASO 6: ADAPTAR MODELO USER LOCAL

### Archivo: `model/UserLocal.kt` (NUEVO)

Este modelo almacena datos que NO van al backend:

```kotlin
package com.example.goldenburgers.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad para datos locales del usuario.
 *
 * Almacena información que NO se envía al backend:
 * - Foto de perfil (URI local)
 * - Género
 * - Fecha de nacimiento
 *
 * La clave primaria es el Firebase UID para vincular
 * con la sesión del usuario.
 */
@Entity(
    tableName = "user_local",
    indices = [Index(value = ["firebase_uid"], unique = true)]
)
data class UserLocal(
    @PrimaryKey
    @ColumnInfo(name = "firebase_uid")
    val firebaseUid: String,

    @ColumnInfo(name = "email")
    val email: String,

    @ColumnInfo(name = "profile_image_uri")
    val profileImageUri: String? = null,

    @ColumnInfo(name = "gender")
    val gender: String? = null,

    @ColumnInfo(name = "birth_date")
    val birthDate: String? = null,  // Formato: YYYY-MM-DD

    // Campos de dirección para el formulario (se concatenan al enviar)
    @ColumnInfo(name = "street")
    val street: String? = null,

    @ColumnInfo(name = "number")
    val number: String? = null,

    @ColumnInfo(name = "commune")
    val commune: String? = null,

    @ColumnInfo(name = "city")
    val city: String? = null,

    @ColumnInfo(name = "region")
    val region: String? = null
) {
    /**
     * Concatena los campos de dirección en el formato del backend.
     * Ejemplo: "Av. Principal 123, Providencia, Santiago, Metropolitana"
     */
    fun getConcatenatedAddress(): String {
        val parts = listOfNotNull(
            if (!street.isNullOrBlank() && !number.isNullOrBlank()) "$street $number" else street,
            commune,
            city,
            region
        ).filter { it.isNotBlank() }

        return parts.joinToString(", ")
    }
}
```

### Archivo: `model/UserLocalDao.kt` (NUEVO)

```kotlin
package com.example.goldenburgers.model

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operaciones con datos locales del usuario.
 */
@Dao
interface UserLocalDao {

    /**
     * Obtiene los datos locales por Firebase UID.
     */
    @Query("SELECT * FROM user_local WHERE firebase_uid = :firebaseUid")
    suspend fun getByFirebaseUid(firebaseUid: String): UserLocal?

    /**
     * Obtiene los datos locales como Flow (reactivo).
     */
    @Query("SELECT * FROM user_local WHERE firebase_uid = :firebaseUid")
    fun getByFirebaseUidFlow(firebaseUid: String): Flow<UserLocal?>

    /**
     * Inserta o reemplaza datos locales.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(userLocal: UserLocal)

    /**
     * Actualiza solo la foto de perfil.
     */
    @Query("UPDATE user_local SET profile_image_uri = :uri WHERE firebase_uid = :firebaseUid")
    suspend fun updateProfileImage(firebaseUid: String, uri: String?)

    /**
     * Actualiza campos de dirección.
     */
    @Query("""
        UPDATE user_local SET
            street = :street,
            number = :number,
            commune = :commune,
            city = :city,
            region = :region
        WHERE firebase_uid = :firebaseUid
    """)
    suspend fun updateAddress(
        firebaseUid: String,
        street: String?,
        number: String?,
        commune: String?,
        city: String?,
        region: String?
    )

    /**
     * Elimina datos locales de un usuario.
     */
    @Query("DELETE FROM user_local WHERE firebase_uid = :firebaseUid")
    suspend fun delete(firebaseUid: String)
}
```

---

## PASO 7: CREAR MODELO DE FAVORITOS LOCALES

### Archivo: `model/FavoriteProduct.kt` (NUEVO)

```kotlin
package com.example.goldenburgers.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

/**
 * Entidad para productos favoritos.
 *
 * Los favoritos se guardan localmente por usuario.
 * No se sincronizan con el backend.
 *
 * Clave primaria compuesta: (productId, userFirebaseUid)
 * Esto permite que cada usuario tenga sus propios favoritos.
 */
@Entity(
    tableName = "favorite_products",
    primaryKeys = ["product_id", "user_firebase_uid"],
    indices = [
        Index(value = ["user_firebase_uid"]),
        Index(value = ["product_id"])
    ]
)
data class FavoriteProduct(
    @ColumnInfo(name = "product_id")
    val productId: Long,

    @ColumnInfo(name = "user_firebase_uid")
    val userFirebaseUid: String,

    @ColumnInfo(name = "added_at")
    val addedAt: Long = System.currentTimeMillis()
)
```

### Archivo: `model/FavoriteProductDao.kt` (NUEVO)

```kotlin
package com.example.goldenburgers.model

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operaciones con favoritos.
 */
@Dao
interface FavoriteProductDao {

    /**
     * Obtiene todos los IDs de productos favoritos de un usuario.
     */
    @Query("SELECT product_id FROM favorite_products WHERE user_firebase_uid = :firebaseUid")
    fun getFavoriteProductIds(firebaseUid: String): Flow<List<Long>>

    /**
     * Verifica si un producto es favorito del usuario.
     */
    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM favorite_products
            WHERE product_id = :productId AND user_firebase_uid = :firebaseUid
        )
    """)
    suspend fun isFavorite(productId: Long, firebaseUid: String): Boolean

    /**
     * Agrega producto a favoritos.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteProduct)

    /**
     * Elimina producto de favoritos.
     */
    @Query("""
        DELETE FROM favorite_products
        WHERE product_id = :productId AND user_firebase_uid = :firebaseUid
    """)
    suspend fun removeFavorite(productId: Long, firebaseUid: String)

    /**
     * Toggle favorito (agrega si no existe, elimina si existe).
     */
    @Transaction
    suspend fun toggleFavorite(productId: Long, firebaseUid: String): Boolean {
        val isFav = isFavorite(productId, firebaseUid)
        if (isFav) {
            removeFavorite(productId, firebaseUid)
        } else {
            addFavorite(FavoriteProduct(productId, firebaseUid))
        }
        return !isFav  // Retorna el nuevo estado
    }

    /**
     * Elimina todos los favoritos de un usuario.
     */
    @Query("DELETE FROM favorite_products WHERE user_firebase_uid = :firebaseUid")
    suspend fun clearFavorites(firebaseUid: String)

    /**
     * Cuenta favoritos de un usuario.
     */
    @Query("SELECT COUNT(*) FROM favorite_products WHERE user_firebase_uid = :firebaseUid")
    suspend fun countFavorites(firebaseUid: String): Int
}
```

### Actualizar: `model/GoldenBurgersDatabase.kt`

Agregar las nuevas entidades y DAOs:

```kotlin
package com.example.goldenburgers.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Base de datos Room.
 *
 * IMPORTANTE: Incrementar version al agregar entidades.
 * Version 3 → 4: Agregamos UserLocal y FavoriteProduct
 */
@Database(
    entities = [
        Producto::class,
        User::class,           // Mantener por compatibilidad (migración gradual)
        UserLocal::class,      // NUEVO
        FavoriteProduct::class // NUEVO
    ],
    version = 4,
    exportSchema = false
)
abstract class GoldenBurgersDatabase : RoomDatabase() {

    // DAOs existentes
    abstract fun productoDao(): ProductoDAO
    abstract fun userDao(): UserDao

    // NUEVOS DAOs
    abstract fun userLocalDao(): UserLocalDao
    abstract fun favoriteProductDao(): FavoriteProductDao

    companion object {
        @Volatile
        private var INSTANCE: GoldenBurgersDatabase? = null

        // Migración de versión 3 a 4
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Crear tabla user_local
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS user_local (
                        firebase_uid TEXT NOT NULL PRIMARY KEY,
                        email TEXT NOT NULL,
                        profile_image_uri TEXT,
                        gender TEXT,
                        birth_date TEXT,
                        street TEXT,
                        number TEXT,
                        commune TEXT,
                        city TEXT,
                        region TEXT
                    )
                """)

                // Crear índice para user_local
                database.execSQL("""
                    CREATE UNIQUE INDEX IF NOT EXISTS index_user_local_firebase_uid
                    ON user_local (firebase_uid)
                """)

                // Crear tabla favorite_products
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS favorite_products (
                        product_id INTEGER NOT NULL,
                        user_firebase_uid TEXT NOT NULL,
                        added_at INTEGER NOT NULL,
                        PRIMARY KEY (product_id, user_firebase_uid)
                    )
                """)

                // Crear índices para favorite_products
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_favorite_products_user_firebase_uid
                    ON favorite_products (user_firebase_uid)
                """)
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_favorite_products_product_id
                    ON favorite_products (product_id)
                """)
            }
        }

        fun getDatabase(context: Context): GoldenBurgersDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GoldenBurgersDatabase::class.java,
                    "golden_burgers_database"
                )
                    .addMigrations(MIGRATION_3_4)
                    // Si prefieres borrar y recrear (desarrollo):
                    // .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(context))
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }

    /**
     * Callback para inicializar datos al crear la BD.
     */
    private class DatabaseCallback(
        private val context: Context
    ) : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // Aquí se pueden insertar datos iniciales si es necesario
            // Los productos ahora vienen del backend
        }
    }
}
```

---

## RESUMEN PARTE 1

### Archivos creados/modificados:

| Archivo                         | Acción   | Descripción                            |
| ------------------------------- | --------- | --------------------------------------- |
| `build.gradle.kts` (app)      | Modificar | Agregar dependencias Firebase, Retrofit |
| `build.gradle.kts` (proyecto) | Modificar | Agregar plugin Google Services          |
| `google-services.json`        | Crear     | Descargar de Firebase Console           |
| `AndroidManifest.xml`         | Modificar | Agregar permisos Internet               |
| `GoldenBurgersApplication.kt` | Crear     | Inicializar Firebase                    |
| `AuthInterceptor.kt`          | Crear     | Agregar JWT a requests                  |
| `ApiClient.kt`                | Crear     | Configurar Retrofit                     |
| `ApiService.kt`               | Crear     | Definir endpoints                       |
| `SessionManager.kt`           | Modificar | Agregar JWT y Firebase UID              |
| `dto/request/*.kt`            | Crear     | 7 archivos de requests                  |
| `dto/response/*.kt`           | Crear     | 12 archivos de responses                |
| `UserLocal.kt`                | Crear     | Datos locales del usuario               |
| `UserLocalDao.kt`             | Crear     | DAO para UserLocal                      |
| `FavoriteProduct.kt`          | Crear     | Modelo de favoritos                     |
| `FavoriteProductDao.kt`       | Crear     | DAO para favoritos                      |
| `GoldenBurgersDatabase.kt`    | Modificar | Agregar nuevas entidades                |

### Siguiente parte:

Continúa en **INTEGRACION-PARTE2-AUTENTICACION.md** para implementar el flujo de autenticación con Firebase.

---

**Continúa en:** [PARTE 2 - Autenticación y Catálogo](./INTEGRACION-PARTE2-AUTENTICACION.md)
