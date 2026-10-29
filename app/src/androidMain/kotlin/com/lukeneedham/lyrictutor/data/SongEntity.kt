package com.lukeneedham.lyrictutor.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "song")
data class SongEntity(
    @PrimaryKey val trackId: String,
    val title: String,
    val searchTitle: String,
    /** A JSON list: the order matters (the first is the main artist). */
    val artistsJson: String,
    val coverUrl: String?,
    val durationSeconds: Int?,
    val releaseDate: String?,
    val lastOpened: Long,
)
