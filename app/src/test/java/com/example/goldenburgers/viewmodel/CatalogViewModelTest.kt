package com.example.goldenburgers.viewmodel

import com.example.goldenburgers.model.ProductRepository
import com.example.goldenburgers.model.SessionManager
import com.example.goldenburgers.model.data.Cliente
import com.example.goldenburgers.model.data.Producto
import com.example.goldenburgers.model.data.Rol
import com.example.goldenburgers.model.data.Usuario
import com.example.goldenburgers.repository.ClienteRepository
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

@ExperimentalCoroutinesApi
class CatalogViewModelTest : ShouldSpec() {

    private lateinit var productRepository: ProductRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var clienteRepository: ClienteRepository
    private lateinit var viewModel: CatalogViewModel

    private val testDispatcher = UnconfinedTestDispatcher()

    init {
        beforeTest {
            Dispatchers.setMain(testDispatcher)
            productRepository = mockk(relaxed = true)
            sessionManager = mockk(relaxed = true)
            clienteRepository = mockk(relaxed = true)
            viewModel = CatalogViewModel(productRepository, sessionManager, clienteRepository)
        }

        afterTest {
            Dispatchers.resetMain()
        }

        context("Funcionalidad del Carrito") {
            should("añadir un producto debería agregarlo a la lista de items y actualizar el subtotal") {
                val productoFalso = Producto(1L, 1L, "Burger", "desc", 5000.0, null, 1, "cat")
                viewModel.addToCart(productoFalso)
                val state = viewModel.uiState.value
                state.cartItems.size shouldBe 1
                state.cartSubtotal shouldBe 5000.0
            }

            should("añadir el mismo producto dos veces debería aumentar la cantidad") {
                val productoFalso = Producto(1L, 1L, "Doble Queso", null, 6000.0, null, 1, "Burgers")
                viewModel.addToCart(productoFalso)
                viewModel.addToCart(productoFalso)
                viewModel.uiState.value.cartItems.first().quantity shouldBe 2
            }

            should("increaseQuantity debería aumentar la cantidad y el subtotal") {
                val productoFalso = Producto(1L, 1L, "Burger", "desc", 5000.0, null, 1, "cat")
                viewModel.addToCart(productoFalso)
                viewModel.increaseQuantity(1L)
                val state = viewModel.uiState.value
                state.cartItems.first().quantity shouldBe 2
                state.cartSubtotal shouldBe 10000.0
            }

            should("decreaseQuantity debería reducir la cantidad y el subtotal") {
                val productoFalso = Producto(1L, 1L, "Doble Queso", null, 6000.0, null, 1, "Burgers")
                viewModel.addToCart(productoFalso)
                viewModel.addToCart(productoFalso)
                viewModel.decreaseQuantity(1L)
                val state = viewModel.uiState.value
                state.cartItems.first().quantity shouldBe 1
                state.cartSubtotal shouldBe 6000.0
            }

            should("decreaseQuantity a cero debería eliminar el producto del carrito") {
                val productoFalso = Producto(1L, 1L, "Doble Queso", null, 6000.0, null, 1, "Burgers")
                viewModel.addToCart(productoFalso)
                viewModel.decreaseQuantity(1L)
                val state = viewModel.uiState.value
                state.cartItems.isEmpty() shouldBe true
                state.cartSubtotal shouldBe 0.0
            }

            should("clearCart debería vaciar el carrito y resetear el subtotal") {
                val producto1 = Producto(1L, 1L, "Burger", null, 5000.0, null, 1, "Burgers")
                viewModel.addToCart(producto1)
                viewModel.clearCart()
                val state = viewModel.uiState.value
                state.cartItems.isEmpty() shouldBe true
                state.cartSubtotal shouldBe 0.0
            }
        }

        context("Funcionalidad de Favoritos") {
            should("toggleFavorite debería llamar a addFavorite si el producto no es favorito") {
                viewModel.toggleFavorite(123L, false)
                coVerify(exactly = 1) { productRepository.addFavorite(123L) }
            }

            should("toggleFavorite debería llamar a removeFavorite si el producto ya es favorito") {
                viewModel.toggleFavorite(456L, true)
                coVerify(exactly = 1) { productRepository.removeFavorite(456L) }
            }
        }

        context("Carga de Datos Iniciales") {
            should("loadInitialData debería observar y actualizar productos y favoritos") {
                // Arrange
                val fakeProducts = listOf(Producto(1L, 1L, "P1", "d", 1.0, null, 1, "c"))
                val fakeFavorites = listOf(Producto(2L, 1L, "P2", "d", 2.0, null, 1, "c"))
                every { productRepository.allProducts } returns flowOf(fakeProducts)
                every { productRepository.favoriteProducts } returns flowOf(fakeFavorites)

                // Act
                viewModel.loadInitialData()

                // Assert
                val state = viewModel.uiState.value
                state.products.size shouldBe 1
                state.products.first().idProducto shouldBe 1L
                state.favorites.size shouldBe 1
                state.favorites.first().idProducto shouldBe 2L
            }

            should("loadUserName debería usar el nombre del cliente si el cliente existe") {
                val fakeUsuario = Usuario("uid", "test@test.com", Rol(3, "C"), "fecha")
                val fakeCliente = Cliente(1L, fakeUsuario, "Usuario de Prueba", "phone", emptyList())
                coEvery { sessionManager.loggedInUserEmailFlow } returns flowOf("test@test.com")
                every { clienteRepository.currentCliente } returns MutableStateFlow(fakeCliente)
                viewModel.loadUserName()
                viewModel.uiState.value.userName shouldBe "Usuario de Prueba"
            }

            should("loadUserName debería usar el email si el cliente no existe") {
                coEvery { sessionManager.loggedInUserEmailFlow } returns flowOf("usuario_sin_perfil@test.com")
                every { clienteRepository.currentCliente } returns MutableStateFlow(null)
                viewModel.loadUserName()
                viewModel.uiState.value.userName shouldBe "usuario_sin_perfil"
            }
        }

        context("Casos Nulos y de Borde") {
            should("toggleFavorite con ID nulo no debería hacer nada") {
                viewModel.toggleFavorite(null, false)
                coVerify(exactly = 0) { productRepository.addFavorite(any()) }
                coVerify(exactly = 0) { productRepository.removeFavorite(any()) }
            }

            should("increaseQuantity con ID nulo no debería hacer nada") {
                val productoFalso = Producto(1L, 1L, "Burger", "desc", 5000.0, null, 1, "cat")
                viewModel.addToCart(productoFalso)
                val initialState = viewModel.uiState.value
                viewModel.increaseQuantity(null)
                viewModel.uiState.value shouldBe initialState // El estado no debe cambiar
            }

            should("decreaseQuantity con ID nulo no debería hacer nada") {
                val productoFalso = Producto(1L, 1L, "Burger", "desc", 5000.0, null, 1, "cat")
                viewModel.addToCart(productoFalso)
                val initialState = viewModel.uiState.value
                viewModel.decreaseQuantity(null)
                viewModel.uiState.value shouldBe initialState // El estado no debe cambiar
            }
        }
    }
}