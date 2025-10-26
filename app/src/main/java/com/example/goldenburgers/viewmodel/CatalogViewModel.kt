package com.example.goldenburgers.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.goldenburgers.model.ProductRepository
import com.example.goldenburgers.model.Producto
import com.example.goldenburgers.model.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale


/**
 * Data class para representar un item dentro del carrito de compras.
 */
data class CartItem(
    val product: Producto,
    val quantity: Int
)

/**
 * [ACTUALIZADO] El estado ahora incluye el nombre del usuario logueado.
 */
data class CatalogUiState(
    val products: List<Producto> = emptyList(),
    val favorites: List<Producto> = emptyList(),
    val cartItems: List<CartItem> = emptyList(),
    val userName: String? = null // <-- AÑADIDO
) {
    val cartSubtotal: Double
        get() = cartItems.sumOf { it.product.precio * it.quantity }
}

/**
 * [ACTUALIZADO] ViewModel ahora recibe SessionManager y carga el nombre del usuario.
 */
class CatalogViewModel(
    val repository: ProductRepository,
    private val sessionManager: SessionManager // <-- AÑADIDO
) : ViewModel() {

    private val _uiState = MutableStateFlow(CatalogUiState())
    val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()

    init {
        observeProducts()
        observeFavorites()
        loadUserName() // <-- AÑADIDO
    }

    /**
     * [NUEVO] Carga el nombre del usuario actual y lo pone en el estado.
     */
    private fun loadUserName() {
        viewModelScope.launch {
            val userEmail = sessionManager.loggedInUserEmailFlow.first()
            if (userEmail != null) {
                val user = repository.findUserByEmail(userEmail)
                _uiState.update { it.copy(userName = user?.fullName) }
            }
        }
    }

    private fun observeProducts() {
        viewModelScope.launch {
            repository.allProducts
                .catch { exception -> println("Error observing products: $exception") }
                .collect { productList -> _uiState.update { it.copy(products = productList) } }
        }
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            repository.favoriteProducts
                .catch { exception -> println("Error observing favorites: $exception") }
                .collect { favoriteList -> _uiState.update { it.copy(favorites = favoriteList) } }
        }
    }

    fun toggleFavorite(productId: Int, isCurrentlyFavorite: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateFavorite(productId, !isCurrentlyFavorite)
        }
    }

    // --- LÓGICA DEL CARRITO ---

    fun addToCart(product: Producto) {
        _uiState.update { currentState ->
            val cart = currentState.cartItems.toMutableList()
            val existingItemIndex = cart.indexOfFirst { it.product.id == product.id }

            if (existingItemIndex != -1) {
                val existingItem = cart[existingItemIndex]
                cart[existingItemIndex] = existingItem.copy(quantity = existingItem.quantity + 1)
            } else {
                cart.add(CartItem(product = product, quantity = 1))
            }
            currentState.copy(cartItems = cart)
        }
    }

    fun increaseQuantity(productId: Int) {
        _uiState.update { currentState ->
            val updatedCart = currentState.cartItems.map {
                if (it.product.id == productId) it.copy(quantity = it.quantity + 1) else it
            }
            currentState.copy(cartItems = updatedCart)
        }
    }

    fun decreaseQuantity(productId: Int) {
        _uiState.update { currentState ->
            val cart = currentState.cartItems.toMutableList()
            val itemIndex = cart.indexOfFirst { it.product.id == productId }

            if (itemIndex != -1) {
                val item = cart[itemIndex]
                if (item.quantity > 1) {
                    cart[itemIndex] = item.copy(quantity = item.quantity - 1)
                } else {
                    cart.removeAt(itemIndex)
                }
            }
            currentState.copy(cartItems = cart)
        }
    }

    fun clearCart() {
        _uiState.update { currentState ->
            currentState.copy(cartItems = emptyList())
        }
    }
}

/**
 * Función de extensión para formatear un Double como moneda Chilena (CLP).
 */
fun Double.toCurrencyFormat(): String {
    val format = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CL"))
    format.maximumFractionDigits = 0
    return format.format(this).replace("CLP", "").trim()
}
