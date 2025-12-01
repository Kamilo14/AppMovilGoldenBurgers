# GoldenBurgers

**GoldenBurgers** es una aplicación móvil desarrollada para la compra y gestión de pedidos de hamburguesas. Permite a los usuarios navegar por un catálogo de productos, gestionar su carrito de compras, realizar pedidos, y administrar su perfil y direcciones.

## Integrantes
*   Fabian Basaes
*   Camilo Calderon
*   Luckas Trujillo

## Funcionalidades
*   **Autenticación:** Registro e inicio de sesión de usuarios (integración con Firebase).
*   **Catálogo de Productos:** Visualización de productos por categorías, búsqueda y detalles de producto.
*   **Carrito de Compras:** Gestión de productos seleccionados, cantidades y cálculo de totales.
*   **Estimación de Demora:** Cálculo dinámico del tiempo de entrega estimado utilizando APIs de enrutamiento.
*   **Pagos:** Simulación de procesamiento de pagos con tarjeta de crédito/débito.
*   **Gestión de Pedidos:** Realización de pedidos, selección de métodos de pago y seguimiento de estado.
  * **Finalización de Pedido:** Formulario obligatorio de datos del cliente y dirección de envío previo al pago.
*   **Perfil de Usuario:** Administración de información personal y direcciones de envío.
*   **Favoritos:** Lista de productos favoritos del usuario.
*   **Ubicación:** Integración con servicios de mapas para la gestión de direcciones.

## Endpoints Usados

### Propios (API Backend)
La aplicación se comunica con un backend propio. Los siguientes endpoints son los utilizados para las funcionalidades principales actuales:

*   **Autenticación y Usuarios:**
    *   `POST auth/login`: Inicio de sesión de usuario.
    *   `POST auth/refresh`: Refrescar token de sesión.
    *   `POST clientes`: Registrar un nuevo cliente.
    *   `GET clientes/usuario/{firebaseUid}`: Obtener información del cliente vinculada a Firebase.
    *   `GET clientes/{id}`: Obtener detalles del perfil.
    *   `PUT clientes/perfil`: Actualizar información personal.
    *   `POST clientes/direcciones`: Crear nueva dirección de envío.
    *   `GET clientes/{idCliente}/direcciones`: Listar direcciones de envío registradas.
    *   `PUT clientes/direcciones/{id}`: Actualizar una dirección.
    *   `DELETE clientes/direcciones/{id}`: Eliminar una dirección.
*   **Catálogo:**
    *   `GET catalogo/productos`: Listar el inventario completo.
    *   `GET catalogo/productos/{id}`: Obtener detalle de un producto.
    *   `GET catalogo/categorias`: Listar categorías disponibles.
    *   `GET catalogo/productos/categoria/{idCategoria}`: Filtrar productos por categoría.
*   **Pedidos:**
    *   `GET pedidos`: Listado general de pedidos.
    *   `GET pedidos/{id}`: Detalle de un pedido específico.
    *   `GET pedidos/cliente/{idCliente}`: Historial de pedidos del usuario.
    *   `POST pedidos/completo`: Crear un nuevo pedido con todos sus detalles.
    *   `PUT pedidos/cambiar-estado/{idPedido}/estado/{idEstado}`: Actualizar el estado de un pedido.
*   **Ubicación:**
    *   `GET ciudades`: Listado de ciudades con cobertura.

### Externos
*   **Nominatim (OpenStreetMap):**
    *   `GET search`: Búsqueda de direcciones y geocodificación.
*   **OSRM (Open Source Routing Machine):**
    *   `GET route/v1/{profile}/{coordinates}`: Cálculo de rutas y estimación de entregas en vehiculos.

## Instrucciones para Ejecutar el Proyecto

1.  **Requisitos Previos:**
    *   Android Studio (versión recomendada: Koala o superior).
    *   JDK 17 o superior configurado en el IDE.
    *   Dispositivo Android o Emulador con API Level 26 (Android 8.0) o superior.

2.  **Configuración:**
    *   Clonar el repositorio desde GitHub.
    *   Abrir el proyecto en Android Studio.
    *   Sincronizar el proyecto con Gradle (`File > Sync Project with Gradle Files`).
    *   Asegurarse de tener el archivo `google-services.json` en la carpeta `app/` para la integración con Firebase.

3.  **Ejecución:**
    *   Seleccionar el dispositivo de destino en la barra de herramientas.
    *   Hacer clic en el botón "Run" (icono de reproducción verde) o presionar `Shift + F10`.

## APK Firmado y Ubicación del Archivo .jks
*   **APK Firmado:** `app/release/app-release.apk`.
*   **Archivo Keystore (.jks):** `keystore/goldenburgers.jks` 

## Código Fuente
El código fuente de la aplicación móvil se encuentra estructurado en la carpeta `app/src/main/java/com/example/goldenburgers`.
*   **View:** Componentes de UI (Jetpack Compose).
*   **ViewModel:** Lógica de presentación y estado.
*   **Model:** Modelos de datos y repositorios.
*   **Service:** Definición de interfaces para las APIs (Retrofit).

## Evidencia de Trabajo Colaborativo
El historial de commits en este repositorio sirve como evidencia del trabajo colaborativo realizado por los integrantes del equipo.
