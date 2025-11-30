package com.example.goldenburgers.repository

import com.example.goldenburgers.model.data.Cliente
import com.example.goldenburgers.model.dto.CiudadDTO
import com.example.goldenburgers.model.dto.ClienteDTO
import com.example.goldenburgers.model.dto.CrearDireccionRequest
import com.example.goldenburgers.model.dto.DireccionClienteDTO
import com.example.goldenburgers.model.dto.RolDTO
import com.example.goldenburgers.model.dto.UsuarioDTO
import com.example.goldenburgers.service.GestionUsuarioApiService
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import retrofit2.Response

@ExperimentalCoroutinesApi
class ClienteRepositoryTest : ShouldSpec() {

    private lateinit var networkSource: ClienteNetworkSource
    private lateinit var apiService: GestionUsuarioApiService
    private lateinit var repository: ClienteRepository

    init {
        beforeTest {
            networkSource = mockk()
            apiService = mockk()
            repository = ClienteRepository(networkSource)

            // Siempre simulamos que el networkSource devuelve nuestro apiService falso
            coEvery { networkSource.getService() } returns apiService
        }

        context("Obtención de Cliente") {
            should("debería obtener y mapear un cliente cuando la API responde con éxito") {
                // Arrange: Preparamos un DTO falso y una respuesta exitosa
                val fakeRolDto = RolDTO(3, "Cliente")
                val fakeUserDto = UsuarioDTO("uid-falso", "test@test.com", fakeRolDto, "2025-01-01")
                val fakeClienteDto = ClienteDTO(1L, fakeUserDto, "Test User", "123456789", emptyList())
                val fakeResponse = Response.success(fakeClienteDto)

                coEvery { apiService.obtenerClientePorFirebaseUid(any()) } returns fakeResponse

                // Act: Llamamos al método del repositorio
                val result = repository.obtenerClientePorFirebaseUid("uid-falso")

                // Assert: Verificamos el resultado
                result.isSuccess shouldBe true
                val cliente = result.getOrNull()
                cliente.shouldBeInstanceOf<Cliente>()
                cliente?.idCliente shouldBe 1L
                cliente?.nombreCliente shouldBe "Test User"

                // Verificamos que el StateFlow se actualizó
                repository.currentCliente.first()?.idCliente shouldBe 1L
            }
        }

        context("Creación de Dirección") {
            should("debería llamar al servicio de API con los datos correctos") {
                // Arrange
                val fakeDireccionDto = DireccionClienteDTO(10L, 1L, CiudadDTO(1L, "Viña"), "Calle Falsa 123", "Casa")
                val fakeResponse = Response.success(fakeDireccionDto)
                coEvery { apiService.crearDireccion(any()) } returns fakeResponse

                // Act
                repository.crearDireccion(1L, 1L, "Calle Falsa 123", "Casa")

                // Assert: Verificamos que se llamó al método de la API con un request que contiene los datos correctos
                coVerify(exactly = 1) { apiService.crearDireccion(match {
                    it.idCliente == 1L &&
                    it.idCiudad == 1L &&
                    it.direccion == "Calle Falsa 123" &&
                    it.alias == "Casa"
                }) }
            }
        }
    }
}