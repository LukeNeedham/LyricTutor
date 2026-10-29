package com.lukeneedham.lyrictutor.data.settings

/** The user's settings on the device. Implemented in `androidMain`, where the API key is stored encrypted. */
interface SettingsStore {
    suspend fun load(): AiSettings

    suspend fun save(settings: AiSettings)
}
