package com.lukeneedham.lyrictutor.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lukeneedham.lyrictutor.data.settings.PlayProviderStore
import com.lukeneedham.lyrictutor.data.track.PlayProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.playerDataStore: DataStore<Preferences> by preferencesDataStore(name = "player")

/** The chosen play service in DataStore. */
class DataStorePlayProviderStore(context: Context) : PlayProviderStore {

    private val dataStore = context.applicationContext.playerDataStore

    override val provider: Flow<PlayProvider> = dataStore.data.map { preferences ->
        PlayProvider.entries.firstOrNull { it.name == preferences[PROVIDER] } ?: PlayProvider.Spotify
    }

    override suspend fun set(provider: PlayProvider) {
        dataStore.edit { it[PROVIDER] = provider.name }
    }

    private companion object {
        val PROVIDER = stringPreferencesKey("play_provider")
    }
}
