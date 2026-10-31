package com.lukeneedham.lyrictutor.di

import android.content.Context
import androidx.room.Room
import com.lukeneedham.lyrictutor.data.AppDatabase
import com.lukeneedham.lyrictutor.data.MIGRATION_1_2
import com.lukeneedham.lyrictutor.data.MIGRATION_2_3

object DatabaseFactory {
    private const val DATABASE_NAME = "lyrictutor.db"

    fun create(context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()
}
