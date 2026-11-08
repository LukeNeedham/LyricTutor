package com.lukeneedham.lyrictutor.data.annotation

import com.lukeneedham.lyrictutor.data.settings.AiProvider
import com.lukeneedham.lyrictutor.data.settings.SettingsStore
import com.lukeneedham.lyrictutor.data.track.TrackInfo

/** Annotates with the provider chosen in Settings, read on each request so that a change applies at once. */
class ProviderAnnotator(
    private val settingsStore: SettingsStore,
    private val annotators: Map<AiProvider, LyricsAnnotator>,
) : LyricsAnnotator {

    /** The provider of the latest request: the repository reads it after the answer, to store where the translations came from. */
    override var providerName: String = annotators.getValue(AiProvider.Gemini).providerName
        private set

    override suspend fun annotate(song: TrackInfo, lines: List<AnnotationLine>): Result<List<LineAnnotation>> {
        val annotator = annotators.getValue(settingsStore.load().provider)
        providerName = annotator.providerName
        return annotator.annotate(song, lines)
    }
}

/** Chooses a part of speech with the provider chosen in Settings. */
class ProviderPartOfSpeechChooser(
    private val settingsStore: SettingsStore,
    private val choosers: Map<AiProvider, PartOfSpeechChooser>,
) : PartOfSpeechChooser {

    override suspend fun choose(line: String, word: String, language: String?, options: List<String>): Result<String?> =
        choosers.getValue(settingsStore.load().provider).choose(line, word, language, options)
}
