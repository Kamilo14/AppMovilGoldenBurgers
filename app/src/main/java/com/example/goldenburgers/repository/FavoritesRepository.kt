package com.example.goldenburgers.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.goldenburgers.model.SessionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

class FavoritesRepository(private val context: Context, private val sessionManager: SessionManager) {

    private fun getFavoritesKey(userEmail: String) = stringSetPreferencesKey("favorites_${userEmail}")

    val favoriteProductIds: Flow<Set<Long>> = sessionManager.loggedInUserEmailFlow
        .distinctUntilChanged()
        .flatMapLatest { userEmail ->
            if (userEmail.isNullOrBlank()) {
                flowOf(emptySet())
            } else {
                val userFavoritesKey = getFavoritesKey(userEmail)
                context.dataStore.data.map {
                    it[userFavoritesKey]?.mapNotNull { id -> id.toLongOrNull() }?.toSet() ?: emptySet()
                }
            }
        }

    suspend fun addFavorite(productId: Long) {
        val userEmail = sessionManager.loggedInUserEmailFlow.first()
        if (userEmail.isNullOrBlank()) return

        val userFavoritesKey = getFavoritesKey(userEmail)
        context.dataStore.edit { preferences ->
            val currentFavorites = preferences[userFavoritesKey] ?: emptySet()
            preferences[userFavoritesKey] = currentFavorites + productId.toString()
        }
    }

    suspend fun removeFavorite(productId: Long) {
        val userEmail = sessionManager.loggedInUserEmailFlow.first()
        if (userEmail.isNullOrBlank()) return

        val userFavoritesKey = getFavoritesKey(userEmail)
        context.dataStore.edit { preferences ->
            val currentFavorites = preferences[userFavoritesKey] ?: emptySet()
            preferences[userFavoritesKey] = currentFavorites - productId.toString()
        }
    }
}