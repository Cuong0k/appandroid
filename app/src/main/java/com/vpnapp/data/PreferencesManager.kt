package com.vpnapp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vpnapp.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "vpn_prefs")

class PreferencesManager(private val context: Context) {

    companion object {
        val KEY_AUTH_DATA    = stringPreferencesKey(Constants.KEY_AUTH_DATA)
        val KEY_USER_TOKEN   = stringPreferencesKey(Constants.KEY_USER_TOKEN)
        val KEY_BASE_URL     = stringPreferencesKey(Constants.KEY_BASE_URL)
        val KEY_SELECTED_NODE = stringPreferencesKey(Constants.KEY_SELECTED_NODE)
        val KEY_USER_EMAIL   = stringPreferencesKey("user_email")
    }

    val authData:     Flow<String?> = context.dataStore.data.map { it[KEY_AUTH_DATA] }
    val userToken:    Flow<String?> = context.dataStore.data.map { it[KEY_USER_TOKEN] }
    val baseUrl:      Flow<String?> = context.dataStore.data.map { it[KEY_BASE_URL] }
    val selectedNode: Flow<String?> = context.dataStore.data.map { it[KEY_SELECTED_NODE] }
    val userEmail:    Flow<String?> = context.dataStore.data.map { it[KEY_USER_EMAIL] }

    suspend fun saveAuthData(authData: String, token: String, email: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTH_DATA]  = authData
            prefs[KEY_USER_TOKEN] = token
            prefs[KEY_USER_EMAIL] = email
        }
    }

    suspend fun saveBaseUrl(url: String) {
        context.dataStore.edit { it[KEY_BASE_URL] = url }
    }

    suspend fun saveSelectedNode(nodeJson: String) {
        context.dataStore.edit { it[KEY_SELECTED_NODE] = nodeJson }
    }

    suspend fun clearAuth() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_AUTH_DATA)
            prefs.remove(KEY_USER_TOKEN)
            prefs.remove(KEY_USER_EMAIL)
            prefs.remove(KEY_SELECTED_NODE)
        }
    }
}
