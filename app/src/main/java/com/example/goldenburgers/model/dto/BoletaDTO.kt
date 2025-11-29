package com.example.goldenburgers.model.dto

import com.google.gson.annotations.SerializedName

data class BoletaDTO(
    @SerializedName("idBoleta") val idBoleta: Long?,
    @SerializedName("idVenta") val idVenta: Long,
    @SerializedName("numeroSii") val numeroSii: String,
    @SerializedName("urlDocumento") val urlDocumento: String?,
    @SerializedName("iva") val iva: Double,
    @SerializedName("totalConIva") val totalConIva: Double?
)
