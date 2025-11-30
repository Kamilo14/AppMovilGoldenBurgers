package com.example.goldenburgers.service

import com.example.goldenburgers.model.dto.geocoding.NominatimPlaceDTO
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Interfaz de Retrofit para interactuar con la API de Nominatim (OpenStreetMap).
 */
interface NominatimApiService {

    /**
     * Convierte una dirección de texto en coordenadas geográficas.
     * @param query La dirección a buscar (ej: "Etchevers 210, Viña del Mar, Chile").
     * @param format El formato de respuesta (siempre "jsonv2").
     * @param limit El número máximo de resultados (queremos solo el mejor, así que 1).
     * @return Una lista que contiene el resultado de la geocodificación.
     */
    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("format") format: String = "jsonv2",
        @Query("limit") limit: Int = 1
    ): List<NominatimPlaceDTO>
}