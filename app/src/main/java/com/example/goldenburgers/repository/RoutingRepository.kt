package com.example.goldenburgers.repository

import com.example.goldenburgers.service.ExternalApiClient
import com.example.goldenburgers.service.NominatimApiService
import com.example.goldenburgers.service.OsrmApiService

class RoutingRepository {

    // URLs base para las APIs externas
    private val nominatimApi = ExternalApiClient.createService(NominatimApiService::class.java, "https://nominatim.openstreetmap.org/")
    // [CORREGIDO] Se usa https para cumplir con la política de seguridad de Android
    private val osrmApi = ExternalApiClient.createService(OsrmApiService::class.java, "https://router.project-osrm.org/")

    // Dirección fija del restaurante
    private val restaurantAddress = "Etchevers 210, Viña del Mar, Chile"

    // Tiempo de preparación fijo en minutos
    private val preparationTimeMinutes = 15

    /**
     * Calcula el tiempo de entrega estimado, combinando llamadas a Nominatim y OSRM.
     * @param clientAddress La dirección del cliente.
     * @return El tiempo estimado de entrega en minutos, o null si ocurre un error.
     */
    suspend fun getEstimatedDeliveryTime(clientAddress: String): Int? {
        return try {
            // 1. Obtener coordenadas del restaurante
            val restaurantCoords = nominatimApi.search(restaurantAddress).firstOrNull() ?: return null

            // 2. Obtener coordenadas del cliente
            val clientCoords = nominatimApi.search("$clientAddress, Chile").firstOrNull() ?: return null

            // 3. Formatear las coordenadas para OSRM: "lon,lat;lon,lat"
            val coordinatesString = "${restaurantCoords.longitude},${restaurantCoords.latitude};${clientCoords.longitude},${clientCoords.latitude}"

            // 4. Obtener la duración de la ruta desde OSRM
            val routeResponse = osrmApi.getRoute(coordinates = coordinatesString)
            val travelTimeSeconds = routeResponse.routes.firstOrNull()?.duration ?: return null
            
            // 5. Calcular y devolver el tiempo total en minutos
            val travelTimeMinutes = (travelTimeSeconds / 60).toInt()
            
            preparationTimeMinutes + travelTimeMinutes
        } catch (e: Exception) {
            // Si algo falla (red, parsing, etc.), devolvemos null
            null
        }
    }
}