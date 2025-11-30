package com.example.goldenburgers.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.goldenburgers.R

// [NUEVO] Se define la familia de fuentes para la tipografía personalizada
val steadyFontFamily = FontFamily(
    Font(R.font.steady, FontWeight.Normal)
)

val Typography = Typography(
    // [CORREGIDO] Se aplica la nueva fuente al estilo de texto más grande
    displayLarge = TextStyle(
        fontFamily = steadyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 72.sp, // Ajusta el tamaño según sea necesario
        lineHeight = 80.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)
