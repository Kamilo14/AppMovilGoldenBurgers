package com.example.goldenburgers.model.dto.geocoding

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object para parsear la respuesta de la API de Nominatim.
 * Contiene las coordenadas de un lugar.
 */
data class NominatimPlaceDTO(
    @SerializedName("lat") val latitude: String,
    @SerializedName("lon") val longitude: String
)
