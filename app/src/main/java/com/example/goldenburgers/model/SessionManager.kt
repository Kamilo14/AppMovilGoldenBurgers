package com.example.goldenburgers.model

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// El DataStore ahora se llamará "session_prefs" para reflejar su propósito.
private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "session_prefs")

/**
 * Gestion de la sesión del usuario.
 */
class SessionManager(private val context: Context) {

    // Claves para guardar los datos del usuario
    private val loggedInUserEmailKey = stringPreferencesKey("logged_in_user_email")
    private val authTokenKey = stringPreferencesKey("auth_token") // NUEVO: Para guardar el token JWT

    /**
     * Un Flow que emite el email del usuario logueado.
     * Si no hay nadie logueado, emite null.
     */
    val loggedInUserEmailFlow: Flow<String?> = context.sessionDataStore.data
        .map { preferences ->
            preferences[loggedInUserEmailKey]
        }
        
    /**
     * Un Flow que emite el token de autenticación.
     * Útil para clientes reactivos.
     */
    val authTokenFlow: Flow<String?> = context.sessionDataStore.data
        .map { preferences ->
            preferences[authTokenKey]
        }

    /**
     * Guarda el email del usuario para marcarlo como logueado.
     * @param email El email del usuario que ha iniciado sesión.
     */
    suspend fun saveUserSession(email: String, token: String? = null) {
        context.sessionDataStore.edit { preferences ->
            preferences[loggedInUserEmailKey] = email
            if (token != null) {
                preferences[authTokenKey] = token
            }
        }
    }
    
    /**
     * Guarda solo el token (por si se renueva independientemente del login)
     */
    suspend fun saveToken(token: String) {
        context.sessionDataStore.edit { preferences ->
            preferences[authTokenKey] = token
        }
    }

    /**
     * Limpia la sesión del usuario, eliminando email y token.
     */
    suspend fun clearUserSession() {
        context.sessionDataStore.edit { preferences ->
            preferences.remove(loggedInUserEmailKey)
            preferences.remove(authTokenKey)
        }
    }
}
