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
 * Gestiona la sesión del usuario.
 * [ACTUALIZADO] Ahora guarda el email del usuario logueado en lugar de un booleano.
 */
class SessionManager(private val context: Context) {

    // La clave para guardar el email del usuario.
    private val loggedInUserEmailKey = stringPreferencesKey("logged_in_user_email")

    /**
     * Un Flow que emite el email del usuario logueado.
     * Si no hay nadie logueado, emite null.
     */
    val loggedInUserEmailFlow: Flow<String?> = context.sessionDataStore.data
        .map { preferences ->
            preferences[loggedInUserEmailKey]
        }

    /**
     * Guarda el email del usuario para marcarlo como logueado.
     * @param email El email del usuario que ha iniciado sesión.
     */
    suspend fun saveUserSession(email: String) {
        context.sessionDataStore.edit { preferences ->
            preferences[loggedInUserEmailKey] = email
        }
    }

    /**
     * Limpia la sesión del usuario, eliminando su email guardado.
     */
    suspend fun clearUserSession() {
        context.sessionDataStore.edit { preferences ->
            preferences.remove(loggedInUserEmailKey)
        }
    }
}
