package com.lukeneedham.lyrictutor.data.wiktionary

import com.lukeneedham.common.logger.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodeURLPath
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException

/**
 * Looks a word up in the English Wiktionary (its REST `page/definition` endpoint). A word that is a form of another ("voy" is a form
 * of "ir") also gets the meanings of the other, with one more request.
 */
class WiktionaryClient(private val client: HttpClient) : WiktionaryLookup {

    override suspend fun find(word: String, language: String?): Result<WiktionaryResult?> = try {
        Result.success(lookUp(word, language))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Logger.e("Wiktionary lookup failed for $word", e)
        Result.failure(e)
    }

    private suspend fun lookUp(word: String, language: String?): WiktionaryResult? {
        for (title in WiktionaryProtocol.candidates(word)) {
            val entries = WiktionaryProtocol.entriesFor(fetchEntries(title), language)
            if (entries.isEmpty()) continue

            val languageName = entries.first().language
            val senses = WiktionaryProtocol.senses(entries).map { sense -> withMeaningsOfLemma(sense, languageName) }
            return WiktionaryResult(
                title = title,
                language = languageName,
                url = WiktionaryProtocol.pageUrl(title, languageName),
                senses = senses,
            )
        }
        return null
    }

    /** A form has no meanings of its own: those of its dictionary form, of the same part of speech, are given to it. */
    private suspend fun withMeaningsOfLemma(sense: WiktionarySense, languageName: String): WiktionarySense {
        val lemma = sense.lemma ?: return sense
        if (sense.glosses.isNotEmpty()) return sense
        val entries = WiktionaryProtocol.entriesFor(fetchEntries(lemma), languageName)
        val samePartOfSpeech = entries.filter { it.partOfSpeech.equals(sense.partOfSpeech, ignoreCase = true) }
        val glosses = (samePartOfSpeech.ifEmpty { entries }).flatMap { WiktionaryProtocol.glosses(it) }
        return sense.copy(glosses = glosses.take(GLOSSES_OF_LEMMA))
    }

    /** The entries of a page; empty when there is no such page. */
    private suspend fun fetchEntries(title: String): List<WiktionaryProtocol.Entry> {
        val response = client.get("https://en.wiktionary.org/api/rest_v1/page/definition/${title.encodeURLPath()}") {
            header(HttpHeaders.UserAgent, USER_AGENT)
        }
        if (response.status == HttpStatusCode.NotFound) return emptyList()
        check(response.status.isSuccess()) { "Wiktionary answered ${response.status}" }
        return WiktionaryProtocol.parseEntries(response.bodyAsText())
    }

    private companion object {
        const val GLOSSES_OF_LEMMA = 4
        const val USER_AGENT = "LyricTutor (https://github.com/LukeNeedham/LyricTutor)"
    }
}
