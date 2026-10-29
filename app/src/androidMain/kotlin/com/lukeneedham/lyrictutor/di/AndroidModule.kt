package com.lukeneedham.lyrictutor.di

import androidx.room.Room
import com.lukeneedham.lyrictutor.data.AppDatabase
import com.lukeneedham.lyrictutor.data.RoomSongCache
import com.lukeneedham.lyrictutor.data.song.SongCache
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/** What `appModule` needs from the platform: the storage. */
val androidModule = module {
    single { Room.databaseBuilder(androidContext(), AppDatabase::class.java, "lyrictutor.db").build() }
    single<SongCache> { RoomSongCache(dao = get<AppDatabase>().songDao()) }
}
