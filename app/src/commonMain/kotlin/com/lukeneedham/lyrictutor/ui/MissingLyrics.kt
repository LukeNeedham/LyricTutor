package com.lukeneedham.lyrictutor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukeneedham.lyrictutor.generated.resources.Res
import com.lukeneedham.lyrictutor.generated.resources.lyrics_error
import com.lukeneedham.lyrictutor.generated.resources.lyrics_not_found
import com.lukeneedham.lyrictutor.generated.resources.lyrics_not_found_hint
import com.lukeneedham.lyrictutor.generated.resources.lyrics_paste
import com.lukeneedham.lyrictutor.generated.resources.lyrics_paste_cancel
import com.lukeneedham.lyrictutor.generated.resources.lyrics_paste_hint
import com.lukeneedham.lyrictutor.generated.resources.lyrics_paste_save
import com.lukeneedham.lyrictutor.generated.resources.lyrics_retry
import com.lukeneedham.lyrictutor.ui.components.AppButton
import com.lukeneedham.lyrictutor.ui.components.AppButtonStyle
import com.lukeneedham.lyrictutor.ui.components.AppIcon
import com.lukeneedham.lyrictutor.ui.components.AppIconImage
import com.lukeneedham.lyrictutor.ui.components.AppText
import com.lukeneedham.lyrictutor.ui.components.AppTextField
import com.lukeneedham.lyrictutor.ui.theme.AppTheme
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * What takes the place of the lyrics when there are none: the reason, and the ways on (try again, or paste them). [pasteText] is the text
 * in the paste box, or null while the box is closed.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MissingLyrics(
    isError: Boolean,
    pasteText: String?,
    onRetryLyrics: () -> Unit,
    onOpenPaste: () -> Unit,
    onPasteChange: (String) -> Unit,
    onSavePaste: () -> Unit,
    onClosePaste: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = AppTheme.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(theme.surface)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(56.dp).clip(CircleShape).background(if (isError) theme.error.copy(alpha = 0.12f) else theme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            AppIconImage(icon = AppIcon.Note, tint = if (isError) theme.error else theme.onPrimaryContainer)
        }
        AppText(
            text = stringResource(if (isError) Res.string.lyrics_error else Res.string.lyrics_not_found),
            color = if (isError) theme.error else theme.onSurface,
            size = 17.sp,
            weight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        if (!isError && pasteText == null) {
            AppText(
                text = stringResource(Res.string.lyrics_not_found_hint),
                color = theme.onSurfaceVariant,
                size = 14.sp,
                textAlign = TextAlign.Center,
            )
        }
        if (pasteText == null) {
            FlowRow(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (isError) {
                    AppButton(text = stringResource(Res.string.lyrics_retry), onClick = onRetryLyrics, style = AppButtonStyle.Tonal, icon = AppIcon.Refresh)
                }
                AppButton(text = stringResource(Res.string.lyrics_paste), onClick = onOpenPaste)
            }
        } else {
            AppTextField(
                value = pasteText,
                onValueChange = onPasteChange,
                placeholder = stringResource(Res.string.lyrics_paste_hint),
                singleLine = false,
                minLines = 8,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AppButton(text = stringResource(Res.string.lyrics_paste_save), onClick = onSavePaste, icon = AppIcon.Check)
                AppButton(text = stringResource(Res.string.lyrics_paste_cancel), onClick = onClosePaste, style = AppButtonStyle.Plain)
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    AppTheme {
        MissingLyrics(
            isError = false,
            pasteText = null,
            onRetryLyrics = {},
            onOpenPaste = {},
            onPasteChange = {},
            onSavePaste = {},
            onClosePaste = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview
@Composable
private fun PreviewPasting() {
    AppTheme {
        MissingLyrics(
            isError = false,
            pasteText = "",
            onRetryLyrics = {},
            onOpenPaste = {},
            onPasteChange = {},
            onSavePaste = {},
            onClosePaste = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
