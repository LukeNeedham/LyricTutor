package com.lukeneedham.lyrictutor.data.annotation

/** Says which part of speech a word is used as in a line, when its Wiktionary page has several. */
interface PartOfSpeechChooser {
    /**
     * Chooses one of [options] (the parts of speech of the word's page, as Wiktionary names them) for [word] as used in [line]. Success
     * with null when the answer is none of the options. Fails with an [AnnotationException].
     */
    suspend fun choose(line: String, word: String, language: String?, options: List<String>): Result<String?>
}
