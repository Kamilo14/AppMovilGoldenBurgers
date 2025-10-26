package com.example.goldenburgers.ui.theme



import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext


private val DarkColorScheme = darkColorScheme(
    primary = GolgerYellowDark,
    onPrimary = GolgerDark,
    secondary = GolgerDarkSurface,
    onSecondary = Color.White,
    tertiary = GolgerGreenSuccess,
    background = GolgerDarkBackground,
    onBackground = Color.White,
    surface = GolgerDarkSurface,
    onSurface = Color.White,
    onSurfaceVariant = Color.Gray,
    outline = Color.DarkGray,
    error = GolgerRedError,
    onError = Color.White
)


private val LightColorScheme = lightColorScheme(
    primary = GolgerYellow,
    onPrimary = GolgerDark,
    secondary = GolgerDark,
    onSecondary = Color.White,
    tertiary = GolgerGreenSuccess,
    onTertiary = Color.White,
    background = GolgerLightGray,
    onBackground = GolgerDark,
    surface = Color.White, // Color de las Cards
    onSurface = GolgerDark,
    onSurfaceVariant = Color.Gray, // Para textos secundarios/pistas
    outline = Color.LightGray, // Para bordes
    error = GolgerRedError,
    onError = Color.White
)


@Composable
fun GolgerBurguerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }


        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }


    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // 'Typography' viene de tu archivo Type.kt
        content = content
    )
}

