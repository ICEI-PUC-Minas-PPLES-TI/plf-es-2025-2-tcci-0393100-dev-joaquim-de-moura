package br.com.seunome.mobulite.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "auth")

class TokenStore(private val context: Context) {
    private val KEY = stringPreferencesKey("access_token")

    suspend fun save(token: String) {
        context.dataStore.edit { it[KEY] = token }
    }

    suspend fun get(): String? {
        return context.dataStore.data.map { it[KEY] }.first()
    }

    suspend fun clear() {
        context.dataStore.edit { it.remove(KEY) }
    }
}