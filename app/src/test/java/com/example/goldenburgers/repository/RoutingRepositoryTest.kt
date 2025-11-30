package com.example.goldenburgers.repository

import com.example.goldenburgers.model.dto.geocoding.NominatimPlaceDTO
import com.example.goldenburgers.model.dto.routing.OsrmResponse
import com.example.goldenburgers.model.dto.routing.OsrmRoute
import com.example.goldenburgers.service.NominatimApiService
import com.example.goldenburgers.service.OsrmApiService
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk

class RoutingRepositoryTest : ShouldSpec() {

    private lateinit var nominatimApi: NominatimApiService
    private lateinit var osrmApi: OsrmApiService
    private lateinit var repository: RoutingRepository

    init {
        beforeTest {
            // Antes de cada test, creamos las simulaciones (mocks)
            nominatimApi = mockk()
            osrmApi = mockk()
            repository = RoutingRepository(nominatimApi, osrmApi)
        }

        context("Cálculo de Tiempo de Entrega") {

            should("debería devolver el tiempo de entrega estimado cuando todas las APIs responden correctamente") {
                // Arrange: Preparamos las respuestas falsas de las APIs
                val restaurantCoords = NominatimPlaceDTO(latitude = "-33.0", longitude = "-71.5")
                val clientCoords = NominatimPlaceDTO(latitude = "-33.1", longitude = "-71.6")

                // Simulamos que Nominatim devuelve las coordenadas para el restaurante
                coEvery { nominatimApi.search(match { it.contains("Etchevers") }) } returns listOf(restaurantCoords)
                // Simulamos que Nominatim devuelve las coordenadas para el cliente
                coEvery { nominatimApi.search(match { it.contains("Direccion Cliente") }) } returns listOf(clientCoords)
                
                // Simulamos que OSRM devuelve una duración de 600 segundos (10 minutos)
                val travelTimeSeconds = 600.0
                val osrmResponse = OsrmResponse(routes = listOf(OsrmRoute(duration = travelTimeSeconds)))
                coEvery { osrmApi.getRoute(any(), any()) } returns osrmResponse

                // Act: Ejecutamos la función a probar
                val result = repository.getEstimatedDeliveryTime("Direccion Cliente 123")

                // Assert: Verificamos que el resultado sea el esperado
                // 15 minutos (preparación) + 10 minutos (viaje) = 25 minutos
                result shouldBe 25
            }

            should("debería devolver null si Nominatim no encuentra el restaurante") {
                // Arrange: Simulamos que la API no devuelve resultados para el restaurante
                coEvery { nominatimApi.search(match { it.contains("Etchevers") }) } returns emptyList()

                // Act
                val result = repository.getEstimatedDeliveryTime("Direccion Cliente 123")

                // Assert
                result shouldBe null
            }

            should("debería devolver null si OSRM no devuelve una ruta") {
                 // Arrange
                val restaurantCoords = NominatimPlaceDTO(latitude = "-33.0", longitude = "-71.5")
                val clientCoords = NominatimPlaceDTO(latitude = "-33.1", longitude = "-71.6")
                coEvery { nominatimApi.search(any()) } returns listOf(restaurantCoords, clientCoords)
                
                // Simulamos que OSRM no encuentra una ruta
                coEvery { osrmApi.getRoute(any(), any()) } returns OsrmResponse(routes = emptyList())

                // Act
                val result = repository.getEstimatedDeliveryTime("Direccion Cliente 123")

                // Assert
                result shouldBe null
            }
        }
    }
}