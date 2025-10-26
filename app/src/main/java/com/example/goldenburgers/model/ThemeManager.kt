package com.example.goldenburgers.model

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extensión para acceder a un DataStore diferente, dedicado solo al tema.
private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_prefs")

/**
 * Gestiona la preferencia del tema de la aplicación (Modo Oscuro / Claro).
 */
class ThemeManager(private val context: Context) {

    private val isDarkModeKey = booleanPreferencesKey("is_dark_mode_enabled")

    /**
     * Un Flow que emite `true` si el modo oscuro está activado, y `false` en caso contrario.
     */
    val isDarkMode: Flow<Boolean> = context.themeDataStore.data
        .map { preferences ->
            preferences[isDarkModeKey] ?: false
        }

    /**
     * Guarda la preferencia del tema.
     * @param isDarkMode `true` para activar el modo oscuro, `false` para desactivarlo.
     */
    suspend fun setDarkMode(isDarkMode: Boolean) {
        // [CORREGIDO] Se añade el nombre del parámetro 'preferences ->' al lambda.
        context.themeDataStore.edit { preferences ->
            preferences[isDarkModeKey] = isDarkMode
        }
    }
}

