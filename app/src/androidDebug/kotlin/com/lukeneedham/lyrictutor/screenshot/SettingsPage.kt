package com.lukeneedham.lyrictutor.screenshot

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.lukeneedham.lyrictutor.data.settings.AiProvider
import com.lukeneedham.lyrictutor.data.settings.ProviderSettings
import com.lukeneedham.lyrictutor.ui.SettingsPageContent
import com.lukeneedham.lyrictutor.ui.SettingsUiState
import com.lukeneedham.lyrictutor.ui.theme.AppTheme

/** Rendered by CI to docs/screenshots/<position in order.txt>-SettingsPage.png. */
@Preview
@Composable
private fun SettingsPageScreenshot() {
    val state = SettingsUiState(isDebugEnabled = true, providers = mapOf(AiProvider.Gemini to ProviderSettings("AIzaSyExampleKey", "gemini-3.8-flash")))
    AppTheme {
        SettingsPageContent(state = state, onBack = {}, onProviderChange = {}, onApiKeyChange = {}, onModelChange = {}, onUseCodingPlanChange = {}, onShowModels = {}, onDismissModels = {}, onPickModel = {}, onSave = {}, onOpenDebug = {})
    }
}
