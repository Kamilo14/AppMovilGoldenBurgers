package com.example.goldenburgers.model.dto

import com.google.gson.annotations.SerializedName

data class ProductoDTO(
    @SerializedName("id") val id: Long?,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("descripcion") val descripcion: String?,
    @SerializedName("precio") val precio: Double,
    @SerializedName("imagen") val imagen: String?,
    @SerializedName("disponible") val disponible: Boolean,
    // El backend envía "categoria" como String (nombre) y "idCategoria" como Long
    @SerializedName("categoria") val categoria: String?, 
    @SerializedName("idCategoria") val idCategoria: Long?
)
