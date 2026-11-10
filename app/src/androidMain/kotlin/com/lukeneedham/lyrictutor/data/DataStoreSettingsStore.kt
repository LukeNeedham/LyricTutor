package com.lukeneedham.lyrictutor.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lukeneedham.common.logger.Logger
import com.lukeneedham.lyrictutor.data.settings.AiProvider
import com.lukeneedham.lyrictutor.data.settings.AiSettings
import com.lukeneedham.lyrictutor.data.settings.ProviderSettings
import com.lukeneedham.lyrictutor.data.settings.SettingsStore
import kotlinx.coroutines.flow.first

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Settings in DataStore. The API keys are encrypted with [KeystoreCipher]; the model name is not secret. */
class DataStoreSettingsStore(
    context: Context,
    private val cipher: KeystoreCipher,
) : SettingsStore {

    private val dataStore = context.applicationContext.settingsDataStore

    override suspend fun load(): AiSettings {
        val preferences = dataStore.data.first()
        return AiSettings(
            provider = AiProvider.entries.firstOrNull { it.name == preferences[PROVIDER] } ?: AiProvider.Gemini,
            providers = AiProvider.entries.associateWith { provider ->
                ProviderSettings(
                    apiKey = decrypt(preferences[apiKeyKey(provider)]),
                    model = preferences[modelKey(provider)] ?: provider.defaultModel,
                )
            },
        )
    }

    private fun decrypt(encrypted: String?): String = encrypted?.let {
        try {
            cipher.decrypt(it)
        } catch (e: Exception) {
            // The Keystore key can be lost (a restored backup, a changed lock screen): the user enters the key again
            Logger.e("A stored API key could not be decrypted", e)
            ""
        }
    }.orEmpty()

    override suspend fun save(settings: AiSettings) {
        dataStore.edit { preferences ->
            preferences[PROVIDER] = settings.provider.name
            for (provider in AiProvider.entries) {
                val saved = settings.of(provider)
                if (saved.apiKey.isEmpty()) preferences.remove(apiKeyKey(provider)) else preferences[apiKeyKey(provider)] = cipher.encrypt(saved.apiKey)
                preferences[modelKey(provider)] = saved.model
            }
        }
    }

    private companion object {
        val PROVIDER = stringPreferencesKey("ai_provider")

        // "gemini_api_key_encrypted" and "gemini_model" are the names from before there was another service: Gemini's saved settings still read
        fun apiKeyKey(provider: AiProvider) = stringPreferencesKey("${provider.name.lowercase()}_api_key_encrypted")

        fun modelKey(provider: AiProvider) = stringPreferencesKey("${provider.name.lowercase()}_model")
    }
}
