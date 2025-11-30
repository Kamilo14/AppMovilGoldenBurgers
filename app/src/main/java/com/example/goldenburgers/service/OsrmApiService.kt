package com.example.goldenburgers.service

import com.example.goldenburgers.model.dto.routing.OsrmResponse
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Interfaz de Retrofit para interactuar con la API de OSRM (Open Source Routing Machine).
 */
interface OsrmApiService {

    /**
     * Calcula la ruta más rápida entre dos coordenadas.
     * @param profile El modo de viaje (siempre "car" para nosotros).
     * @param coordinates Las coordenadas de origen y destino en formato "lon,lat;lon,lat".
     * @return Un objeto OsrmResponse con los detalles de la ruta.
     */
    @GET("route/v1/{profile}/{coordinates}")
    suspend fun getRoute(
        @Path("profile") profile: String = "car",
        @Path("coordinates") coordinates: String
    ): OsrmResponse
}