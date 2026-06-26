package com.termux.hermesmobile.network

import android.content.Context
import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Imported configuration from bundled assets or device file.
 *
 * @property serverUrl The Hermes API server URL
 * @property apiKey The API key (may be empty)
 * @property defaultModel The default model to use
 * @property provider The provider name
 * @property smartRouting Whether smart routing is enabled
 * @property cheapModel The cheap/fallback model
 */
@Serializable
data class HermesConfig(
    val server_url: String = "http://127.0.0.1:8642",
    val api_key: String = "VI7IxKay3liyWc76H7g43iFT8xWvg2MZD2S49-bp8uQ",
    val default_model: String = "MiniMax-M3",
    val provider: String = "minimax_cn",
    val smart_routing: Boolean = true,
    val cheap_model: String = "MiniMax-M3"
)

/**
 * Result of a config import operation indicating which source was used.
 */
sealed class ConfigSource {
    /** Config was loaded from the bundled assets file. */
    data object Assets : ConfigSource()

    /** Config was loaded from a device file. */
    data class DeviceFile(val path: String) : ConfigSource()

    /** No config file was found. */
    data object None : ConfigSource()
}

/**
 * Reads Hermes configuration from bundled assets or a device file and applies
 * it to [SettingsRepository].
 *
 * Sources are checked in order; the first found wins:
 * 1. `/sdcard/hermes_config.json` (device file — user-placed)
 * 2. `hermes_defaults.json` from the app assets bundle
 *
 * @param context Android context used to read assets and DataStore
 * @return Pair of the loaded config and the source it came from, or null if no config found
 */
fun importHermesConfig(context: Context): Pair<HermesConfig, ConfigSource>? {
    val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    // 1. Try device file first (user-placed config takes precedence)
    val deviceFile = File("/sdcard/hermes_config.json")
    if (deviceFile.exists() && deviceFile.isFile) {
        return try {
            val text = deviceFile.readText()
            val config = json.decodeFromString<HermesConfig>(text)
            Log.i("ConfigImporter", "Loaded config from device file: ${deviceFile.path}")
            Pair(config, ConfigSource.DeviceFile(deviceFile.path))
        } catch (e: Exception) {
            Log.w("ConfigImporter", "Failed to parse device config: ${e.message}")
            null
        }
    }

    // 2. Fall back to bundled assets
    return try {
        val text = context.assets.open("hermes_defaults.json").bufferedReader().use { it.readText() }
        val config = json.decodeFromString<HermesConfig>(text)
        Log.i("ConfigImporter", "Loaded config from bundled assets")
        Pair(config, ConfigSource.Assets)
    } catch (e: Exception) {
        Log.w("ConfigImporter", "Failed to read bundled config: ${e.message}")
        null
    }
}

/**
 * Applies the given [config] to [SettingsRepository], persisting all fields.
 *
 * @param settingsRepo The repository to update
 * @param config The configuration values to apply
 */
suspend fun applyHermesConfig(settingsRepo: SettingsRepository, config: HermesConfig) {
    settingsRepo.setServerUrl(config.server_url)
    settingsRepo.setApiKey(config.api_key)
    config.default_model.let { settingsRepo.setDefaultModel(it) }
    config.provider.let { settingsRepo.setProvider(it) }
    config.smart_routing.let { settingsRepo.setSmartRouting(it) }
    config.cheap_model.let { settingsRepo.setCheapModel(it) }
}