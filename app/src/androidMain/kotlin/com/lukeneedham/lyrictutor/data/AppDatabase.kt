package com.lukeneedham.lyrictutor.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [SongEntity::class, LyricsEntity::class, AnnotationEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
}
