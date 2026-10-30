package com.lukeneedham.lyrictutor.data.wiktionary

import com.lukeneedham.common.logger.Logger
import com.lukeneedham.lyrictutor.data.annotation.WordAnnotation
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException

/**
 * Finds the page of a word on the English Wiktionary, which has a section for each language. The word as written in the lyrics is
 * preferred; the dictionary form of the model is tried only when that page has no section for the word's language.
 */
class WiktionaryClient(private val client: HttpClient) : WiktionaryLookup {

    override suspend fun find(word: WordAnnotation): Result<WiktionaryEntry?> = try {
        Result.success(lookUp(word))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Logger.e("Wiktionary lookup failed for ${word.surface}", e)
        Result.failure(e)
    }

    private suspend fun lookUp(word: WordAnnotation): WiktionaryEntry? {
        var firstExisting: WiktionaryEntry? = null
        for (title in WiktionaryProtocol.candidates(word.surface, word.lemma)) {
            val page = fetchPage(title) ?: continue
            val anchor = WiktionaryProtocol.languageAnchor(page, word.language)
            val entry = WiktionaryEntry(page.title, WiktionaryProtocol.pageUrl(page.title, anchor))
            if (anchor != null) return entry
            // A page with no section for the language (or a language the model did not name) is the fallback
            if (firstExisting == null) firstExisting = entry
        }
        return firstExisting
    }

    private suspend fun fetchPage(title: String) = client.get("https://en.wiktionary.org/w/api.php") {
        header(HttpHeaders.UserAgent, USER_AGENT)
        parameter("action", "parse")
        parameter("page", title)
        parameter("prop", "sections")
        parameter("format", "json")
        parameter("redirects", "1")
    }.let { response ->
        // A page that does not exist is an answer with status 200 and an error in the body: it parses to null
        check(response.status.isSuccess()) { "Wiktionary answered ${response.status}" }
        WiktionaryProtocol.parsePage(response.bodyAsText())
    }

    private companion object {
        const val USER_AGENT = "LyricTutor (https://github.com/LukeNeedham/LyricTutor)"
    }
}
