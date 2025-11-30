package com.example.goldenburgers.model

import com.example.goldenburgers.model.dto.ProductoDTO
import com.example.goldenburgers.repository.FavoritesRepository
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first

@ExperimentalCoroutinesApi
class ProductRepositoryTest : ShouldSpec() {

    private lateinit var networkSource: ProductNetworkSource
    private lateinit var favoritesRepository: FavoritesRepository
    private lateinit var repository: ProductRepository

    init {
        beforeTest {
            networkSource = mockk()
            favoritesRepository = mockk(relaxed = true)
            repository = ProductRepository(networkSource, favoritesRepository)
        }

        context("Obtención de Productos") {
            should("debería obtener y mapear productos cuando la red responde con éxito") {
                // Arrange
                // [CORRECCIÓN CLAVE] Se usa el constructor con argumentos nombrados para coincidir con la data class
                val fakeDto = ProductoDTO(
                    id = 1L,
                    nombre = "Burger DTO",
                    descripcion = "desc",
                    precio = 5000.0,
                    imagen = "http://localhost/img.png",
                    disponible = true,
                    categoria = "Burgers",
                    idCategoria = 1L
                )
                coEvery { networkSource.fetchProducts() } returns listOf(fakeDto)

                // Act
                val productos = repository.allProducts.first()

                // Assert
                productos.size shouldBe 1
                val producto = productos.first()
                producto.idProducto shouldBe 1L
                producto.nombreProducto shouldBe "Burger DTO"
                producto.imagenUrl shouldBe "http://10.0.2.2/img.png"
            }

            should("debería emitir una lista vacía cuando la red lanza una excepción") {
                // Arrange
                coEvery { networkSource.fetchProducts() } throws RuntimeException("Error de red simulado")

                // Act
                val productos = repository.allProducts.first()

                // Assert
                productos.isEmpty() shouldBe true
            }
        }

        context("Gestión de Favoritos") {
            should("addFavorite debería delegar la llamada al favoritesRepository") {
                val productId = 123L
                repository.addFavorite(productId)
                coVerify(exactly = 1) { favoritesRepository.addFavorite(productId) }
            }

            should("removeFavorite debería delegar la llamada al favoritesRepository") {
                val productId = 456L
                repository.removeFavorite(productId)
                coVerify(exactly = 1) { favoritesRepository.removeFavorite(productId) }
            }
        }
    }
}