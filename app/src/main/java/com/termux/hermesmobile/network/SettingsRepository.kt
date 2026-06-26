package com.termux.hermesmobile.network

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hermes_settings")

class SettingsRepository(private val context: Context) {

    companion object {
        val KEY_SERVER_URL = stringPreferencesKey("server_url")
        val KEY_API_KEY = stringPreferencesKey("api_key")
        val KEY_DEFAULT_MODEL = stringPreferencesKey("default_model")
        val KEY_PROVIDER = stringPreferencesKey("provider")
        val KEY_SMART_ROUTING = booleanPreferencesKey("smart_routing")
        val KEY_CHEAP_MODEL = stringPreferencesKey("cheap_model")

        /** Default Ollama URL — used as the sentinel value to detect unconfigured state. */
        const val DEFAULT_URL = "http://127.0.0.1:11434"

        /** Actual Hermes gateway URL (Termux local). Surfaced in Setup Wizard / Settings placeholder. */
        const val HERMES_GATEWAY_URL = "http://127.0.0.1:8642"
    }

    val serverUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_SERVER_URL] ?: DEFAULT_URL
    }

    val apiKey: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_API_KEY] ?: ""
    }

    val defaultModel: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_DEFAULT_MODEL] ?: "MiniMax-M3"
    }

    val provider: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_PROVIDER] ?: "minimax_cn"
    }

    val smartRouting: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SMART_ROUTING] ?: true
    }

    val cheapModel: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_CHEAP_MODEL] ?: "MiniMax-M3"
    }

    /**
     * Returns true when the server URL has been set to a non-default value,
     * indicating the app has been configured (either via the wizard or manually).
     */
    suspend fun isConfigured(): Boolean {
        return serverUrl.first() != DEFAULT_URL
    }

    suspend fun setServerUrl(url: String) {
        context.dataStore.edit { it[KEY_SERVER_URL] = url }
    }

    suspend fun setApiKey(key: String) {
        context.dataStore.edit { it[KEY_API_KEY] = key }
    }

    suspend fun setDefaultModel(model: String) {
        context.dataStore.edit { it[KEY_DEFAULT_MODEL] = model }
    }

    suspend fun setProvider(provider: String) {
        context.dataStore.edit { it[KEY_PROVIDER] = provider }
    }

    suspend fun setSmartRouting(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SMART_ROUTING] = enabled }
    }

    suspend fun setCheapModel(model: String) {
        context.dataStore.edit { it[KEY_CHEAP_MODEL] = model }
    }
}
