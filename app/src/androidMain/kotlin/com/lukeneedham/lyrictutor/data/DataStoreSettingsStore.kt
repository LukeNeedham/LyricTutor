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
            geminiApiKey = decrypt(preferences[GEMINI_API_KEY]),
            geminiModel = preferences[GEMINI_MODEL] ?: AiProvider.Gemini.defaultModel,
            deepSeekApiKey = decrypt(preferences[DEEPSEEK_API_KEY]),
            deepSeekModel = preferences[DEEPSEEK_MODEL] ?: AiProvider.DeepSeek.defaultModel,
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
            if (settings.geminiApiKey.isEmpty()) preferences.remove(GEMINI_API_KEY) else preferences[GEMINI_API_KEY] = cipher.encrypt(settings.geminiApiKey)
            preferences[GEMINI_MODEL] = settings.geminiModel
            if (settings.deepSeekApiKey.isEmpty()) preferences.remove(DEEPSEEK_API_KEY) else preferences[DEEPSEEK_API_KEY] = cipher.encrypt(settings.deepSeekApiKey)
            preferences[DEEPSEEK_MODEL] = settings.deepSeekModel
        }
    }

    private companion object {
        val PROVIDER = stringPreferencesKey("ai_provider")
        // The names of the Gemini keys are from before there was another provider: they stay, so that saved settings still read
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key_encrypted")
        val GEMINI_MODEL = stringPreferencesKey("gemini_model")
        val DEEPSEEK_API_KEY = stringPreferencesKey("deepseek_api_key_encrypted")
        val DEEPSEEK_MODEL = stringPreferencesKey("deepseek_model")
    }
}
