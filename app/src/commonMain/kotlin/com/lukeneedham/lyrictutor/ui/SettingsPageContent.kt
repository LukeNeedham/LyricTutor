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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukeneedham.common.ui.DebugButton
import com.lukeneedham.lyrictutor.generated.resources.Res
import com.lukeneedham.lyrictutor.generated.resources.settings_ai_title
import com.lukeneedham.lyrictutor.generated.resources.settings_api_key_label
import com.lukeneedham.lyrictutor.generated.resources.settings_api_key_note
import com.lukeneedham.lyrictutor.generated.resources.settings_attribution
import com.lukeneedham.lyrictutor.generated.resources.settings_model_label
import com.lukeneedham.lyrictutor.generated.resources.settings_save
import com.lukeneedham.lyrictutor.generated.resources.settings_saved
import com.lukeneedham.lyrictutor.generated.resources.settings_title
import com.lukeneedham.lyrictutor.ui.theme.AppTheme
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** All of the page's UI. `internal`: the screenshots of the app use it too. */
@Composable
internal fun SettingsPageContent(
    state: SettingsUiState,
    onApiKeyChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onSave: () -> Unit,
    onOpenDebug: () -> Unit,
) {
    val theme = AppTheme.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BasicText(
            text = stringResource(Res.string.settings_title),
            style = TextStyle(
                color = theme.onBackground,
                fontFamily = theme.fontFamily,
                fontSize = 24.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        BasicText(
            text = stringResource(Res.string.settings_ai_title),
            style = TextStyle(
                color = theme.onBackground,
                fontFamily = theme.fontFamily,
                fontSize = 18.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        Field(
            label = stringResource(Res.string.settings_api_key_label),
            value = state.apiKey,
            onValueChange = onApiKeyChange,
            visualTransformation = PasswordVisualTransformation(),
            keyboardType = KeyboardType.Password,
        )
        Field(
            label = stringResource(Res.string.settings_model_label),
            value = state.model,
            onValueChange = onModelChange,
        )
        BasicText(
            text = stringResource(Res.string.settings_api_key_note),
            style = TextStyle(color = theme.outline, fontFamily = theme.fontFamily, fontSize = 14.sp, lineHeight = 20.sp),
        )
        Box(
            modifier = Modifier
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(theme.primary)
                .clickable(onClick = onSave)
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                text = stringResource(if (state.isSaved) Res.string.settings_saved else Res.string.settings_save),
                style = TextStyle(
                    color = theme.onPrimary,
                    fontFamily = theme.fontFamily,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
        BasicText(
            text = stringResource(Res.string.settings_attribution),
            style = TextStyle(color = theme.outline, fontFamily = theme.fontFamily, fontSize = 14.sp, lineHeight = 20.sp),
        )
        // Draws nothing unless debugging is on, so it stays in the layout for release builds
        if (state.isDebugEnabled) DebugButton(onClick = onOpenDebug)
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    val theme = AppTheme.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BasicText(
            text = label,
            style = TextStyle(color = theme.onBackground, fontFamily = theme.fontFamily, fontSize = 14.sp, lineHeight = 20.sp),
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            visualTransformation = visualTransformation,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            textStyle = TextStyle(color = theme.onSurface, fontFamily = theme.fontFamily, fontSize = 16.sp, lineHeight = 24.sp),
            cursorBrush = SolidColor(theme.primary),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .background(theme.surface, RoundedCornerShape(12.dp))
                        .border(1.dp, theme.outline, RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.CenterStart,
                ) { innerTextField() }
            },
        )
    }
}

/** For Android Studio while building the page: debugging on. */
@Preview
@Composable
private fun Preview() {
    AppTheme {
        SettingsPageContent(
            state = SettingsUiState(isDebugEnabled = true, apiKey = "AIzaSyExampleKey", model = "gemini-2.5-flash"),
            onApiKeyChange = {},
            onModelChange = {},
            onSave = {},
            onOpenDebug = {},
        )
    }
}
