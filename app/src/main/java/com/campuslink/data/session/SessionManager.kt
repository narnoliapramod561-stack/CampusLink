package com.campuslink.data.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.campuslink.domain.model.AppSettings
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("user_session")

@Singleton
class SessionManager @Inject constructor(@ApplicationContext private val context: Context) {
    private val KEY_USER_ID   = stringPreferencesKey("user_id")
    private val KEY_USERNAME  = stringPreferencesKey("username")
    private val KEY_SETTINGS  = stringPreferencesKey("settings_json")

    suspend fun saveUser(userId: String, username: String) =
        context.dataStore.edit { it[KEY_USER_ID] = userId; it[KEY_USERNAME] = username }

    val currentUser: Flow<Pair<String,String>?> = context.dataStore.data.map { prefs ->
        val id = prefs[KEY_USER_ID]; val n = prefs[KEY_USERNAME]
        if (id != null && n != null) Pair(id, n) else null
    }

    suspend fun isLoggedIn() = context.dataStore.data.first()[KEY_USER_ID] != null
    suspend fun getUserId()   = context.dataStore.data.first()[KEY_USER_ID]
    suspend fun getUsername() = context.dataStore.data.first()[KEY_USERNAME]
    suspend fun clearUser()   = context.dataStore.edit { it.clear() }

    suspend fun saveSettings(settings: AppSettings) =
        context.dataStore.edit { it[KEY_SETTINGS] = Gson().toJson(settings) }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        val json = prefs[KEY_SETTINGS]
        if (json != null) Gson().fromJson(json, AppSettings::class.java) else AppSettings()
    }
}
