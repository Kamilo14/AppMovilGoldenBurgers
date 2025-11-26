package com.example.goldenburgers.model.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO para Dirección de Cliente - usado en comunicación con API
 */
data class DireccionClienteDTO(
    @SerializedName("idDireccion")
    val idDireccion: Long,

    @SerializedName("idCliente")
    val idCliente: Long,

    @SerializedName("ciudad")
    val ciudad: CiudadDTO,

    @SerializedName("direccion")
    val direccion: String,

    @SerializedName("alias")
    val alias: String? = null
)
