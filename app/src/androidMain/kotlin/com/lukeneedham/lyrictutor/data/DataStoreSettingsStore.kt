package com.lukeneedham.lyrictutor.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lukeneedham.common.logger.Logger
import com.lukeneedham.lyrictutor.data.settings.AiSettings
import com.lukeneedham.lyrictutor.data.settings.SettingsStore
import kotlinx.coroutines.flow.first

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Settings in DataStore. The API key is encrypted with [KeystoreCipher]; the model name is not secret. */
class DataStoreSettingsStore(
    context: Context,
    private val cipher: KeystoreCipher,
) : SettingsStore {

    private val dataStore = context.applicationContext.settingsDataStore

    override suspend fun load(): AiSettings {
        val preferences = dataStore.data.first()
        val apiKey = preferences[API_KEY]?.let { encrypted ->
            try {
                cipher.decrypt(encrypted)
            } catch (e: Exception) {
                // The Keystore key can be lost (a restored backup, a changed lock screen): the user enters the key again
                Logger.e("The stored API key could not be decrypted", e)
                ""
            }
        }.orEmpty()
        return AiSettings(apiKey = apiKey, model = preferences[MODEL] ?: AiSettings.DEFAULT_MODEL)
    }

    override suspend fun save(settings: AiSettings) {
        dataStore.edit { preferences ->
            if (settings.apiKey.isEmpty()) preferences.remove(API_KEY) else preferences[API_KEY] = cipher.encrypt(settings.apiKey)
            preferences[MODEL] = settings.model
        }
    }

    private companion object {
        val API_KEY = stringPreferencesKey("gemini_api_key_encrypted")
        val MODEL = stringPreferencesKey("gemini_model")
    }
}
