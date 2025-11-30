package com.example.goldenburgers.model

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "session_prefs")

class SessionManager(private val context: Context) {

    private val loggedInUserEmailKey = stringPreferencesKey("logged_in_user_email")
    private val authTokenKey = stringPreferencesKey("auth_token")
    // [NUEVO] Clave para guardar la URI de la foto de perfil
    private val profileImageUriKey = stringPreferencesKey("profile_image_uri")

    val loggedInUserEmailFlow: Flow<String?> = context.sessionDataStore.data
        .map { preferences ->
            preferences[loggedInUserEmailKey]
        }
        
    val authTokenFlow: Flow<String?> = context.sessionDataStore.data
        .map { preferences ->
            preferences[authTokenKey]
        }

    // [NUEVO] Flow para observar la URI de la foto de perfil
    val profileImageUriFlow: Flow<String?> = context.sessionDataStore.data
        .map { preferences ->
            preferences[profileImageUriKey]
        }

    suspend fun saveUserSession(email: String, token: String? = null) {
        context.sessionDataStore.edit { preferences ->
            preferences[loggedInUserEmailKey] = email
            if (token != null) {
                preferences[authTokenKey] = token
            }
        }
    }
    
    suspend fun saveToken(token: String) {
        context.sessionDataStore.edit { preferences ->
            preferences[authTokenKey] = token
        }
    }

    // [NUEVO] Función para guardar la URI de la foto de perfil
    suspend fun saveProfileImageUri(uri: String) {
        context.sessionDataStore.edit { preferences ->
            preferences[profileImageUriKey] = uri
        }
    }

    suspend fun clearUserSession() {
        context.sessionDataStore.edit { preferences ->
            preferences.remove(loggedInUserEmailKey)
            preferences.remove(authTokenKey)
            // [NUEVO] Limpiar también la foto de perfil al cerrar sesión
            preferences.remove(profileImageUriKey)
        }
    }
}