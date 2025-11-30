package com.example.goldenburgers.model.dto.routing

import com.google.gson.annotations.SerializedName

/**
 * DTO para parsear la respuesta de la API de OSRM.
 */
data class OsrmResponse(
    @SerializedName("routes") val routes: List<OsrmRoute>
)

data class OsrmRoute(
    @SerializedName("duration") val duration: Double // Duración en segundos
)
