package com.lukeneedham.lyrictutor.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Adds the annotations. The songs and lyrics of version 1 stay. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `annotation` (" +
                "`trackId` TEXT NOT NULL, `lineIndex` INTEGER NOT NULL, `json` TEXT NOT NULL, " +
                "`provider` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`trackId`, `lineIndex`))",
        )
    }
}

/** Adds the cache of Wiktionary answers. Everything else stays. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `wiktionary_page` (" +
                "`title` TEXT NOT NULL, `body` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`title`))",
        )
    }
}
