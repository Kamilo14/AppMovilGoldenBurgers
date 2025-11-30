package com.example.goldenburgers.view

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.goldenburgers.navigation.AppScreens
import com.example.goldenburgers.viewmodel.CatalogViewModel
import com.example.goldenburgers.viewmodel.FakePaymentViewModel
import com.example.goldenburgers.viewmodel.PaymentResult
import com.example.goldenburgers.viewmodel.toCurrencyFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FakePaymentScreen(
    navController: NavController,
    viewModel: FakePaymentViewModel,
    catalogViewModel: CatalogViewModel, 
    orderId: Long,
    totalAmount: Double
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var cardNumber by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var cardHolderName by remember { mutableStateOf("") }
    
    // --- [NUEVO] Estados para validación y animación ---
    var cardError by remember { mutableStateOf<String?>(null) }
    val shakeController = remember { Animatable(0f) }

    LaunchedEffect(cardError) {
        if (cardError != null) {
            shakeController.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 500
                    -10f at 100; 10f at 200; -10f at 300; 10f at 400; 0f at 500
                }
            )
        }
    }

    LaunchedEffect(uiState.paymentResult) {
        when (uiState.paymentResult) {
            PaymentResult.SUCCESS -> {
                catalogViewModel.clearCart()
                navController.navigate("${AppScreens.PaymentResultScreen.route}/true") { popUpTo(AppScreens.CheckoutScreen.route) { inclusive = true } }
                viewModel.resetPaymentResult()
            }
            PaymentResult.FAILURE -> {
                navController.navigate("${AppScreens.PaymentResultScreen.route}/false") { popUpTo(AppScreens.CheckoutScreen.route) { inclusive = true } }
                viewModel.resetPaymentResult()
            }
            null -> {}
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Simulación de Pago") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } }) }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Total a Pagar: ${totalAmount.toCurrencyFormat()}", style = MaterialTheme.typography.headlineSmall)
                    
                    OutlinedTextField(
                        value = cardNumber,
                        onValueChange = { 
                            if (it.length <= 16 && it.all(Char::isDigit)) {
                                cardNumber = it
                                cardError = null // Limpia el error al escribir
                            }
                         },
                        label = { Text("Número de Tarjeta") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().graphicsLayer { translationX = shakeController.value },
                        isError = cardError != null,
                        supportingText = { cardError?.let { Text(it) } }
                    )
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = expiryDate,
                            onValueChange = { if (it.all(Char::isDigit) && it.length <= 4) expiryDate = it },
                            label = { Text("MM/AA") },
                            visualTransformation = DateVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(16.dp))
                        OutlinedTextField(
                            value = cvv,
                            onValueChange = { if (it.all(Char::isDigit)) cvv = it },
                            label = { Text("CVV") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = cardHolderName, 
                        onValueChange = { cardHolderName = it }, 
                        label = { Text("Nombre del Titular") }, 
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            var isValid = true
                            if (cardNumber.length != 16) {
                                cardError = "La tarjeta debe tener 16 dígitos."
                                isValid = false
                            }
                            // Se podrían añadir validaciones para los otros campos aquí
                            
                            if (isValid) {
                                viewModel.processPayment(cardNumber, orderId)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text("Pagar")
                    }
                }
            }
        }
    }
}

private class DateVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = if (text.text.length >= 4) text.text.substring(0..3) else text.text
        var out = ""
        for (i in trimmed.indices) {
            out += trimmed[i]
            if (i == 1) out += "/"
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 1) return offset
                if (offset <= 4) return offset + 1
                return 5
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 2) return offset
                if (offset <= 5) return offset - 1
                return 4
            }
        }

        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}