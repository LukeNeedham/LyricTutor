package com.lukeneedham.lyrictutor.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lukeneedham.lyrictutor.data.settings.AiSettings
import com.lukeneedham.lyrictutor.data.settings.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    isDebugEnabled: Boolean,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState(isDebugEnabled = isDebugEnabled))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = settingsStore.load()
            _uiState.update { it.copy(apiKey = settings.apiKey, model = settings.model) }
        }
    }

    fun onApiKeyChange(apiKey: String) = _uiState.update { it.copy(apiKey = apiKey, isSaved = false) }

    fun onModelChange(model: String) = _uiState.update { it.copy(model = model, isSaved = false) }

    fun onSave() {
        val state = _uiState.value
        val settings = AiSettings(
            apiKey = state.apiKey.trim(),
            model = state.model.trim().ifEmpty { AiSettings.DEFAULT_MODEL },
        )
        viewModelScope.launch {
            settingsStore.save(settings)
            _uiState.update { it.copy(apiKey = settings.apiKey, model = settings.model, isSaved = true) }
        }
    }
}
