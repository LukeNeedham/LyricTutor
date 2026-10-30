package com.lukeneedham.lyrictutor.di

import androidx.room.Room
import com.lukeneedham.common.network.NetworkLogInterceptor
import com.lukeneedham.lyrictutor.data.AppDatabase
import com.lukeneedham.lyrictutor.data.DataStoreSettingsStore
import com.lukeneedham.lyrictutor.data.KeystoreCipher
import com.lukeneedham.lyrictutor.data.MIGRATION_1_2
import com.lukeneedham.lyrictutor.data.RoomSongCache
import com.lukeneedham.lyrictutor.data.settings.SettingsStore
import com.lukeneedham.lyrictutor.data.song.SongCache
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/** What `appModule` needs from the platform: the HTTP client, the storage and the encryption of the API key. */
val androidModule = module {
    // The one HTTP client of the app: Ktor on OkHttp, with every call recorded for the debug kit network page
    single {
        HttpClient(OkHttp) {
            // The default of the OkHttp engine is ten seconds for each of these, too short for Gemini; its call sets its own limits
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 30_000
            }
            engine { addInterceptor(NetworkLogInterceptor()) }
        }
    }
    single { Room.databaseBuilder(androidContext(), AppDatabase::class.java, "lyrictutor.db")
            .addMigrations(MIGRATION_1_2)
            .build() }
    single { KeystoreCipher() }
    single<SettingsStore> { DataStoreSettingsStore(context = androidContext(), cipher = get()) }
    single<SongCache> { RoomSongCache(dao = get<AppDatabase>().songDao()) }
}
