package com.lukeneedham.lyrictutor.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lukeneedham.lyrictutor.data.annotation.AnnotationException
import com.lukeneedham.lyrictutor.data.annotation.ModelCatalog
import com.lukeneedham.lyrictutor.data.settings.AiProvider
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
    private val modelCatalogs: Map<AiProvider, ModelCatalog>,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState(isDebugEnabled = isDebugEnabled))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = settingsStore.load()
            _uiState.update {
                it.copy(
                    provider = settings.provider,
                    geminiApiKey = settings.geminiApiKey,
                    geminiModel = settings.geminiModel,
                    deepSeekApiKey = settings.deepSeekApiKey,
                    deepSeekModel = settings.deepSeekModel,
                )
            }
        }
    }

    /** Switches the service being edited: the other service's key and model are kept as they are. */
    fun onProviderChange(provider: AiProvider) = _uiState.update {
        it.copy(provider = provider, isSaved = false, models = ModelListUiState.Idle)
    }

    /** Edits the key of the chosen service. */
    fun onApiKeyChange(apiKey: String) = _uiState.update {
        if (it.provider == AiProvider.DeepSeek) it.copy(deepSeekApiKey = apiKey, isSaved = false) else it.copy(geminiApiKey = apiKey, isSaved = false)
    }

    fun onModelChange(model: String) = _uiState.update {
        if (it.provider == AiProvider.DeepSeek) it.copy(deepSeekModel = model, isSaved = false) else it.copy(geminiModel = model, isSaved = false)
    }

    /** Takes a model of the list: it is only kept when the user saves, like the other edits. */
    fun onPickModel(model: String) {
        onModelChange(model)
        _uiState.update { it.copy(models = ModelListUiState.Idle) }
    }

    fun onDismissModels() = _uiState.update { it.copy(models = ModelListUiState.Idle) }

    fun onShowModels() {
        _uiState.update { it.copy(models = ModelListUiState.Loading) }
        viewModelScope.launch {
            val models = modelCatalogs.getValue(_uiState.value.provider).list(_uiState.value.apiKey.trim()).fold(
                onSuccess = { ModelListUiState.Loaded(it) },
                onFailure = {
                    when (it) {
                        is AnnotationException.MissingKey -> ModelListUiState.MissingKey
                        is AnnotationException.InvalidKey -> ModelListUiState.InvalidKey
                        else -> ModelListUiState.Failed
                    }
                },
            )
            _uiState.update { it.copy(models = models) }
        }
    }

    fun onSave() {
        val state = _uiState.value
        val settings = AiSettings(
            provider = state.provider,
            geminiApiKey = state.geminiApiKey.trim(),
            geminiModel = state.geminiModel.trim().ifEmpty { AiProvider.Gemini.defaultModel },
            deepSeekApiKey = state.deepSeekApiKey.trim(),
            deepSeekModel = state.deepSeekModel.trim().ifEmpty { AiProvider.DeepSeek.defaultModel },
        )
        viewModelScope.launch {
            settingsStore.save(settings)
            _uiState.update {
                it.copy(
                    geminiApiKey = settings.geminiApiKey,
                    geminiModel = settings.geminiModel,
                    deepSeekApiKey = settings.deepSeekApiKey,
                    deepSeekModel = settings.deepSeekModel,
                    isSaved = true,
                )
            }
        }
    }
}
