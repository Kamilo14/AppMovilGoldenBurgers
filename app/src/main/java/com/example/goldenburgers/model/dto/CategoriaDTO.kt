package com.example.goldenburgers.model.dto

import com.google.gson.annotations.SerializedName

data class CategoriaDTO(
    @SerializedName("id") val id: Long?,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("descripcion") val descripcion: String?
)
