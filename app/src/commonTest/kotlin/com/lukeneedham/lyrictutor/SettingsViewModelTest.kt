package com.lukeneedham.lyrictutor

import com.lukeneedham.lyrictutor.data.annotation.AnnotationException
import com.lukeneedham.lyrictutor.data.annotation.ModelCatalog
import com.lukeneedham.lyrictutor.data.settings.AiProvider
import com.lukeneedham.lyrictutor.data.settings.AiSettings
import com.lukeneedham.lyrictutor.data.settings.SettingsStore
import com.lukeneedham.lyrictutor.ui.ModelListUiState
import com.lukeneedham.lyrictutor.ui.SettingsViewModel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

// Names without spaces: a test of commonTest also runs on the targets that do not allow them
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private class FakeSettingsStore(var settings: AiSettings = AiSettings()) : SettingsStore {
        override suspend fun load() = settings
        override suspend fun save(settings: AiSettings) { this.settings = settings }
    }

    private class FakeModelCatalog(private val result: Result<List<String>> = Result.success(emptyList())) : ModelCatalog {
        var asked: String? = null
        override suspend fun list(apiKey: String): Result<List<String>> {
            asked = apiKey
            return result
        }
    }

    private fun catalogs(catalog: ModelCatalog) = AiProvider.entries.associateWith { catalog }

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun theStateSaysWhetherDebuggingIsOn() {
        assertEquals(true, SettingsViewModel(isDebugEnabled = true, settingsStore = FakeSettingsStore(), modelCatalogs = catalogs(FakeModelCatalog())).uiState.value.isDebugEnabled)
        assertEquals(false, SettingsViewModel(isDebugEnabled = false, settingsStore = FakeSettingsStore(), modelCatalogs = catalogs(FakeModelCatalog())).uiState.value.isDebugEnabled)
    }

    @Test
    fun theStateStartsWithTheSavedSettings() {
        val store = FakeSettingsStore(AiSettings(geminiApiKey = "key", geminiModel = "model"))
        val state = SettingsViewModel(isDebugEnabled = false, settingsStore = store, modelCatalogs = catalogs(FakeModelCatalog())).uiState.value
        assertEquals("key", state.apiKey)
        assertEquals("model", state.model)
        assertEquals(AiProvider.Gemini, state.provider)
    }

    @Test
    fun savingTrimsTheTextAndFallsBackToTheDefaultModel() {
        val store = FakeSettingsStore()
        val viewModel = SettingsViewModel(isDebugEnabled = false, settingsStore = store, modelCatalogs = catalogs(FakeModelCatalog()))
        viewModel.onApiKeyChange("  key \n")
        viewModel.onModelChange("  ")
        viewModel.onSave()

        assertEquals(AiSettings(geminiApiKey = "key", geminiModel = AiProvider.Gemini.defaultModel), store.settings)
        assertTrue(viewModel.uiState.value.isSaved)
    }

    @Test
    fun theModelsAreAskedForWithTheKeyBeingEdited() {
        val catalog = FakeModelCatalog(Result.success(listOf("gemini-a", "gemini-b")))
        val viewModel = SettingsViewModel(isDebugEnabled = false, settingsStore = FakeSettingsStore(), modelCatalogs = catalogs(catalog))
        viewModel.onApiKeyChange(" key ")
        viewModel.onShowModels()

        assertEquals("key", catalog.asked)
        assertEquals(ModelListUiState.Loaded(listOf("gemini-a", "gemini-b")), viewModel.uiState.value.models)
    }

    @Test
    fun aMissingOrRefusedKeyIsToldApart() {
        val missing = SettingsViewModel(false, FakeSettingsStore(), catalogs(FakeModelCatalog(Result.failure(AnnotationException.MissingKey()))))
        missing.onShowModels()
        assertEquals(ModelListUiState.MissingKey, missing.uiState.value.models)

        val refused = SettingsViewModel(false, FakeSettingsStore(), catalogs(FakeModelCatalog(Result.failure(AnnotationException.InvalidKey()))))
        refused.onShowModels()
        assertEquals(ModelListUiState.InvalidKey, refused.uiState.value.models)

        val broken = SettingsViewModel(false, FakeSettingsStore(), catalogs(FakeModelCatalog(Result.failure(AnnotationException.Failed("x")))))
        broken.onShowModels()
        assertEquals(ModelListUiState.Failed, broken.uiState.value.models)
    }

    @Test
    fun pickingAModelFillsTheFieldAndClosesTheList() {
        val catalog = FakeModelCatalog(Result.success(listOf("gemini-a")))
        val viewModel = SettingsViewModel(isDebugEnabled = false, settingsStore = FakeSettingsStore(), modelCatalogs = catalogs(catalog))
        viewModel.onShowModels()
        viewModel.onPickModel("gemini-a")

        assertEquals("gemini-a", viewModel.uiState.value.model)
        assertEquals(ModelListUiState.Idle, viewModel.uiState.value.models)
    }

    @Test
    fun eachProviderKeepsItsOwnKeyAndModel() {
        val store = FakeSettingsStore()
        val viewModel = SettingsViewModel(isDebugEnabled = false, settingsStore = store, modelCatalogs = catalogs(FakeModelCatalog()))
        viewModel.onApiKeyChange("gemini-key")
        viewModel.onProviderChange(AiProvider.DeepSeek)
        assertEquals("", viewModel.uiState.value.apiKey)
        viewModel.onApiKeyChange(" deepseek-key ")
        viewModel.onModelChange("deepseek-x")
        viewModel.onSave()

        assertEquals(
            AiSettings(
                provider = AiProvider.DeepSeek,
                geminiApiKey = "gemini-key",
                geminiModel = AiProvider.Gemini.defaultModel,
                deepSeekApiKey = "deepseek-key",
                deepSeekModel = "deepseek-x",
            ),
            store.settings,
        )
    }

    @Test
    fun theModelsAreAskedOfTheChosenProvider() {
        val gemini = FakeModelCatalog()
        val deepSeek = FakeModelCatalog(Result.success(listOf("deepseek-a")))
        val viewModel = SettingsViewModel(
            isDebugEnabled = false,
            settingsStore = FakeSettingsStore(),
            modelCatalogs = mapOf(AiProvider.Gemini to gemini, AiProvider.DeepSeek to deepSeek),
        )
        viewModel.onProviderChange(AiProvider.DeepSeek)
        viewModel.onApiKeyChange("k")
        viewModel.onShowModels()

        assertEquals(null, gemini.asked)
        assertEquals("k", deepSeek.asked)
        assertEquals(ModelListUiState.Loaded(listOf("deepseek-a")), viewModel.uiState.value.models)
    }
}
