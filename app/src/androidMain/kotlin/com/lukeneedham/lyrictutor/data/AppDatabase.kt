package com.lukeneedham.lyrictutor.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [SongEntity::class, LyricsEntity::class, AnnotationEntity::class, WiktionaryPageEntity::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun wiktionaryPageDao(): WiktionaryPageDao
}
