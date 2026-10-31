package com.lukeneedham.lyrictutor.ui

import com.lukeneedham.lyrictutor.data.track.TrackInfo

data class HomeUiState(
    val input: String = "",
    /** Set when the last attempt found no Spotify or YouTube song in [input]. */
    val showInvalidLink: Boolean = false,
    /** How the user is looking a song up. */
    val mode: LookupMode = LookupMode.Link,
    /** The song name and artist typed in [LookupMode.Name]. */
    val songName: String = "",
    val artistName: String = "",
    /** Set when the last attempt in [LookupMode.Name] had no song name or no artist. */
    val showMissingName: Boolean = false,
    /** The songs opened before, most recent first. */
    val recents: List<TrackInfo> = emptyList(),
    /** The languages of each song, by track id: the most common first. */
    val songLanguages: Map<String, List<String>> = emptyMap(),
    /** The language the songs are narrowed to, or null for all of them. */
    val selectedLanguage: String? = null,
) {
    /** Every language of the songs, the one that is in the most songs first. */
    val languages: List<String>
        get() = songLanguages.values.flatten().groupingBy { it }.eachCount().entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key }).map { it.key }

    /** [recents], only the songs with [selectedLanguage] when there is one. */
    val shownSongs: List<TrackInfo>
        get() = if (selectedLanguage == null) recents else recents.filter { selectedLanguage in songLanguages[it.trackId].orEmpty() }
}
