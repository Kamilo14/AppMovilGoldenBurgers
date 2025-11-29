package com.example.goldenburgers.model.dto

import com.google.gson.annotations.SerializedName

data class VentaDTO(
    @SerializedName("idVenta") val idVenta: Long?,
    @SerializedName("idPedido") val idPedido: Long,
    @SerializedName("totalVenta") val totalVenta: Double,
    @SerializedName("fechaVenta") val fechaVenta: String
)
