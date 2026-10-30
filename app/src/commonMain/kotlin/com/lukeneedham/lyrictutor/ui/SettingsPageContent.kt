package com.lukeneedham.lyrictutor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukeneedham.common.ui.DebugButton
import com.lukeneedham.lyrictutor.generated.resources.Res
import com.lukeneedham.lyrictutor.generated.resources.settings_about_title
import com.lukeneedham.lyrictutor.generated.resources.settings_ai_intro
import com.lukeneedham.lyrictutor.generated.resources.settings_ai_title
import com.lukeneedham.lyrictutor.generated.resources.settings_api_key_label
import com.lukeneedham.lyrictutor.generated.resources.settings_api_key_note
import com.lukeneedham.lyrictutor.generated.resources.settings_attribution
import com.lukeneedham.lyrictutor.generated.resources.settings_key_hide
import com.lukeneedham.lyrictutor.generated.resources.settings_key_show
import com.lukeneedham.lyrictutor.generated.resources.settings_model_label
import com.lukeneedham.lyrictutor.generated.resources.settings_models_failed
import com.lukeneedham.lyrictutor.generated.resources.settings_models_invalid_key
import com.lukeneedham.lyrictutor.generated.resources.settings_models_loading
import com.lukeneedham.lyrictutor.generated.resources.settings_models_need_key
import com.lukeneedham.lyrictutor.generated.resources.settings_models_none
import com.lukeneedham.lyrictutor.generated.resources.settings_models_show
import com.lukeneedham.lyrictutor.generated.resources.settings_save
import com.lukeneedham.lyrictutor.generated.resources.settings_saved
import com.lukeneedham.lyrictutor.generated.resources.settings_title
import com.lukeneedham.lyrictutor.ui.components.AppButton
import com.lukeneedham.lyrictutor.ui.components.AppButtonStyle
import com.lukeneedham.lyrictutor.ui.components.AppIcon
import com.lukeneedham.lyrictutor.ui.components.AppIconButton
import com.lukeneedham.lyrictutor.ui.components.AppToolbar
import com.lukeneedham.lyrictutor.ui.components.navigationBarHeight
import com.lukeneedham.lyrictutor.ui.components.sideInsetsPadding
import com.lukeneedham.lyrictutor.ui.components.AppIconImage
import com.lukeneedham.lyrictutor.ui.components.AppText
import com.lukeneedham.lyrictutor.ui.components.AppTextField
import com.lukeneedham.lyrictutor.ui.components.Spinner
import com.lukeneedham.lyrictutor.ui.theme.AppTheme
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** All of the page's UI. `internal`: the screenshots of the app use it too. */
@Composable
internal fun SettingsPageContent(
    state: SettingsUiState,
    onBack: () -> Unit,
    onApiKeyChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onShowModels: () -> Unit,
    onDismissModels: () -> Unit,
    onPickModel: (String) -> Unit,
    onSave: () -> Unit,
    onOpenDebug: () -> Unit,
) {
    val theme = AppTheme.current
    // Only how the key is drawn: nothing the ViewModel needs to know
    var isKeyShown by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxSize().background(theme.background).sideInsetsPadding().imePadding()) {
        AppToolbar(title = stringResource(Res.string.settings_title), onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp + navigationBarHeight()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(title = stringResource(Res.string.settings_ai_title)) {
                AppText(text = stringResource(Res.string.settings_ai_intro), color = theme.onSurfaceVariant, size = 14.sp)
                Field(label = stringResource(Res.string.settings_api_key_label)) {
                    AppTextField(
                        value = state.apiKey,
                        onValueChange = onApiKeyChange,
                        visualTransformation = if (isKeyShown) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailing = {
                            AppIconButton(
                                icon = if (isKeyShown) AppIcon.EyeOff else AppIcon.Eye,
                                contentDescription = stringResource(if (isKeyShown) Res.string.settings_key_hide else Res.string.settings_key_show),
                                onClick = { isKeyShown = !isKeyShown },
                                tint = theme.onSurfaceVariant,
                            )
                        },
                    )
                    AppText(text = stringResource(Res.string.settings_api_key_note), color = theme.onSurfaceVariant, size = 13.sp)
                }
                Field(label = stringResource(Res.string.settings_model_label)) {
                    AppTextField(value = state.model, onValueChange = onModelChange)
                    ModelButton(state = state.models, onShow = onShowModels)
                }
                AppButton(
                    text = stringResource(if (state.isSaved) Res.string.settings_saved else Res.string.settings_save),
                    onClick = onSave,
                    icon = if (state.isSaved) AppIcon.Check else null,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Card(title = stringResource(Res.string.settings_about_title)) {
                AppText(text = stringResource(Res.string.settings_attribution), color = theme.onSurfaceVariant, size = 14.sp)
            }
            // Draws nothing unless debugging is on, so it stays in the layout for release builds
            if (state.isDebugEnabled) DebugButton(onClick = onOpenDebug)
        }
    }
    (state.models as? ModelListUiState.Loaded)?.let {
        ModelSheet(models = it.models, selected = state.model, onPick = onPickModel, onDismiss = onDismissModels)
    }
}

@Composable
private fun Card(title: String, content: @Composable () -> Unit) {
    val theme = AppTheme.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(theme.surface)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AppText(text = title, size = 18.sp, weight = FontWeight.Bold)
        content()
    }
}

@Composable
private fun Field(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AppText(text = label, size = 14.sp, weight = FontWeight.SemiBold)
        content()
    }
}

/** The button that fetches the models of the key: while it does, a spinner; if it cannot, why. The models themselves are in [ModelSheet]. */
@Composable
private fun ModelButton(state: ModelListUiState, onShow: () -> Unit) {
    val theme = AppTheme.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (state == ModelListUiState.Loading) {
            Row(
                modifier = Modifier.heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Spinner(diameter = 18.dp)
                AppText(text = stringResource(Res.string.settings_models_loading), color = theme.onSurfaceVariant, size = 14.sp)
            }
        } else {
            val problem = when (state) {
                ModelListUiState.MissingKey -> Res.string.settings_models_need_key
                ModelListUiState.InvalidKey -> Res.string.settings_models_invalid_key
                ModelListUiState.Failed -> Res.string.settings_models_failed
                else -> null
            }
            problem?.let { AppText(text = stringResource(it), color = theme.error, size = 14.sp) }
            AppButton(
                text = stringResource(Res.string.settings_models_show),
                onClick = onShow,
                style = AppButtonStyle.Tonal,
            )
        }
    }
}

/** A bottom sheet with a row for each model of the key: tapping one picks it and closes the sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelSheet(models: List<String>, selected: String, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    val theme = AppTheme.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = theme.surface,
        contentColor = theme.onSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp + navigationBarHeight()),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (models.isEmpty()) {
                AppText(text = stringResource(Res.string.settings_models_none), color = theme.error, size = 14.sp)
            }
            for (model in models) {
                val isSelected = model == selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) theme.primaryContainer else theme.surfaceVariant)
                        .clickable(role = Role.RadioButton) { onPick(model) }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AppText(
                        text = model,
                        color = if (isSelected) theme.onPrimaryContainer else theme.onSurface,
                        size = 15.sp,
                        weight = if (isSelected) FontWeight.SemiBold else null,
                        modifier = Modifier.weight(1f),
                    )
                    if (isSelected) AppIconImage(icon = AppIcon.Check, tint = theme.onPrimaryContainer, iconSize = 20.dp)
                }
            }
        }
    }
}

/** For Android Studio while building the page: debugging on. */
@Preview
@Composable
private fun Preview() {
    AppTheme {
        SettingsPageContent(
            state = SettingsUiState(isDebugEnabled = true, apiKey = "AIzaSyExampleKey", model = "gemini-3.8-flash"),
            onBack = {},
            onApiKeyChange = {},
            onModelChange = {},
            onShowModels = {},
            onDismissModels = {},
            onPickModel = {},
            onSave = {},
            onOpenDebug = {},
        )
    }
}

@Preview
@Composable
private fun PreviewModels() {
    AppTheme {
        SettingsPageContent(
            state = SettingsUiState(
                isDebugEnabled = false,
                apiKey = "AIzaSyExampleKey",
                model = "gemini-3.8-flash",
                isSaved = true,
                models = ModelListUiState.Loaded(listOf("gemini-3.8-flash", "gemini-3.8-pro")),
            ),
            onBack = {},
            onApiKeyChange = {},
            onModelChange = {},
            onShowModels = {},
            onDismissModels = {},
            onPickModel = {},
            onSave = {},
            onOpenDebug = {},
        )
    }
}
