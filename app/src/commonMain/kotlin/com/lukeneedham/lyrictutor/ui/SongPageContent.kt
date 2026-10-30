package com.lukeneedham.lyrictutor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukeneedham.common.logger.Logger
import com.lukeneedham.lyrictutor.data.annotation.LineAnnotation
import com.lukeneedham.lyrictutor.data.lyrics.LyricLine
import com.lukeneedham.lyrictutor.data.lyrics.Lyrics
import com.lukeneedham.lyrictutor.data.lyrics.LyricsSourceKind
import com.lukeneedham.lyrictutor.data.track.SpotifyLinkParser
import com.lukeneedham.lyrictutor.data.track.TrackInfo
import com.lukeneedham.lyrictutor.data.wiktionary.WiktionaryResult
import com.lukeneedham.lyrictutor.data.wiktionary.WiktionarySense
import com.lukeneedham.lyrictutor.generated.resources.Res
import com.lukeneedham.lyrictutor.generated.resources.lyrics_loading
import com.lukeneedham.lyrictutor.generated.resources.lyrics_pasted_note
import com.lukeneedham.lyrictutor.generated.resources.song_error_network
import com.lukeneedham.lyrictutor.generated.resources.song_error_unreadable
import com.lukeneedham.lyrictutor.generated.resources.song_loading
import com.lukeneedham.lyrictutor.generated.resources.song_retry
import com.lukeneedham.lyrictutor.ui.components.AppButton
import com.lukeneedham.lyrictutor.ui.components.AppIcon
import com.lukeneedham.lyrictutor.ui.components.AppToolbar
import com.lukeneedham.lyrictutor.ui.components.navigationBarHeight
import com.lukeneedham.lyrictutor.ui.components.sideInsetsPadding
import com.lukeneedham.lyrictutor.ui.components.AppText
import com.lukeneedham.lyrictutor.ui.components.Spinner
import com.lukeneedham.lyrictutor.ui.components.trackColor
import com.lukeneedham.lyrictutor.ui.theme.AppTheme
import com.lukeneedham.lyrictutor.ui.theme.LocalAppTheme
import com.lukeneedham.lyrictutor.ui.theme.withArtColors
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** How far the list is scrolled when the cover and the title of the song are out of sight: the height of the header down to its title. */
private const val HEADER_SCROLLED_AWAY_DP = 300

/** All of the page's UI. `internal`: the screenshots of the app use it too. */
@Composable
internal fun SongPageContent(
    state: SongUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onRetryLyrics: () -> Unit,
    onOpenPaste: () -> Unit,
    onPasteChange: (String) -> Unit,
    onSavePaste: () -> Unit,
    onClosePaste: () -> Unit,
    onRetryAnnotations: () -> Unit,
    onTranslateAgain: () -> Unit,
    onOpenSettings: () -> Unit,
    onWordClick: (SelectedWord) -> Unit,
    onWordDismiss: () -> Unit,
) {
    // The page is painted with the colours of the song's cover, once they are read
    val appTheme = AppTheme.current
    val artColors = (state as? SongUiState.Loaded)?.artColors
    val theme = remember(appTheme, artColors) { if (artColors == null) appTheme else appTheme.withArtColors(artColors) }
    val uriHandler = LocalUriHandler.current
    // The sheet opens over the lower half of the page; the page height is what that is measured against
    var pageHeightPx by remember { mutableIntStateOf(0) }
    // Where the selected word is on the screen, so that the dimming of the page can leave it out
    var selectedBounds by remember { mutableStateOf<Rect?>(null) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    // Once the cover and the title have scrolled out of sight they move into the toolbar
    val isHeaderScrolledAway by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > with(density) { HEADER_SCROLLED_AWAY_DP.dp.toPx() }
        }
    }
    // Opens the song in the Spotify app, or in the browser when the app is not there: the app cannot play it by itself
    fun playOnSpotify(trackId: String) {
        try {
            uriHandler.openUri(SpotifyLinkParser.trackUrl(trackId))
        } catch (e: Exception) {
            Logger.e("Could not open the song in Spotify", e)
        }
    }
    CompositionLocalProvider(LocalAppTheme provides theme) {
    Box(modifier = Modifier.fillMaxSize().background(theme.background).onSizeChanged { pageHeightPx = it.height }) {
        // A glow of the song's own colours at the top, behind the toolbar and the cover: it reaches under the status bar
        if (state is SongUiState.Loaded) {
            val glowStart = artColors?.primary ?: trackColor(state.track.trackId)
            val glowEnd = artColors?.secondary ?: trackColor(state.track.trackId.reversed(), value = 0.6f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
                    .background(Brush.verticalGradient(listOf(glowStart.copy(alpha = 0.38f), glowEnd.copy(alpha = 0.12f), Color.Transparent))),
            )
        }
        Column(modifier = Modifier.fillMaxSize().sideInsetsPadding().imePadding()) {
            AppToolbar(
                onBack = onBack,
                center = (state as? SongUiState.Loaded)?.let { loaded ->
                    {
                        AnimatedVisibility(visible = isHeaderScrolledAway, enter = fadeIn(), exit = fadeOut()) {
                            SongToolbarTitle(track = loaded.track, onClick = { scope.launch { listState.animateScrollToItem(0) } })
                        }
                    }
                },
            )
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (state) {
                SongUiState.Loading -> CenteredMessage(text = stringResource(Res.string.song_loading), showSpinner = true)

                is SongUiState.Error -> CenteredMessage(
                    text = stringResource(if (state.isNetwork) Res.string.song_error_network else Res.string.song_error_unreadable),
                    isError = true,
                    action = { AppButton(text = stringResource(Res.string.song_retry), onClick = onRetry, icon = AppIcon.Refresh) },
                )

                is SongUiState.Loaded -> {
                    val lyricsFound = state.lyrics as? LyricsUiState.Found
                    val hasNotice = lyricsFound != null && state.annotationStatus != AnnotationStatus.Idle
                    // The sheet covers the bottom of the song: the lines get room to scroll above it, and the tapped line is brought there
                    val sheetHeight = if (state.selectedWord != null) pageHeightPx / 2 else 0
                    // The items before the first line: the header, the notice and the note of pasted lyrics (when there are any)
                    val firstLineItem = 1 + (if (hasNotice) 1 else 0) + (if (lyricsFound?.lyrics?.source == LyricsSourceKind.Pasted) 1 else 0)
                    LaunchedEffect(state.selectedWord, sheetHeight) {
                        val selected = state.selectedWord ?: return@LaunchedEffect
                        val target = firstLineItem + selected.lineIndex
                        val layout = listState.layoutInfo
                        val item = layout.visibleItemsInfo.firstOrNull { it.index == target }
                        val visibleEnd = layout.viewportEndOffset - sheetHeight
                        when {
                            item == null || item.offset < layout.viewportStartOffset -> listState.animateScrollToItem(target)
                            item.offset + item.size > visibleEnd -> listState.animateScrollBy((item.offset + item.size - visibleEnd).toFloat())
                        }
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            bottom = 32.dp + navigationBarHeight() + with(LocalDensity.current) { sheetHeight.toDp() },
                        ),
                    ) {
                        item {
                            SongHeader(
                                track = state.track,
                                canTranslateAgain = state.annotationStatus == AnnotationStatus.Idle && state.annotations.isNotEmpty(),
                                onPlay = { playOnSpotify(state.track.trackId) },
                                onTranslateAgain = onTranslateAgain,
                            )
                        }
                        if (hasNotice) {
                            item {
                                AnnotationNotice(
                                    status = state.annotationStatus,
                                    detail = state.annotationDetail,
                                    doneLines = state.annotations.size,
                                    totalLines = lyricsFound?.lyrics?.lines?.size ?: 0,
                                    onRetry = onRetryAnnotations,
                                    onOpenSettings = onOpenSettings,
                                    modifier = Modifier.padding(top = 24.dp),
                                )
                            }
                        }
                        if (lyricsFound?.lyrics?.source == LyricsSourceKind.Pasted) item { PastedNote() }
                        when (val lyrics = state.lyrics) {
                            LyricsUiState.Loading -> item {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Spinner(diameter = 20.dp)
                                    AppText(text = stringResource(Res.string.lyrics_loading), color = theme.onSurfaceVariant, size = 15.sp)
                                }
                            }

                            is LyricsUiState.Found -> itemsIndexed(lyrics.lyrics.lines) { index, line ->
                                LyricLineRow(
                                    line = line,
                                    annotation = state.annotations[index],
                                    isTranslating = state.annotationStatus == AnnotationStatus.Loading,
                                    selectedToken = state.selectedWord?.takeIf { it.lineIndex == index }?.tokenIndex,
                                    onWordClick = { word, language, token ->
                                        onWordClick(SelectedWord(word, language, lineIndex = index, tokenIndex = token))
                                    },
                                    modifier = Modifier.padding(bottom = 14.dp, top = if (index == 0) 24.dp else 0.dp),
                                    onSelectedBoundsChange = { selectedBounds = it },
                                )
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
            }
            }
        }
        if (state is SongUiState.Loaded) {
            WordScrim(isShown = state.selectedWord != null, hole = selectedBounds)
            WordSheet(
                selected = state.selectedWord,
                wiktionary = state.wiktionary,
                onOpenUrl = uriHandler::openUri,
                onDismiss = onWordDismiss,
            )
        }
    }
    }
}

/** Dims the page while the word sheet is open, except for the selected word ([hole]), which stays as it was. It takes no touches. */
@Composable
private fun WordScrim(isShown: Boolean, hole: Rect?) {
    val alpha by animateFloatAsState(targetValue = if (isShown) 0.28f else 0f, label = "scrim")
    var origin by remember { mutableStateOf(Offset.Zero) }
    if (alpha == 0f) return
    val padding = with(LocalDensity.current) { 4.dp.toPx() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { origin = it.positionInWindow() }
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawBehind {
                drawRect(Color.Black.copy(alpha = alpha))
                if (hole != null && isShown) {
                    drawRoundRect(
                        color = Color.Black,
                        topLeft = Offset(hole.left - origin.x - padding, hole.top - origin.y - padding),
                        size = Size(hole.width + 2 * padding, hole.height + 2 * padding),
                        cornerRadius = CornerRadius(padding * 2),
                        blendMode = BlendMode.Clear,
                    )
                }
            },
    )
}

@Composable
private fun PastedNote() {
    val theme = AppTheme.current
    AppText(
        text = stringResource(Res.string.lyrics_pasted_note),
        color = theme.onPrimaryContainer,
        size = 12.sp,
        weight = FontWeight.Medium,
        modifier = Modifier
            .padding(top = 24.dp, bottom = 4.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(theme.primaryContainer)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** A message in the middle of the page, with something to do about it ([action]) when it is an error. */
@Composable
private fun CenteredMessage(
    text: String,
    showSpinner: Boolean = false,
    isError: Boolean = false,
    action: (@Composable () -> Unit)? = null,
) {
    val theme = AppTheme.current
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        if (showSpinner) Spinner(diameter = 36.dp)
        AppText(
            text = text,
            color = if (isError) theme.error else theme.onSurfaceVariant,
            size = 16.sp,
            textAlign = TextAlign.Center,
        )
        action?.invoke()
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
            onBack = {},
            onRetry = {},
            onRetryLyrics = {},
            onOpenPaste = {},
            onPasteChange = {},
            onSavePaste = {},
            onClosePaste = {},
            onRetryAnnotations = {},
            onTranslateAgain = {},
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
        lines = listOf(LyricLine("Uno dos tres cuatro"), LyricLine("Cinco seis siete ocho")),
        source = LyricsSourceKind.Lrclib,
    )
    PreviewContent(SongUiState.Loaded(previewTrack, LyricsUiState.Found(lyrics)))
}

@Preview
@Composable
private fun PreviewAnnotated() {
    val lyrics = Lyrics(
        lines = listOf(LyricLine("Uno dos tres cuatro")),
        source = LyricsSourceKind.Lrclib,
    )
    val annotation = LineAnnotation(index = 0, translation = "One two three four", language = "Spanish")
    PreviewContent(SongUiState.Loaded(previewTrack, LyricsUiState.Found(lyrics), annotations = mapOf(0 to annotation)))
}

@Preview
@Composable
private fun PreviewWordSelected() {
    val lyrics = Lyrics(
        lines = listOf(LyricLine("Uno dos tres cuatro")),
        source = LyricsSourceKind.Lrclib,
    )
    val annotation = LineAnnotation(index = 0, translation = "One two three four", language = "Spanish")
    val result = WiktionaryResult(
        title = "dos",
        language = "Spanish",
        url = "https://en.wiktionary.org/wiki/dos#Spanish",
        senses = listOf(WiktionarySense(partOfSpeech = "Numeral", glosses = listOf("two"))),
    )
    PreviewContent(
        SongUiState.Loaded(
            track = previewTrack,
            lyrics = LyricsUiState.Found(lyrics),
            annotations = mapOf(0 to annotation),
            selectedWord = SelectedWord(surface = "dos", language = "Spanish", lineIndex = 0, tokenIndex = 1),
            wiktionary = WiktionaryUiState.Found(result),
        ),
    )
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
private fun PreviewLoading() {
    PreviewContent(SongUiState.Loading)
}

@Preview
@Composable
private fun PreviewError() {
    PreviewContent(SongUiState.Error(isNetwork = true))
}
