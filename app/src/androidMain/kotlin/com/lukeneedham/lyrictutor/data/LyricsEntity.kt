package com.lukeneedham.lyrictutor.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lyrics")
data class LyricsEntity(
    @PrimaryKey val trackId: String,
    /** The serialized `Lyrics`: the lines are only ever read together. */
    val json: String,
)
