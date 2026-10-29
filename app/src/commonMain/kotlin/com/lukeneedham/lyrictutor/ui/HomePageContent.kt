package com.lukeneedham.lyrictutor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukeneedham.lyrictutor.generated.resources.Res
import com.lukeneedham.lyrictutor.generated.resources.home_invalid_link
import com.lukeneedham.lyrictutor.generated.resources.home_link_hint
import com.lukeneedham.lyrictutor.generated.resources.home_open_song
import com.lukeneedham.lyrictutor.generated.resources.home_subtitle
import com.lukeneedham.lyrictutor.generated.resources.home_title
import com.lukeneedham.lyrictutor.generated.resources.settings_title
import com.lukeneedham.lyrictutor.ui.theme.AppTheme
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** All of the page's UI. `internal`: the screenshots of the app use it too. */
@Composable
internal fun HomePageContent(
    state: HomeUiState,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val theme = AppTheme.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background)
            .safeDrawingPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BasicText(
            text = stringResource(Res.string.home_title),
            style = TextStyle(
                color = theme.onBackground,
                fontFamily = theme.fontFamily,
                fontSize = 28.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        BasicText(
            text = stringResource(Res.string.home_subtitle),
            style = TextStyle(
                color = theme.onBackground,
                fontFamily = theme.fontFamily,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            ),
        )

        val textStyle = TextStyle(
            color = theme.onSurface,
            fontFamily = theme.fontFamily,
            fontSize = 16.sp,
            lineHeight = 24.sp,
        )
        BasicTextField(
            value = state.input,
            onValueChange = onInputChange,
            textStyle = textStyle,
            cursorBrush = SolidColor(theme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { onSubmit() }),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .background(theme.surface, RoundedCornerShape(12.dp))
                        .border(1.dp, if (state.showInvalidLink) theme.error else theme.outline, RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (state.input.isEmpty()) {
                        BasicText(
                            text = stringResource(Res.string.home_link_hint),
                            style = textStyle.copy(color = theme.outline),
                        )
                    }
                    innerTextField()
                }
            },
        )
        if (state.showInvalidLink) {
            BasicText(
                text = stringResource(Res.string.home_invalid_link),
                style = TextStyle(
                    color = theme.error,
                    fontFamily = theme.fontFamily,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                ),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .background(theme.primary, RoundedCornerShape(24.dp))
                .clickable(onClick = onSubmit),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                text = stringResource(Res.string.home_open_song),
                style = TextStyle(
                    color = theme.onPrimary,
                    fontFamily = theme.fontFamily,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }

        Box(
            modifier = Modifier
                .heightIn(min = 48.dp)
                .clickable(onClick = onOpenSettings)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicText(
                text = stringResource(Res.string.settings_title),
                style = TextStyle(
                    color = theme.primary,
                    fontFamily = theme.fontFamily,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                ),
            )
        }
    }
}

/** For Android Studio while building the page. */
@Preview
@Composable
private fun Preview() {
    AppTheme {
        HomePageContent(state = HomeUiState(), onInputChange = {}, onSubmit = {}, onOpenSettings = {})
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
            onOpenSettings = {},
        )
    }
}
