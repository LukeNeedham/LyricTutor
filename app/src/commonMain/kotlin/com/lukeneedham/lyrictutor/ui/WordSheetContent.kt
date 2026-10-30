package com.lukeneedham.lyrictutor.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukeneedham.lyrictutor.data.annotation.WordAnnotation
import com.lukeneedham.lyrictutor.generated.resources.Res
import com.lukeneedham.lyrictutor.generated.resources.word_ai_note
import com.lukeneedham.lyrictutor.generated.resources.word_cognates
import com.lukeneedham.lyrictutor.generated.resources.word_wiktionary_error
import com.lukeneedham.lyrictutor.generated.resources.word_wiktionary_loading
import com.lukeneedham.lyrictutor.generated.resources.word_wiktionary_not_found
import com.lukeneedham.lyrictutor.generated.resources.word_wiktionary_open
import com.lukeneedham.lyrictutor.generated.resources.word_wiktionary_title
import com.lukeneedham.lyrictutor.generated.resources.word_language
import com.lukeneedham.lyrictutor.generated.resources.word_lemma
import com.lukeneedham.lyrictutor.generated.resources.word_meaning
import com.lukeneedham.lyrictutor.generated.resources.word_morphology
import com.lukeneedham.lyrictutor.generated.resources.word_note
import com.lukeneedham.lyrictutor.ui.theme.AppTheme
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** What the sheet of a word shows. Stateless, and unaware of the sheet around it. */
@Composable
internal fun WordSheetContent(
    word: WordAnnotation,
    wiktionary: WiktionaryUiState,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = AppTheme.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BasicText(
            text = word.surface,
            style = TextStyle(
                color = theme.onSurface,
                fontFamily = theme.fontFamily,
                fontSize = 28.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        word.meaning?.let { Fact(stringResource(Res.string.word_meaning), it) }
        word.lemma?.let { lemma ->
            Fact(stringResource(Res.string.word_lemma), listOfNotNull(lemma, word.partOfSpeech).joinToString(" · "))
        }
        word.morphology?.let { Fact(stringResource(Res.string.word_morphology), it) }
        word.language?.let { Fact(stringResource(Res.string.word_language), it) }
        word.note?.let { Fact(stringResource(Res.string.word_note), it) }
        WiktionaryLink(wiktionary, onOpenUrl)
        if (word.cognates.isNotEmpty()) Fact(stringResource(Res.string.word_cognates), word.cognates.joinToString(", "))
        BasicText(
            text = stringResource(Res.string.word_ai_note),
            modifier = Modifier.padding(top = 4.dp),
            style = TextStyle(color = theme.outline, fontFamily = theme.fontFamily, fontSize = 14.sp, lineHeight = 20.sp),
        )
    }
}

/** Where the word comes from is not asked of the AI: it is a link to the Wiktionary page of the word, at its language. */
@Composable
private fun WiktionaryLink(state: WiktionaryUiState, onOpenUrl: (String) -> Unit) {
    val theme = AppTheme.current
    Column {
        BasicText(
            text = stringResource(Res.string.word_wiktionary_title),
            style = TextStyle(color = theme.outline, fontFamily = theme.fontFamily, fontSize = 13.sp, lineHeight = 18.sp),
        )
        val textStyle = TextStyle(color = theme.onSurface, fontFamily = theme.fontFamily, fontSize = 16.sp, lineHeight = 24.sp)
        val linkStyle = textStyle.copy(color = theme.primary, textDecoration = TextDecoration.Underline)
        when (state) {
            WiktionaryUiState.Loading -> BasicText(stringResource(Res.string.word_wiktionary_loading), style = textStyle.copy(color = theme.outline))
            WiktionaryUiState.Error -> BasicText(stringResource(Res.string.word_wiktionary_error), style = textStyle.copy(color = theme.error))
            is WiktionaryUiState.Found -> Link(stringResource(Res.string.word_wiktionary_open, state.title), linkStyle) { onOpenUrl(state.url) }
            is WiktionaryUiState.NotFound -> Link(stringResource(Res.string.word_wiktionary_not_found), linkStyle) { onOpenUrl(state.searchUrl) }
        }
    }
}

@Composable
private fun Link(text: String, style: TextStyle, onClick: () -> Unit) {
    Box(modifier = Modifier.heightIn(min = 48.dp).clickable(onClick = onClick), contentAlignment = Alignment.CenterStart) {
        BasicText(text = text, style = style)
    }
}

@Composable
private fun Fact(label: String, value: String) {
    val theme = AppTheme.current
    Column {
        BasicText(
            text = label,
            style = TextStyle(color = theme.outline, fontFamily = theme.fontFamily, fontSize = 13.sp, lineHeight = 18.sp),
        )
        BasicText(
            text = value,
            style = TextStyle(color = theme.onSurface, fontFamily = theme.fontFamily, fontSize = 16.sp, lineHeight = 24.sp),
        )
    }
}

/** For Android Studio while building the sheet. */
@Preview
@Composable
private fun Preview() {
    AppTheme {
        WordSheetContent(
            word = WordAnnotation(
                surface = "voy",
                language = "Spanish",
                lemma = "ir",
                partOfSpeech = "verb",
                morphology = "present indicative, 1st person singular",
                meaning = "I go",
                cognates = listOf("evade", "invade"),
            ),
            wiktionary = WiktionaryUiState.Found("voy", "https://en.wiktionary.org/wiki/voy#Spanish"),
            onOpenUrl = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
