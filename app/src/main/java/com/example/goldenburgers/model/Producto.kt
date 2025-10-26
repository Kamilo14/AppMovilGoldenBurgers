package com.example.goldenburgers.model

import androidx.annotation.DrawableRes
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


/**
 * Data class que representa el modelo de un producto en la aplicación.
 * Es también una Entidad (@Entity) de Room para la base de datos local.
 */
@Entity(tableName = "productos") // Nombre de la tabla en la base de datos
data class Producto(
    @PrimaryKey val id: Int, // Clave primaria
    @ColumnInfo(name = "nombre") val nombre: String,
    @ColumnInfo(name = "descripcion") val descripcion: String,
    @ColumnInfo(name = "precio") val precio: Double,
    @ColumnInfo(name = "imagen_referencia") @DrawableRes val imagenReferencia: Int, // Referencia a drawable
    @ColumnInfo(name = "categoria") val categoria: String,
    @ColumnInfo(name = "es_favorito") var esFavorito: Boolean = false // Estado de favorito
)