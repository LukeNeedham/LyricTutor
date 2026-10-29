package com.lukeneedham.lyrictutor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.lukeneedham.lyrictutor.data.annotation.LineAnnotation
import com.lukeneedham.lyrictutor.data.annotation.WordAnnotation
import com.lukeneedham.lyrictutor.data.lyrics.LyricLine
import com.lukeneedham.lyrictutor.data.lyrics.Lyrics
import com.lukeneedham.lyrictutor.data.lyrics.LyricsSourceKind
import com.lukeneedham.lyrictutor.data.track.TrackInfo
import com.lukeneedham.lyrictutor.generated.resources.Res
import com.lukeneedham.lyrictutor.generated.resources.annotation_failed
import com.lukeneedham.lyrictutor.generated.resources.annotation_invalid_key
import com.lukeneedham.lyrictutor.generated.resources.annotation_loading
import com.lukeneedham.lyrictutor.generated.resources.annotation_missing_key
import com.lukeneedham.lyrictutor.generated.resources.annotation_open_settings
import com.lukeneedham.lyrictutor.generated.resources.annotation_rate_limited
import com.lukeneedham.lyrictutor.generated.resources.annotation_retry
import com.lukeneedham.lyrictutor.generated.resources.lyrics_error
import com.lukeneedham.lyrictutor.generated.resources.lyrics_loading
import com.lukeneedham.lyrictutor.generated.resources.lyrics_not_found
import com.lukeneedham.lyrictutor.generated.resources.lyrics_paste
import com.lukeneedham.lyrictutor.generated.resources.lyrics_paste_cancel
import com.lukeneedham.lyrictutor.generated.resources.lyrics_paste_hint
import com.lukeneedham.lyrictutor.generated.resources.lyrics_paste_save
import com.lukeneedham.lyrictutor.generated.resources.lyrics_pasted_note
import com.lukeneedham.lyrictutor.generated.resources.lyrics_retry
import com.lukeneedham.lyrictutor.generated.resources.song_cover_description
import com.lukeneedham.lyrictutor.generated.resources.song_error_network
import com.lukeneedham.lyrictutor.generated.resources.song_error_unreadable
import com.lukeneedham.lyrictutor.generated.resources.song_loading
import com.lukeneedham.lyrictutor.generated.resources.song_retry
import com.lukeneedham.lyrictutor.ui.theme.AppTheme
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** All of the page's UI. `internal`: the screenshots of the app use it too. */
@Composable
internal fun SongPageContent(
    state: SongUiState,
    onRetry: () -> Unit,
    onRetryLyrics: () -> Unit,
    onOpenPaste: () -> Unit,
    onPasteChange: (String) -> Unit,
    onSavePaste: () -> Unit,
    onClosePaste: () -> Unit,
    onRetryAnnotations: () -> Unit,
    onOpenSettings: () -> Unit,
    onWordClick: (WordAnnotation) -> Unit,
    onWordDismiss: () -> Unit,
) {
    val theme = AppTheme.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background)
            .safeDrawingPadding(),
    ) {
        when (state) {
            SongUiState.Loading -> Message(text = stringResource(Res.string.song_loading), modifier = Modifier.padding(16.dp))

            is SongUiState.Error -> Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Message(
                    text = stringResource(if (state.isNetwork) Res.string.song_error_network else Res.string.song_error_unreadable),
                    isError = true,
                )
                Button(text = stringResource(Res.string.song_retry), onClick = onRetry)
            }

            is SongUiState.Loaded -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { Header(state.track) }
                when (val lyrics = state.lyrics) {
                    LyricsUiState.Loading -> item { Message(text = stringResource(Res.string.lyrics_loading)) }

                    is LyricsUiState.Found -> {
                        if (lyrics.lyrics.source == LyricsSourceKind.Pasted) {
                            item { Message(text = stringResource(Res.string.lyrics_pasted_note), isSubtle = true) }
                        }
                        item {
                            AnnotationNotice(
                                status = state.annotationStatus,
                                detail = state.annotationDetail,
                                onRetry = onRetryAnnotations,
                                onOpenSettings = onOpenSettings,
                            )
                        }
                        itemsIndexed(lyrics.lyrics.lines) { index, line ->
                            LyricLineRow(line = line, annotation = state.annotations[index], onWordClick = onWordClick)
                        }
                    }

                    LyricsUiState.NotFound, LyricsUiState.Error -> item {
                        MissingLyrics(
                            isError = lyrics == LyricsUiState.Error,
                            pasteText = state.pasteText,
                            onRetryLyrics = onRetryLyrics,
                            onOpenPaste = onOpenPaste,
                            onPasteChange = onPasteChange,
                            onSavePaste = onSavePaste,
                            onClosePaste = onClosePaste,
                        )
                    }
                }
            }
        }
        if (state is SongUiState.Loaded) WordSheet(word = state.selectedWord, onDismiss = onWordDismiss)
    }
}

@Composable
private fun Header(track: TrackInfo) {
    val theme = AppTheme.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AsyncImage(
            model = track.coverUrl,
            contentDescription = stringResource(Res.string.song_cover_description),
            modifier = Modifier
                .size(160.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(theme.surface),
        )
        BasicText(
            text = track.title,
            style = TextStyle(
                color = theme.onBackground,
                fontFamily = theme.fontFamily,
                fontSize = 24.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        BasicText(
            text = track.artists.joinToString(", "),
            style = TextStyle(color = theme.onBackground, fontFamily = theme.fontFamily, fontSize = 16.sp, lineHeight = 24.sp),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LyricLineRow(line: LyricLine, annotation: LineAnnotation?, onWordClick: (WordAnnotation) -> Unit) {
    val theme = AppTheme.current
    val textStyle = TextStyle(color = theme.onBackground, fontFamily = theme.fontFamily, fontSize = 18.sp, lineHeight = 26.sp)
    Column {
        if (annotation == null || annotation.words.isEmpty()) {
            BasicText(text = line.text, style = textStyle)
        } else {
            // Each word of the line that the model explained can be tapped; the others (and any it left out) are plain text
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (token in line.text.split(' ').filter { it.isNotEmpty() }) {
                    val word = annotation.words.firstOrNull { it.surface.normalizedWord() == token.normalizedWord() }
                    Box(
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .then(if (word != null) Modifier.clickable { onWordClick(word) } else Modifier),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        BasicText(
                            text = token,
                            style = if (word != null) textStyle.copy(color = theme.primary) else textStyle,
                        )
                    }
                }
            }
        }
        annotation?.translation?.let { translation ->
            BasicText(
                text = translation,
                style = TextStyle(color = theme.outline, fontFamily = theme.fontFamily, fontSize = 15.sp, lineHeight = 22.sp),
            )
        }
    }
}

/** Compares a word of the line with a word of the annotation: case and punctuation do not count. */
private fun String.normalizedWord(): String = lowercase().filter { it.isLetterOrDigit() }

@Composable
private fun AnnotationNotice(status: AnnotationStatus, detail: String?, onRetry: () -> Unit, onOpenSettings: () -> Unit) {
    val message = when (status) {
        AnnotationStatus.Idle -> return
        AnnotationStatus.Loading -> stringResource(Res.string.annotation_loading)
        AnnotationStatus.MissingKey -> stringResource(Res.string.annotation_missing_key)
        AnnotationStatus.InvalidKey -> stringResource(Res.string.annotation_invalid_key)
        AnnotationStatus.RateLimited -> stringResource(Res.string.annotation_rate_limited)
        AnnotationStatus.Failed -> stringResource(Res.string.annotation_failed)
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Message(text = message, isError = status != AnnotationStatus.Loading, isSubtle = status == AnnotationStatus.Loading)
        if (detail != null) Message(text = detail, isSubtle = true)
        when (status) {
            AnnotationStatus.MissingKey, AnnotationStatus.InvalidKey ->
                Button(text = stringResource(Res.string.annotation_open_settings), onClick = onOpenSettings)
            AnnotationStatus.RateLimited, AnnotationStatus.Failed ->
                Button(text = stringResource(Res.string.annotation_retry), onClick = onRetry)
            else -> Unit
        }
    }
}

@Composable
private fun MissingLyrics(
    isError: Boolean,
    pasteText: String?,
    onRetryLyrics: () -> Unit,
    onOpenPaste: () -> Unit,
    onPasteChange: (String) -> Unit,
    onSavePaste: () -> Unit,
    onClosePaste: () -> Unit,
) {
    val theme = AppTheme.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Message(
            text = stringResource(if (isError) Res.string.lyrics_error else Res.string.lyrics_not_found),
            isError = isError,
        )
        if (pasteText == null) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (isError) Button(text = stringResource(Res.string.lyrics_retry), onClick = onRetryLyrics)
                Button(text = stringResource(Res.string.lyrics_paste), onClick = onOpenPaste)
            }
        } else {
            val textStyle = TextStyle(color = theme.onSurface, fontFamily = theme.fontFamily, fontSize = 16.sp, lineHeight = 24.sp)
            BasicTextField(
                value = pasteText,
                onValueChange = onPasteChange,
                textStyle = textStyle,
                cursorBrush = SolidColor(theme.primary),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 160.dp)
                            .background(theme.surface, RoundedCornerShape(12.dp))
                            .border(1.dp, theme.outline, RoundedCornerShape(12.dp))
                            .padding(16.dp),
                    ) {
                        if (pasteText.isEmpty()) {
                            BasicText(
                                text = stringResource(Res.string.lyrics_paste_hint),
                                style = textStyle.copy(color = theme.outline),
                            )
                        }
                        innerTextField()
                    }
                },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(text = stringResource(Res.string.lyrics_paste_save), onClick = onSavePaste)
                Button(text = stringResource(Res.string.lyrics_paste_cancel), onClick = onClosePaste, isPrimary = false)
            }
        }
    }
}

@Composable
private fun Message(
    text: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    isSubtle: Boolean = false,
) {
    val theme = AppTheme.current
    BasicText(
        text = text,
        modifier = modifier,
        style = TextStyle(
            color = when {
                isError -> theme.error
                isSubtle -> theme.outline
                else -> theme.onBackground
            },
            fontFamily = theme.fontFamily,
            fontSize = if (isSubtle) 14.sp else 16.sp,
            lineHeight = if (isSubtle) 20.sp else 24.sp,
        ),
    )
}

@Composable
private fun Button(text: String, onClick: () -> Unit, isPrimary: Boolean = true) {
    val theme = AppTheme.current
    Box(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(if (isPrimary) theme.primary else theme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = text,
            style = TextStyle(
                color = if (isPrimary) theme.onPrimary else theme.onSurface,
                fontFamily = theme.fontFamily,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

private val previewTrack = TrackInfo(
    trackId = "6GiCszfL4D2GlCU8tFU3sR",
    title = "Si Me Voy (with The Marías)",
    searchTitle = "Si Me Voy",
    artists = listOf("Cuco", "The Marías"),
    coverUrl = null,
    durationSeconds = 203,
    releaseDate = "2023-03-10",
)

@Composable
private fun PreviewContent(state: SongUiState) {
    AppTheme {
        SongPageContent(
            state = state,
            onRetry = {},
            onRetryLyrics = {},
            onOpenPaste = {},
            onPasteChange = {},
            onSavePaste = {},
            onClosePaste = {},
            onRetryAnnotations = {},
            onOpenSettings = {},
            onWordClick = {},
            onWordDismiss = {},
        )
    }
}

/** For Android Studio while building the page. */
@Preview
@Composable
private fun Preview() {
    val lyrics = Lyrics(
        lines = listOf(LyricLine("Si me voy, me voy contigo"), LyricLine("Y si me quedo, me quedo contigo")),
        source = LyricsSourceKind.Lrclib,
    )
    PreviewContent(SongUiState.Loaded(previewTrack, LyricsUiState.Found(lyrics)))
}

@Preview
@Composable
private fun PreviewAnnotated() {
    val lyrics = Lyrics(
        lines = listOf(LyricLine("Si me voy, me voy contigo")),
        source = LyricsSourceKind.Lrclib,
    )
    val annotation = LineAnnotation(
        index = 0,
        translation = "If I go, I go with you",
        words = listOf(WordAnnotation(surface = "voy", lemma = "ir", meaning = "I go")),
    )
    PreviewContent(SongUiState.Loaded(previewTrack, LyricsUiState.Found(lyrics), annotations = mapOf(0 to annotation)))
}

@Preview
@Composable
private fun PreviewNotFound() {
    PreviewContent(SongUiState.Loaded(previewTrack, LyricsUiState.NotFound))
}

@Preview
@Composable
private fun PreviewPasting() {
    PreviewContent(SongUiState.Loaded(previewTrack, LyricsUiState.NotFound, pasteText = ""))
}

@Preview
@Composable
private fun PreviewError() {
    PreviewContent(SongUiState.Error(isNetwork = true))
}
