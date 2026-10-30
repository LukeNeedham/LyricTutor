package com.lukeneedham.lyrictutor.di

import androidx.room.Room
import com.lukeneedham.lyrictutor.data.AppDatabase
import com.lukeneedham.lyrictutor.data.CoilArtColorExtractor
import com.lukeneedham.lyrictutor.data.art.ArtColorExtractor
import com.lukeneedham.lyrictutor.data.DataStoreSettingsStore
import com.lukeneedham.lyrictutor.data.KeystoreCipher
import com.lukeneedham.lyrictutor.data.MIGRATION_1_2
import com.lukeneedham.lyrictutor.data.RoomSongCache
import com.lukeneedham.lyrictutor.data.settings.SettingsStore
import com.lukeneedham.lyrictutor.data.song.SongCache
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/** What `appModule` needs from the platform: the storage, the encryption of the API key and reading the pixels of a cover. */
val androidModule = module {
    single { Room.databaseBuilder(androidContext(), AppDatabase::class.java, "lyrictutor.db")
            .addMigrations(MIGRATION_1_2)
            .build() }
    single { KeystoreCipher() }
    single<SettingsStore> { DataStoreSettingsStore(context = androidContext(), cipher = get()) }
    single<ArtColorExtractor> { CoilArtColorExtractor(context = androidContext()) }
    single<SongCache> { RoomSongCache(dao = get<AppDatabase>().songDao()) }
}
