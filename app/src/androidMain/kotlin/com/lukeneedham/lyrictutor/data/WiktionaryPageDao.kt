package com.lukeneedham.lyrictutor.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WiktionaryPageDao {
    @Query("SELECT * FROM wiktionary_page WHERE title = :title")
    suspend fun get(title: String): WiktionaryPageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(page: WiktionaryPageEntity)
}
