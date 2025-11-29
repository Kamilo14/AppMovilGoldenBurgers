package com.example.goldenburgers.model.dto

import com.google.gson.annotations.SerializedName

data class DevolucionDTO(
    @SerializedName("idDevolucion") val idDevolucion: Long?,
    @SerializedName("idVenta") val idVenta: Long,
    @SerializedName("montoDevuelto") val montoDevuelto: Double,
    @SerializedName("motivo") val motivo: String?,
    @SerializedName("fechaDevolucion") val fechaDevolucion: String
)
