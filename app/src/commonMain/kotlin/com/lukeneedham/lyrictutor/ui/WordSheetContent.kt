package com.lukeneedham.lyrictutor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukeneedham.lyrictutor.data.wiktionary.WiktionaryResult
import com.lukeneedham.lyrictutor.data.wiktionary.WiktionarySense
import com.lukeneedham.lyrictutor.generated.resources.Res
import com.lukeneedham.lyrictutor.generated.resources.word_meanings_of
import com.lukeneedham.lyrictutor.generated.resources.word_no_senses
import com.lukeneedham.lyrictutor.generated.resources.word_open_wiktionary
import com.lukeneedham.lyrictutor.generated.resources.word_wiktionary_error
import com.lukeneedham.lyrictutor.generated.resources.word_wiktionary_not_found
import com.lukeneedham.lyrictutor.ui.components.AppIcon
import com.lukeneedham.lyrictutor.ui.components.AppIconButton
import com.lukeneedham.lyrictutor.ui.components.AppText
import com.lukeneedham.lyrictutor.ui.components.Spinner
import com.lukeneedham.lyrictutor.ui.theme.AppTheme
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * What the sheet of a word shows: the word, its language, and what Wiktionary says of it. [language] is the one the AI gave to the line,
 * shown until Wiktionary names its own. Stateless, and unaware of the sheet around it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WordSheetContent(
    word: String,
    language: String?,
    wiktionary: WiktionaryUiState,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = AppTheme.current
    val shownLanguage = (wiktionary as? WiktionaryUiState.Found)?.result?.language ?: language
    // The page of the word on Wiktionary (a search for it when there is no page); none while it is still being looked up
    val wiktionaryUrl = when (wiktionary) {
        is WiktionaryUiState.Found -> wiktionary.result.url
        is WiktionaryUiState.NotFound -> wiktionary.searchUrl
        else -> null
    }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppText(text = word, size = 34.sp, lineHeight = 40.sp, weight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (shownLanguage != null) Chip(text = shownLanguage, container = theme.primaryContainer, content = theme.onPrimaryContainer)
                    val result = (wiktionary as? WiktionaryUiState.Found)?.result
                    for (partOfSpeech in result?.senses.orEmpty().map { it.partOfSpeech.lowercase() }.distinct()) {
                        Chip(text = partOfSpeech, container = theme.surfaceVariant, content = theme.onSurfaceVariant)
                    }
                }
            }
            if (wiktionaryUrl != null) {
                AppIconButton(
                    icon = AppIcon.External,
                    contentDescription = stringResource(Res.string.word_open_wiktionary),
                    onClick = { onOpenUrl(wiktionaryUrl) },
                    tint = theme.primary,
                    background = theme.primaryContainer,
                    containerSize = 36.dp,
                    iconSize = 20.dp,
                )
            }
        }
        when (wiktionary) {
            WiktionaryUiState.Loading -> Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                Spinner(diameter = 32.dp)
            }

            WiktionaryUiState.Error ->
                AppText(text = stringResource(Res.string.word_wiktionary_error), color = theme.error, size = 15.sp)

            is WiktionaryUiState.NotFound ->
                AppText(text = stringResource(Res.string.word_wiktionary_not_found), color = theme.onSurfaceVariant, size = 15.sp)

            is WiktionaryUiState.Found -> Found(wiktionary.result)
        }
    }
}

@Composable
private fun Chip(text: String, container: Color, content: Color) {
    AppText(
        text = text,
        color = content,
        size = 13.sp,
        weight = FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(container)
            .padding(horizontal = 12.dp, vertical = 5.dp),
    )
}

@Composable
private fun Found(result: WiktionaryResult) {
    val theme = AppTheme.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (result.senses.isEmpty()) {
            AppText(text = stringResource(Res.string.word_no_senses), color = theme.onSurfaceVariant, size = 15.sp)
        }
        for (sense in result.senses) Sense(sense)
    }
}

/** A part of speech: for a word that is only a form of another, what the form is; then the meanings. */
@Composable
private fun Sense(sense: WiktionarySense) {
    val theme = AppTheme.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(theme.surfaceVariant)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val formOf = sense.formOf
        if (formOf != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(theme.primaryContainer)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                AppText(text = formOf, color = theme.onPrimaryContainer, size = 16.sp, weight = FontWeight.SemiBold)
                for (detail in sense.formDetails) {
                    AppText(text = detail, color = theme.onPrimaryContainer.copy(alpha = 0.85f), size = 14.sp)
                }
            }
        } else {
            for (detail in sense.formDetails) AppText(text = detail, color = theme.onSurface, size = 15.sp)
        }
        if (sense.lemma != null && sense.glosses.isNotEmpty()) {
            AppText(
                text = stringResource(Res.string.word_meanings_of, sense.lemma),
                color = theme.onSurfaceVariant,
                size = 13.sp,
                weight = FontWeight.Medium,
            )
        }
        for ((index, gloss) in sense.glosses.withIndex()) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.padding(top = 1.dp).size(24.dp).clip(CircleShape).background(theme.surface),
                    contentAlignment = Alignment.Center,
                ) {
                    AppText(text = (index + 1).toString(), color = theme.onSurfaceVariant, size = 12.sp, weight = FontWeight.Bold)
                }
                AppText(text = gloss, color = theme.onSurface, size = 16.sp, modifier = Modifier.weight(1f))
            }
        }
    }
}

/** For Android Studio while building the sheet. */
@Preview
@Composable
private fun Preview() {
    AppTheme {
        WordSheetContent(
            word = "voy",
            language = "Spanish",
            wiktionary = WiktionaryUiState.Found(
                WiktionaryResult(
                    title = "voy",
                    language = "Spanish",
                    url = "https://en.wiktionary.org/wiki/voy#Spanish",
                    senses = listOf(
                        WiktionarySense(
                            partOfSpeech = "Verb",
                            formOf = "first-person singular present indicative of ir",
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

@Preview
@Composable
private fun PreviewLoading() {
    AppTheme {
        WordSheetContent(
            word = "voy",
            language = "Spanish",
            wiktionary = WiktionaryUiState.Loading,
            onOpenUrl = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
