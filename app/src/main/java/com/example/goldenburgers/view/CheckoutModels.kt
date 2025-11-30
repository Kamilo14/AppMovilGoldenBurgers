package com.example.goldenburgers.view

/**
 * Modelos de datos para las opciones en la pantalla de Checkout.
 * Se mueven a su propio archivo para que puedan ser compartidos entre la Vista y el ViewModel.
 */
data class DeliveryOption(val id: Long, val title: String, val description: String, val cost: Double)
data class PaymentOption(val id: Long, val title: String)
