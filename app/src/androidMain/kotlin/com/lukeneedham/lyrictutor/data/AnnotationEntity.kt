package com.lukeneedham.lyrictutor.data

import androidx.room.Entity

@Entity(tableName = "annotation", primaryKeys = ["trackId", "lineIndex"])
data class AnnotationEntity(
    val trackId: String,
    val lineIndex: Int,
    /** The serialized `LineAnnotation`. */
    val json: String,
    /** Where it came from, for example "gemini". */
    val provider: String,
    val createdAt: Long,
)
