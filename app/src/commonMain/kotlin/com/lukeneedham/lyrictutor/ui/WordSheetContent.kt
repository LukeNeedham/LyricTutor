package com.lukeneedham.lyrictutor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukeneedham.lyrictutor.data.annotation.WordAnnotation
import com.lukeneedham.lyrictutor.generated.resources.Res
import com.lukeneedham.lyrictutor.generated.resources.word_ai_note
import com.lukeneedham.lyrictutor.generated.resources.word_cognates
import com.lukeneedham.lyrictutor.generated.resources.word_etymology
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
internal fun WordSheetContent(word: WordAnnotation, modifier: Modifier = Modifier) {
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
        word.etymology?.let { Fact(stringResource(Res.string.word_etymology), it) }
        if (word.cognates.isNotEmpty()) Fact(stringResource(Res.string.word_cognates), word.cognates.joinToString(", "))
        BasicText(
            text = stringResource(Res.string.word_ai_note),
            modifier = Modifier.padding(top = 4.dp),
            style = TextStyle(color = theme.outline, fontFamily = theme.fontFamily, fontSize = 14.sp, lineHeight = 20.sp),
        )
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
                language = "es",
                lemma = "ir",
                partOfSpeech = "verb",
                morphology = "present indicative, 1st person singular",
                meaning = "I go",
                etymology = "From Latin vadere, to go.",
                cognates = listOf("evade", "invade"),
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}
