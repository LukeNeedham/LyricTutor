package com.lukeneedham.lyrictutor.data.wiktionary

import kotlinx.serialization.Serializable

/** One part of speech of one language on a Wiktionary page: [definitions] are HTML. Every field is optional, as the API can leave any out. */
@Serializable
data class WiktionaryEntryDto(
    val partOfSpeech: String? = null,
    val language: String? = null,
    val definitions: List<WiktionaryDefinitionDto>? = null,
)

@Serializable
data class WiktionaryDefinitionDto(val definition: String? = null)
