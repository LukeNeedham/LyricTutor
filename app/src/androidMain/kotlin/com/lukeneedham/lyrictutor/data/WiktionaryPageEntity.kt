package com.lukeneedham.lyrictutor.data

import androidx.room.Entity

@Entity(tableName = "wiktionary_page", primaryKeys = ["title"])
data class WiktionaryPageEntity(
    val title: String,
    /** The raw answer of the Wiktionary API; empty when there is no such page. */
    val body: String,
    val createdAt: Long,
)
