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


/**
 * Data class para representar un item dentro del carrito de compras.
 */
data class CartItem(
    val product: Producto,
    val quantity: Int
)

/**
 * El estado ahora incluye el nombre del usuario logueado.
 */
data class CatalogUiState(
    val products: List<Producto> = emptyList(),
    val favorites: List<Producto> = emptyList(),
    val cartItems: List<CartItem> = emptyList(),
    val userName: String? = null, // <-- AÑADIDO
    val cartSubtotal: Double = 0.0
) {
    // Calculamos el subtotal dinámicamente si no viene en el constructor,
    // pero para simplificar y evitar loops, mejor lo calculamos al actualizar el estado.
}

/**
 * [ACTUALIZADO] ViewModel ahora recibe SessionManager y carga el nombre del usuario.
 * Necesitamos ClienteRepository para buscar al usuario, ya no ProductRepository.
 */
class CatalogViewModel(
    val repository: ProductRepository,
    private val sessionManager: SessionManager,
    private val clienteRepository: ClienteRepository? = null // Opcional por compatibilidad temporal
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
    fun loadUserName() {
        viewModelScope.launch {
            val userEmail = sessionManager.loggedInUserEmailFlow.first()
            if (!userEmail.isNullOrBlank()) {
                // TODO: Idealmente deberíamos tener un método para buscar por email en ClienteRepository
                // O usar el ID del usuario logueado si SessionManager lo tuviera.
                // Por ahora, intentamos obtener el cliente actual del repositorio si ya se cargó.
                val cliente = clienteRepository?.currentCliente?.value
                
                if (cliente != null) {
                     _uiState.update { it.copy(userName = cliente.nombreCliente) }
                } else {
                    // Si no tenemos repositorio de clientes inyectado o no hay cliente cargado,
                    // usamos el email como fallback (solo la parte antes del @)
                    val nameFromEmail = userEmail.split("@").firstOrNull() ?: "Usuario"
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
        
        // Actualizamos el estado localmente para reflejar el cambio en la UI inmediatamente
        _uiState.update { currentState ->
            val currentFavorites = currentState.favorites.toMutableList()
            if (isCurrentlyFavorite) {
                // Si ya es favorito, lo quitamos
                currentFavorites.removeAll { it.idProducto == productId }
            } else {
                // Si no es favorito, lo buscamos en la lista de productos y lo agregamos
                val product = currentState.products.find { it.idProducto == productId }
                if (product != null) {
                    currentFavorites.add(product)
                }
            }
            currentState.copy(favorites = currentFavorites)
        }

        // TODO: Implementar persistencia en repositorio cuando el backend lo soporte
        /*
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateFavorite(productId, !isCurrentlyFavorite)
        }
        */
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

/**
 * Función de extensión para formatear un Double como moneda Chilena (CLP).
 */
fun Double.toCurrencyFormat(): String {
    val format = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CL"))
    format.maximumFractionDigits = 0
    return format.format(this).replace("CLP", "").trim()
}
