package com.lukeneedham.lyrictutor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukeneedham.lyrictutor.data.track.TrackInfo
import com.lukeneedham.lyrictutor.generated.resources.Res
import com.lukeneedham.lyrictutor.generated.resources.home_hero_subtitle
import com.lukeneedham.lyrictutor.generated.resources.home_hero_title
import com.lukeneedham.lyrictutor.generated.resources.home_how_1_body
import com.lukeneedham.lyrictutor.generated.resources.home_how_1_title
import com.lukeneedham.lyrictutor.generated.resources.home_how_2_body
import com.lukeneedham.lyrictutor.generated.resources.home_how_2_title
import com.lukeneedham.lyrictutor.generated.resources.home_how_3_body
import com.lukeneedham.lyrictutor.generated.resources.home_how_3_title
import com.lukeneedham.lyrictutor.generated.resources.home_how_title
import com.lukeneedham.lyrictutor.generated.resources.home_invalid_link
import com.lukeneedham.lyrictutor.generated.resources.home_languages_title
import com.lukeneedham.lyrictutor.generated.resources.home_artist_hint
import com.lukeneedham.lyrictutor.generated.resources.home_link_hint
import com.lukeneedham.lyrictutor.generated.resources.home_missing_name
import com.lukeneedham.lyrictutor.generated.resources.home_mode_link
import com.lukeneedham.lyrictutor.generated.resources.home_mode_name
import com.lukeneedham.lyrictutor.generated.resources.home_song_hint
import com.lukeneedham.lyrictutor.generated.resources.home_open_song
import com.lukeneedham.lyrictutor.generated.resources.home_recents_title
import com.lukeneedham.lyrictutor.generated.resources.home_title
import com.lukeneedham.lyrictutor.generated.resources.song_cover_description
import com.lukeneedham.lyrictutor.ui.components.AppButton
import com.lukeneedham.lyrictutor.ui.components.AppButtonStyle
import com.lukeneedham.lyrictutor.ui.components.AppIcon
import com.lukeneedham.lyrictutor.ui.components.AppIconButton
import com.lukeneedham.lyrictutor.ui.components.AppIconImage
import com.lukeneedham.lyrictutor.ui.components.AppToolbar
import com.lukeneedham.lyrictutor.ui.components.SettingsAction
import com.lukeneedham.lyrictutor.ui.components.navigationBarHeight
import com.lukeneedham.lyrictutor.ui.components.sideInsetsPadding
import com.lukeneedham.lyrictutor.ui.components.AppText
import com.lukeneedham.lyrictutor.ui.components.AppTextField
import com.lukeneedham.lyrictutor.ui.components.CoverArt
import com.lukeneedham.lyrictutor.ui.components.LanguageTag
import com.lukeneedham.lyrictutor.ui.theme.AppTheme
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** All of the page's UI. `internal`: the screenshots of the app use it too. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HomePageContent(
    state: HomeUiState,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onLanguageClick: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSong: (trackId: String) -> Unit,
    onModeChange: (LookupMode) -> Unit = {},
    onSongNameChange: (String) -> Unit = {},
    onArtistNameChange: (String) -> Unit = {},
) {
    val theme = AppTheme.current
    Column(modifier = Modifier.fillMaxSize().background(theme.background).sideInsetsPadding()) {
        AppToolbar(title = stringResource(Res.string.home_title), actions = { SettingsAction(onClick = onOpenSettings) })
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp + navigationBarHeight()),
        ) {
            item {
                Hero(
                    state = state,
                    onInputChange = onInputChange,
                    onSongNameChange = onSongNameChange,
                    onArtistNameChange = onArtistNameChange,
                    onModeChange = onModeChange,
                    onSubmit = onSubmit,
                )
            }
            if (state.recents.isEmpty()) {
                item { HowItWorks() }
            } else {
                if (state.languages.isNotEmpty()) {
                    item {
                        AppText(
                            text = stringResource(Res.string.home_languages_title),
                            size = 20.sp,
                            weight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 32.dp),
                        )
                    }
                    item {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (language in state.languages) {
                                LanguageTag(
                                    language = language,
                                    isSelected = language == state.selectedLanguage,
                                    onClick = { onLanguageClick(language) },
                                )
                            }
                        }
                    }
                }
                item {
                    AppText(
                        text = stringResource(Res.string.home_recents_title),
                        size = 20.sp,
                        weight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 32.dp, bottom = 8.dp),
                    )
                }
                items(state.shownSongs, key = { it.trackId }) { track ->
                    RecentSongRow(track = track, onClick = { onOpenSong(track.trackId) })
                }
            }
        }
    }
}

/** The first thing to see: what the app does, and the ways to look a song up: a link, or the song name and artist. */
@Composable
private fun Hero(
    state: HomeUiState,
    onInputChange: (String) -> Unit,
    onSongNameChange: (String) -> Unit,
    onArtistNameChange: (String) -> Unit,
    onModeChange: (LookupMode) -> Unit,
    onSubmit: () -> Unit,
) {
    val theme = AppTheme.current
    val submitButton: @Composable () -> Unit = {
        AppIconButton(
            icon = AppIcon.Arrow,
            contentDescription = stringResource(Res.string.home_open_song),
            onClick = onSubmit,
            tint = theme.onPrimary,
            background = theme.primary,
        )
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(theme.gradientStart, theme.gradientEnd)))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppText(
            text = stringResource(Res.string.home_hero_title),
            color = Color.White,
            size = 28.sp,
            lineHeight = 34.sp,
            weight = FontWeight.Bold,
        )
        AppText(
            text = stringResource(Res.string.home_hero_subtitle),
            color = Color.White.copy(alpha = 0.85f),
            size = 15.sp,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppButton(
                text = stringResource(Res.string.home_mode_link),
                onClick = { onModeChange(LookupMode.Link) },
                style = if (state.mode == LookupMode.Link) AppButtonStyle.Primary else AppButtonStyle.Tonal,
            )
            AppButton(
                text = stringResource(Res.string.home_mode_name),
                onClick = { onModeChange(LookupMode.Name) },
                style = if (state.mode == LookupMode.Name) AppButtonStyle.Primary else AppButtonStyle.Tonal,
            )
        }
        when (state.mode) {
            LookupMode.Link -> {
                AppTextField(
                    value = state.input,
                    onValueChange = onInputChange,
                    placeholder = stringResource(Res.string.home_link_hint),
                    isError = state.showInvalidLink,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { onSubmit() }),
                    trailing = submitButton,
                )
                if (state.showInvalidLink) HeroError(stringResource(Res.string.home_invalid_link))
            }

            LookupMode.Name -> {
                AppTextField(
                    value = state.songName,
                    onValueChange = onSongNameChange,
                    placeholder = stringResource(Res.string.home_song_hint),
                    isError = state.showMissingName && state.songName.isBlank(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )
                AppTextField(
                    value = state.artistName,
                    onValueChange = onArtistNameChange,
                    placeholder = stringResource(Res.string.home_artist_hint),
                    isError = state.showMissingName && state.artistName.isBlank(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { onSubmit() }),
                    trailing = submitButton,
                )
                if (state.showMissingName) HeroError(stringResource(Res.string.home_missing_name))
            }
        }
    }
}

@Composable
private fun HeroError(text: String) {
    AppText(text = text, color = Color.White, size = 14.sp, weight = FontWeight.Medium)
}

@Composable
private fun RecentSongRow(track: TrackInfo, onClick: () -> Unit) {
    val theme = AppTheme.current
    Row(
        modifier = Modifier
            .padding(vertical = 6.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(theme.surface)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        CoverArt(
            trackId = track.trackId,
            coverUrl = track.coverUrl,
            contentDescription = stringResource(Res.string.song_cover_description),
            size = 60.dp,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            AppText(
                text = track.title,
                size = 16.sp,
                weight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            AppText(
                text = track.artists.joinToString(", "),
                color = theme.onSurfaceVariant,
                size = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        AppIconImage(icon = AppIcon.Chevron, tint = theme.onSurfaceVariant, iconSize = 20.dp)
    }
}

/** What the page shows until the first song has been opened. */
@Composable
private fun HowItWorks() {
    val theme = AppTheme.current
    Column(modifier = Modifier.padding(top = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        AppText(text = stringResource(Res.string.home_how_title), size = 20.sp, weight = FontWeight.Bold)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(theme.surface)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Step(1, stringResource(Res.string.home_how_1_title), stringResource(Res.string.home_how_1_body))
            Step(2, stringResource(Res.string.home_how_2_title), stringResource(Res.string.home_how_2_body))
            Step(3, stringResource(Res.string.home_how_3_title), stringResource(Res.string.home_how_3_body))
        }
    }
}

@Composable
private fun Step(number: Int, title: String, body: String) {
    val theme = AppTheme.current
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(theme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            AppText(text = number.toString(), color = theme.onPrimaryContainer, size = 16.sp, weight = FontWeight.Bold)
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            AppText(text = title, size = 16.sp, weight = FontWeight.SemiBold)
            AppText(text = body, color = theme.onSurfaceVariant, size = 14.sp)
        }
    }
}

/** For Android Studio while building the page. */
@Preview
@Composable
private fun Preview() {
    AppTheme {
        HomePageContent(state = HomeUiState(), onInputChange = {}, onSubmit = {}, onLanguageClick = {}, onOpenSettings = {}, onOpenSong = {})
    }
}

@Preview
@Composable
private fun PreviewInvalidLink() {
    AppTheme {
        HomePageContent(
            state = HomeUiState(input = "not a link", showInvalidLink = true),
            onInputChange = {},
            onSubmit = {},
            onLanguageClick = {},
            onOpenSettings = {},
            onOpenSong = {},
        )
    }
}

@Preview
@Composable
private fun PreviewNameMode() {
    AppTheme {
        HomePageContent(
            state = HomeUiState(mode = LookupMode.Name, songName = "Si Me Voy"),
            onInputChange = {},
            onSubmit = {},
            onLanguageClick = {},
            onOpenSettings = {},
            onOpenSong = {},
        )
    }
}

@Preview
@Composable
private fun PreviewRecents() {
    AppTheme {
        HomePageContent(
            state = HomeUiState(
                recents = listOf(
                    TrackInfo(
                        trackId = "6GiCszfL4D2GlCU8tFU3sR",
                        title = "Si Me Voy (with The Marías)",
                        searchTitle = "Si Me Voy",
                        artists = listOf("Cuco", "The Marías"),
                        coverUrl = null,
                        durationSeconds = 203,
                        releaseDate = "2023-03-10",
                    ),
                ),
            ),
            onInputChange = {},
            onSubmit = {},
            onLanguageClick = {},
            onOpenSettings = {},
            onOpenSong = {},
        )
    }
}
