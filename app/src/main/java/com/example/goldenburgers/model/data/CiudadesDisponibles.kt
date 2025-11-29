package com.example.goldenburgers.model.data

/**
 * Catálogo de ciudades disponibles en el sistema
 * Estas son las únicas ciudades válidas para direcciones de clientes
 */
object CiudadesDisponibles {

    data class CiudadItem(val id: Long, val nombre: String)

    val ciudades = listOf(
        CiudadItem(1, "Viña del Mar"),
        CiudadItem(2, "Valparaíso"),
        CiudadItem(3, "Curauma"),
        CiudadItem(4, "Quilpué"),
        CiudadItem(5, "Villa Alemana")
    )

    /**
     * Intenta encontrar la ciudad más cercana basada en el nombre detectado por GPS
     * @param nombreDetectado Nombre de la ciudad detectada por Geocoder
     * @return ID de la ciudad si se encuentra coincidencia, null si no
     */
    fun encontrarCiudad(nombreDetectado: String): Long? {
        val nombreLimpio = nombreDetectado.trim().lowercase()

        // Buscar coincidencia exacta primero
        ciudades.forEach { ciudad ->
            if (ciudad.nombre.lowercase() == nombreLimpio) {
                return ciudad.id
            }
        }

        // Buscar coincidencia parcial
        ciudades.forEach { ciudad ->
            if (nombreLimpio.contains(ciudad.nombre.lowercase()) ||
                ciudad.nombre.lowercase().contains(nombreLimpio)) {
                return ciudad.id
            }
        }

        return null
    }

    /**
     * Obtiene el nombre de una ciudad por su ID
     */
    fun obtenerNombrePorId(id: Long): String? {
        return ciudades.find { it.id == id }?.nombre
    }
}
