package br.com.seunome.mobulite.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "session")

class SessionStore(private val context: Context) {

    companion object {
        private val KEY_TOKEN = stringPreferencesKey("token")
        private val KEY_ROLE = stringPreferencesKey("role")
        private val KEY_USER_ID = stringPreferencesKey("userId")
        private val KEY_PENDING_RIDE_ID = stringPreferencesKey("pendingRideId")
        private val KEY_PROFILE_PHOTO_PATH = stringPreferencesKey("profilePhotoPath")
    }

    val tokenFlow: Flow<String?> = context.dataStore.data.map { it[KEY_TOKEN] }
    val roleFlow: Flow<String?> = context.dataStore.data.map { it[KEY_ROLE] }
    val userIdFlow: Flow<String?> = context.dataStore.data.map { it[KEY_USER_ID] }

    // Persists the active ride ID across crashes so it can be canceled on next launch
    val pendingRideIdFlow: Flow<String?> = context.dataStore.data.map { it[KEY_PENDING_RIDE_ID] }

    val profilePhotoPathFlow: Flow<String?> = context.dataStore.data.map { it[KEY_PROFILE_PHOTO_PATH] }

    suspend fun saveSession(token: String, role: String, userId: String? = null) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TOKEN] = token
            prefs[KEY_ROLE] = role
            if (userId != null) prefs[KEY_USER_ID] = userId
        }
    }

    suspend fun savePendingRideId(id: String?) {
        context.dataStore.edit { prefs ->
            if (id != null) prefs[KEY_PENDING_RIDE_ID] = id
            else prefs.remove(KEY_PENDING_RIDE_ID)
        }
    }

    suspend fun saveProfilePhotoPath(path: String?) {
        context.dataStore.edit { prefs ->
            if (path != null) prefs[KEY_PROFILE_PHOTO_PATH] = path
            else prefs.remove(KEY_PROFILE_PHOTO_PATH)
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}