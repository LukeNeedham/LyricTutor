package com.lukeneedham.lyrictutor.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import com.lukeneedham.lyrictutor.data.wiktionary.WiktionaryResult
import com.lukeneedham.lyrictutor.data.wiktionary.WiktionarySense
import com.lukeneedham.lyrictutor.generated.resources.Res
import com.lukeneedham.lyrictutor.generated.resources.word_form_of
import com.lukeneedham.lyrictutor.generated.resources.word_language
import com.lukeneedham.lyrictutor.generated.resources.word_no_senses
import com.lukeneedham.lyrictutor.generated.resources.word_source_note
import com.lukeneedham.lyrictutor.generated.resources.word_wiktionary_error
import com.lukeneedham.lyrictutor.generated.resources.word_wiktionary_loading
import com.lukeneedham.lyrictutor.generated.resources.word_wiktionary_not_found
import com.lukeneedham.lyrictutor.generated.resources.word_wiktionary_open
import com.lukeneedham.lyrictutor.generated.resources.word_wiktionary_title
import com.lukeneedham.lyrictutor.ui.theme.AppTheme
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** What the sheet of a word shows: the word, and what Wiktionary says of it. Stateless, and unaware of the sheet around it. */
@Composable
internal fun WordSheetContent(
    word: String,
    wiktionary: WiktionaryUiState,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = AppTheme.current
    val textStyle = TextStyle(color = theme.onSurface, fontFamily = theme.fontFamily, fontSize = 16.sp, lineHeight = 24.sp)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BasicText(
            text = word,
            style = TextStyle(
                color = theme.onSurface,
                fontFamily = theme.fontFamily,
                fontSize = 28.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        when (wiktionary) {
            WiktionaryUiState.Loading ->
                BasicText(stringResource(Res.string.word_wiktionary_loading), style = textStyle.copy(color = theme.outline))

            WiktionaryUiState.Error ->
                BasicText(stringResource(Res.string.word_wiktionary_error), style = textStyle.copy(color = theme.error))

            is WiktionaryUiState.NotFound -> Link(
                text = stringResource(Res.string.word_wiktionary_not_found),
                onClick = { onOpenUrl(wiktionary.searchUrl) },
            )

            is WiktionaryUiState.Found -> Found(wiktionary.result, onOpenUrl)
        }
        BasicText(
            text = stringResource(Res.string.word_source_note),
            modifier = Modifier.padding(top = 4.dp),
            style = TextStyle(color = theme.outline, fontFamily = theme.fontFamily, fontSize = 14.sp, lineHeight = 20.sp),
        )
    }
}

@Composable
private fun Found(result: WiktionaryResult, onOpenUrl: (String) -> Unit) {
    val theme = AppTheme.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Fact(label = stringResource(Res.string.word_language), value = result.language)
        if (result.senses.isEmpty()) {
            BasicText(
                text = stringResource(Res.string.word_no_senses),
                style = TextStyle(color = theme.outline, fontFamily = theme.fontFamily, fontSize = 16.sp, lineHeight = 24.sp),
            )
        }
        for (sense in result.senses) Sense(sense)
        Column {
            BasicText(
                text = stringResource(Res.string.word_wiktionary_title),
                style = TextStyle(color = theme.outline, fontFamily = theme.fontFamily, fontSize = 13.sp, lineHeight = 18.sp),
            )
            Link(text = stringResource(Res.string.word_wiktionary_open, result.title), onClick = { onOpenUrl(result.url) })
        }
    }
}

/** A part of speech: for a form of another word how it is formed, then the first meanings. */
@Composable
private fun Sense(sense: WiktionarySense) {
    val theme = AppTheme.current
    val textStyle = TextStyle(color = theme.onSurface, fontFamily = theme.fontFamily, fontSize = 16.sp, lineHeight = 24.sp)
    Column {
        BasicText(
            text = sense.partOfSpeech.lowercase(),
            style = TextStyle(color = theme.outline, fontFamily = theme.fontFamily, fontSize = 13.sp, lineHeight = 18.sp),
        )
        if (sense.lemma != null) {
            BasicText(
                text = if (sense.formText != null) stringResource(Res.string.word_form_of, sense.formText, sense.lemma) else sense.lemma,
                style = textStyle.copy(fontWeight = FontWeight.Medium),
            )
        }
        for (gloss in sense.glosses) BasicText(text = "• $gloss", style = textStyle)
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

@Composable
private fun Link(text: String, onClick: () -> Unit) {
    val theme = AppTheme.current
    Box(modifier = Modifier.heightIn(min = 48.dp).clickable(onClick = onClick), contentAlignment = Alignment.CenterStart) {
        BasicText(
            text = text,
            style = TextStyle(
                color = theme.primary,
                fontFamily = theme.fontFamily,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                textDecoration = TextDecoration.Underline,
            ),
        )
    }
}

/** For Android Studio while building the sheet. */
@Preview
@Composable
private fun Preview() {
    AppTheme {
        WordSheetContent(
            word = "voy",
            wiktionary = WiktionaryUiState.Found(
                WiktionaryResult(
                    title = "voy",
                    language = "Spanish",
                    url = "https://en.wiktionary.org/wiki/voy#Spanish",
                    senses = listOf(
                        WiktionarySense(
                            partOfSpeech = "Verb",
                            formText = "first-person singular present indicative",
                            lemma = "ir",
                            glosses = listOf("to go", "to come"),
                        ),
                    ),
                ),
            ),
            onOpenUrl = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
