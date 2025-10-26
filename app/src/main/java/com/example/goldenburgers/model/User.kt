package com.example.goldenburgers.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Data class que representa el modelo de un usuario y la tabla 'users' en la base de datos.
 */
@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val email: String,
    val password: String,
    val fullName: String,
    val phoneNumber: String,
    val gender: String,
    val birthDate: String,
    val street: String,
    val number: String,
    val city: String,
    val region: String,
    val commune: String,
    val profileImageUri: String? = null
)
