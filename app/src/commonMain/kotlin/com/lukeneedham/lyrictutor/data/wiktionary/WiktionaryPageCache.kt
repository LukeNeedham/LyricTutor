package com.lukeneedham.lyrictutor.data.wiktionary

/**
 * Keeps the answers of the Wiktionary API for good, so that a page is asked for once, whatever the song and the session.
 * Implemented with Room in `androidMain`.
 */
interface WiktionaryPageCache {
    /** The raw answer kept for the page [title], or null when it was never asked for. An empty string means there is no such page. */
    suspend fun get(title: String): String?

    /** Keeps [body], the raw answer for the page [title] (empty when there is no such page). */
    suspend fun put(title: String, body: String)
}
