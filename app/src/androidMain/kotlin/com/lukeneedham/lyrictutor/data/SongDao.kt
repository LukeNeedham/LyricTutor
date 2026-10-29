package com.lukeneedham.lyrictutor.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM song ORDER BY lastOpened DESC")
    fun observeSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM song WHERE trackId = :trackId")
    suspend fun getSong(trackId: String): SongEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SongEntity)

    @Query("SELECT * FROM lyrics WHERE trackId = :trackId")
    suspend fun getLyrics(trackId: String): LyricsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLyrics(lyrics: LyricsEntity)
}
