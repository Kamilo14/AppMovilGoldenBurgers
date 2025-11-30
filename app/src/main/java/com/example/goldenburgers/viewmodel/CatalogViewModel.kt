package com.example.goldenburgers.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.goldenburgers.model.ProductRepository
import com.example.goldenburgers.model.data.Producto
import com.example.goldenburgers.model.SessionManager
import com.example.goldenburgers.repository.ClienteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

data class CartItem(
    val product: Producto,
    val quantity: Int
)

data class CatalogUiState(
    val products: List<Producto> = emptyList(),
    val favorites: List<Producto> = emptyList(),
    val cartItems: List<CartItem> = emptyList(),
    val userName: String? = null,
    val cartSubtotal: Double = 0.0
)

class CatalogViewModel(
    private val repository: ProductRepository,
    private val sessionManager: SessionManager,
    private val clienteRepository: ClienteRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(CatalogUiState())
    val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()

    init {
        observeProducts()
        observeFavorites()
        loadUserName()
    }

    fun loadUserName() {
        viewModelScope.launch {
            val userEmail = sessionManager.loggedInUserEmailFlow.first()
            if (!userEmail.isNullOrBlank()) {
                val cliente = clienteRepository?.currentCliente?.value
                if (cliente != null) {
                    _uiState.update { it.copy(userName = cliente.nombreCliente) }
                } else {
                    val nameFromEmail = userEmail.split("@")[0]
                    _uiState.update { it.copy(userName = nameFromEmail) }
                }
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

    fun toggleFavorite(productId: Long?, isCurrentlyFavorite: Boolean) {
        if (productId == null) return
        
        viewModelScope.launch(Dispatchers.IO) {
            if (isCurrentlyFavorite) {
                repository.removeFavorite(productId)
            } else {
                repository.addFavorite(productId)
            }
        }
    }

    // --- LÓGICA DEL CARRITO ---

    fun addToCart(product: Producto) {
        _uiState.update { currentState ->
            val cart = currentState.cartItems.toMutableList()
            val existingItemIndex = cart.indexOfFirst { it.product.idProducto == product.idProducto }

            if (existingItemIndex != -1) {
                val existingItem = cart[existingItemIndex]
                cart[existingItemIndex] = existingItem.copy(quantity = existingItem.quantity + 1)
            } else {
                cart.add(CartItem(product = product, quantity = 1))
            }
            
            val newSubtotal = cart.sumOf { it.product.precioBase * it.quantity }
            currentState.copy(cartItems = cart, cartSubtotal = newSubtotal)
        }
    }

    fun increaseQuantity(productId: Long?) {
        if(productId == null) return
        _uiState.update { currentState ->
            val updatedCart = currentState.cartItems.map {
                if (it.product.idProducto == productId) it.copy(quantity = it.quantity + 1) else it
            }
            val newSubtotal = updatedCart.sumOf { it.product.precioBase * it.quantity }
            currentState.copy(cartItems = updatedCart, cartSubtotal = newSubtotal)
        }
    }

    fun decreaseQuantity(productId: Long?) {
        if(productId == null) return
        _uiState.update { currentState ->
            val cart = currentState.cartItems.toMutableList()
            val itemIndex = cart.indexOfFirst { it.product.idProducto == productId }

            if (itemIndex != -1) {
                val item = cart[itemIndex]
                if (item.quantity > 1) {
                    cart[itemIndex] = item.copy(quantity = item.quantity - 1)
                } else {
                    cart.removeAt(itemIndex)
                }
            }
            val newSubtotal = cart.sumOf { it.product.precioBase * it.quantity }
            currentState.copy(cartItems = cart, cartSubtotal = newSubtotal)
        }
    }

    fun clearCart() {
        _uiState.update { currentState ->
            currentState.copy(cartItems = emptyList(), cartSubtotal = 0.0)
        }
    }
}

fun Double.toCurrencyFormat(): String {
    val format = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CL"))
    format.maximumFractionDigits = 0
    return format.format(this).replace("CLP", "").trim()
}