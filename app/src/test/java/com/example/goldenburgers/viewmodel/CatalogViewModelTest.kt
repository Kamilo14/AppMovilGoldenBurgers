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
import kotlinx.coroutines.flow.flow
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
            
            // [CORRECCIÓN CRÍTICA]
            // Configuramos los mocks para que los Flows emitan al menos un valor por defecto.
            // Esto evita la excepción "NoSuchElementException: Expected at least one element"
            // que ocurre cuando se llama a .first() sobre un Flow vacío.
            coEvery { sessionManager.loggedInUserEmailFlow } returns flowOf(null)
            every { clienteRepository.currentCliente } returns MutableStateFlow(null)
            every { productRepository.allProducts } returns flowOf(emptyList())
            every { productRepository.favoriteProducts } returns flowOf(emptyList())

            viewModel = CatalogViewModel(productRepository, sessionManager, clienteRepository)
        }

        afterTest {
            Dispatchers.resetMain()
        }

        context("Funcionalidad del Carrito") {
            should("addToCart debería agregar un nuevo producto") {
                val producto = Producto(1L, 1L, "Burger", "desc", 5000.0, null, 1, "cat")
                viewModel.addToCart(producto)
                val state = viewModel.uiState.value
                state.cartItems.size shouldBe 1
                state.cartSubtotal shouldBe 5000.0
            }

            should("addToCart debería incrementar la cantidad de un producto existente") {
                val producto = Producto(1L, 1L, "Doble Queso", null, 6000.0, null, 1, "Burgers")
                viewModel.addToCart(producto)
                viewModel.addToCart(producto)
                viewModel.uiState.value.cartItems.first().quantity shouldBe 2
                viewModel.uiState.value.cartSubtotal shouldBe 12000.0
            }

            should("increaseQuantity debería aumentar la cantidad") {
                val producto = Producto(1L, 1L, "Burger", "desc", 5000.0, null, 1, "cat")
                viewModel.addToCart(producto)
                viewModel.increaseQuantity(1L)
                viewModel.uiState.value.cartItems.first().quantity shouldBe 2
            }

            should("decreaseQuantity debería reducir la cantidad") {
                val producto = Producto(1L, 1L, "Doble Queso", null, 6000.0, null, 1, "Burgers")
                viewModel.addToCart(producto)
                viewModel.addToCart(producto)
                viewModel.decreaseQuantity(1L)
                viewModel.uiState.value.cartItems.first().quantity shouldBe 1
            }

            should("decreaseQuantity debería eliminar el producto si la cantidad es 1") {
                val producto = Producto(1L, 1L, "Doble Queso", null, 6000.0, null, 1, "Burgers")
                viewModel.addToCart(producto)
                viewModel.decreaseQuantity(1L)
                viewModel.uiState.value.cartItems.isEmpty() shouldBe true
            }

            should("clearCart debería vaciar el carrito") {
                viewModel.addToCart(Producto(1L, 1L, "Burger", null, 5000.0, null, 1, "Burgers"))
                viewModel.clearCart()
                viewModel.uiState.value.cartItems.isEmpty() shouldBe true
                viewModel.uiState.value.cartSubtotal shouldBe 0.0
            }
        }

        context("Funcionalidad de Favoritos") {
            should("toggleFavorite debería añadir a favoritos") {
                viewModel.toggleFavorite(123L, false)
                coVerify(exactly = 1) { productRepository.addFavorite(123L) }
            }

            should("toggleFavorite debería eliminar de favoritos") {
                viewModel.toggleFavorite(456L, true)
                coVerify(exactly = 1) { productRepository.removeFavorite(456L) }
            }
        }

        context("Carga de Datos") {
            should("loadInitialData debería cargar productos y favoritos") {
                val fakeProducts = listOf(Producto(1L, 1L, "P1", "d", 1.0, null, 1, "c"))
                val fakeFavorites = listOf(Producto(2L, 1L, "P2", "d", 2.0, null, 1, "c"))
                // Sobrescribimos los mocks genéricos con datos específicos para este test
                every { productRepository.allProducts } returns flowOf(fakeProducts)
                every { productRepository.favoriteProducts } returns flowOf(fakeFavorites)
                
                viewModel.loadInitialData()
                
                viewModel.uiState.value.products shouldBe fakeProducts
                viewModel.uiState.value.favorites shouldBe fakeFavorites
            }

            should("loadUserName debería usar el nombre del cliente si existe") {
                val fakeCliente = Cliente(1L, Usuario("uid", "t@t.com", Rol(3, "C"), "f"), "Test User", "p", emptyList())
                coEvery { sessionManager.loggedInUserEmailFlow } returns flowOf("t@t.com")
                every { clienteRepository.currentCliente } returns MutableStateFlow(fakeCliente)
                
                viewModel.loadUserName()
                
                viewModel.uiState.value.userName shouldBe "Test User"
            }

            should("loadUserName debería usar el email si el cliente no existe") {
                coEvery { sessionManager.loggedInUserEmailFlow } returns flowOf("no-cliente@test.com")
                every { clienteRepository.currentCliente } returns MutableStateFlow(null)
                
                viewModel.loadUserName()
                
                viewModel.uiState.value.userName shouldBe "no-cliente"
            }
        }

        context("Casos de Borde y Errores") {
            should("ignorar toggleFavorite si el ID es nulo") {
                viewModel.toggleFavorite(null, false)
                coVerify(exactly = 0) { productRepository.addFavorite(any()) }
                coVerify(exactly = 0) { productRepository.removeFavorite(any()) }
            }

            should("ignorar increaseQuantity si el ID es nulo o no existe") {
                val producto = Producto(1L, 1L, "Burger", "d", 5000.0, null, 1, "c")
                viewModel.addToCart(producto)
                val initialState = viewModel.uiState.value
                viewModel.increaseQuantity(null)
                viewModel.increaseQuantity(999L) // ID no existente
                viewModel.uiState.value shouldBe initialState
            }

            should("ignorar decreaseQuantity si el ID es nulo o no existe") {
                val producto = Producto(1L, 1L, "Burger", "d", 5000.0, null, 1, "c")
                viewModel.addToCart(producto)
                val initialState = viewModel.uiState.value
                viewModel.decreaseQuantity(null)
                viewModel.decreaseQuantity(999L) // ID no existente
                viewModel.uiState.value shouldBe initialState
            }

            should("no crashear si la carga de productos falla") {
                every { productRepository.allProducts } returns flow { throw Exception("DB Error") }
                viewModel.loadInitialData()
                viewModel.uiState.value.products.isEmpty() shouldBe true
            }
            
            should("no crashear si la carga de favoritos falla") {
                every { productRepository.favoriteProducts } returns flow { throw Exception("DB Error") }
                viewModel.loadInitialData()
                viewModel.uiState.value.favorites.isEmpty() shouldBe true
            }
            
            should("no hacer nada si el email del usuario es nulo") {
                 coEvery { sessionManager.loggedInUserEmailFlow } returns flowOf(null)
                 val initialState = viewModel.uiState.value
                 viewModel.loadUserName()
                 viewModel.uiState.value.userName shouldBe initialState.userName
            }
        }
    }
}
