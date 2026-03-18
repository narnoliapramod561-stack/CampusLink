package com.campuslink.data.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("user_session")

@Singleton
class SessionManager @Inject constructor(@ApplicationContext private val context: Context) {
    private val KEY_USER_ID = stringPreferencesKey("user_id")
    private val KEY_USERNAME = stringPreferencesKey("username")

    suspend fun saveUser(userId: String, username: String) {
        context.dataStore.edit {
            it[KEY_USER_ID] = userId
            it[KEY_USERNAME] = username
        }
    }

    val currentUser: Flow<Pair<String, String>?> = context.dataStore.data.map { prefs ->
        val id = prefs[KEY_USER_ID]
        val name = prefs[KEY_USERNAME]
        if (id != null && name != null) Pair(id, name) else null
    }

    suspend fun isLoggedIn(): Boolean =
        context.dataStore.data.first()[KEY_USER_ID] != null

    suspend fun clearUser() = context.dataStore.edit { it.clear() }

    suspend fun getUserId(): String? =
        context.dataStore.data.first()[KEY_USER_ID]

    suspend fun getUsername(): String? =
        context.dataStore.data.first()[KEY_USERNAME]
}
