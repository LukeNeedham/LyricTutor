package com.lukeneedham.lyrictutor.di

import com.lukeneedham.lyrictutor.data.AppDatabase
import com.lukeneedham.lyrictutor.data.CoilArtColorExtractor
import com.lukeneedham.lyrictutor.data.art.ArtColorExtractor
import com.lukeneedham.lyrictutor.data.DataStoreSettingsStore
import com.lukeneedham.lyrictutor.data.KeystoreCipher
import com.lukeneedham.lyrictutor.data.RoomWiktionaryPageCache
import com.lukeneedham.lyrictutor.data.RoomSongCache
import com.lukeneedham.lyrictutor.data.settings.SettingsStore
import com.lukeneedham.lyrictutor.data.song.SongCache
import com.lukeneedham.lyrictutor.data.wiktionary.WiktionaryPageCache
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/** What `KoinModules` needs from the platform: the HTTP client, the storage, the encryption of the API key and reading the pixels of a cover. */
object AndroidModule {
    val bindings = module {
        single { HttpClientFactory.create() }
        single { DatabaseFactory.create(androidContext()) }
        single { KeystoreCipher() }
        single<SettingsStore> { DataStoreSettingsStore(context = androidContext(), cipher = get()) }
        single<ArtColorExtractor> { CoilArtColorExtractor(context = androidContext()) }
        single<WiktionaryPageCache> { RoomWiktionaryPageCache(dao = get<AppDatabase>().wiktionaryPageDao()) }
        single<SongCache> { RoomSongCache(dao = get<AppDatabase>().songDao()) }
    }
}
