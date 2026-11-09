package com.lukeneedham.lyrictutor.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lukeneedham.lyrictutor.data.annotation.AnnotationException
import com.lukeneedham.lyrictutor.data.annotation.ModelCatalogs
import com.lukeneedham.lyrictutor.data.settings.AiProvider
import com.lukeneedham.lyrictutor.data.settings.AiSettings
import com.lukeneedham.lyrictutor.data.settings.ProviderSettings
import com.lukeneedham.lyrictutor.data.settings.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    isDebugEnabled: Boolean,
    private val settingsStore: SettingsStore,
    private val modelCatalogs: ModelCatalogs,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState(isDebugEnabled = isDebugEnabled))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = settingsStore.load()
            _uiState.update { it.copy(provider = settings.provider, providers = settings.providers) }
        }
    }

    /** Switches the service being edited: the other services' keys and models are kept as they are. */
    fun onProviderChange(provider: AiProvider) = _uiState.update {
        it.copy(provider = provider, isSaved = false, models = ModelListUiState.Idle)
    }

    /** Edits the key of the chosen service. */
    fun onApiKeyChange(apiKey: String) = edit { it.copy(apiKey = apiKey) }

    fun onModelChange(model: String) = edit { it.copy(model = model) }

    fun onUseCodingPlanChange(useCodingPlan: Boolean) = edit { it.copy(useCodingPlan = useCodingPlan) }

    private fun edit(change: (ProviderSettings) -> ProviderSettings) = _uiState.update {
        it.copy(providers = it.providers + (it.provider to change(it.of(it.provider))), isSaved = false)
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
            val models = modelCatalogs.of(_uiState.value.provider).list(_uiState.value.apiKey.trim(), _uiState.value.useCodingPlan).fold(
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
            providers = AiProvider.entries.associateWith { provider ->
                val edited = state.of(provider)
                ProviderSettings(
                    apiKey = edited.apiKey.trim(),
                    model = edited.model.trim().ifEmpty { provider.defaultModel },
                    useCodingPlan = edited.useCodingPlan,
                )
            },
        )
        viewModelScope.launch {
            settingsStore.save(settings)
            _uiState.update { it.copy(providers = settings.providers, isSaved = true) }
        }
    }
}
