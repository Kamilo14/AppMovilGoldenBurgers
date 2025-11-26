package com.example.goldenburgers.model.mapper

import com.example.goldenburgers.model.data.*
import com.example.goldenburgers.model.dto.*

/**
 * Mappers para convertir entre DTOs y modelos de dominio
 */

// --- Rol ---
fun RolDTO.toDomain(): Rol = Rol(
    idRol = this.idRol,
    nombreRol = this.nombreRol
)

// --- Ciudad ---
fun CiudadDTO.toDomain(): Ciudad = Ciudad(
    idCiudad = this.idCiudad,
    nombreCiudad = this.nombreCiudad
)

// --- Usuario ---
fun UsuarioDTO.toDomain(): Usuario = Usuario(
    idUsuario = this.idUsuario,
    email = this.email,
    rol = this.rol.toDomain(),
    fechaCreacion = this.fechaCreacion
)

// --- DireccionCliente ---
fun DireccionClienteDTO.toDomain(): DireccionCliente = DireccionCliente(
    idDireccion = this.idDireccion,
    idCliente = this.idCliente,
    ciudad = this.ciudad.toDomain(),
    direccion = this.direccion,
    alias = this.alias
)

// --- Cliente ---
fun ClienteDTO.toDomain(): Cliente = Cliente(
    idCliente = this.idCliente,
    usuario = this.usuario.toDomain(),
    nombreCliente = this.nombreCliente,
    telefonoCliente = this.telefonoCliente,
    direcciones = this.direcciones.map { it.toDomain() }
)
