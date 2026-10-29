package com.lukeneedham.lyrictutor

import com.lukeneedham.lyrictutor.data.settings.AiSettings
import com.lukeneedham.lyrictutor.data.settings.SettingsStore
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

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun theStateSaysWhetherDebuggingIsOn() {
        assertEquals(true, SettingsViewModel(isDebugEnabled = true, settingsStore = FakeSettingsStore()).uiState.value.isDebugEnabled)
        assertEquals(false, SettingsViewModel(isDebugEnabled = false, settingsStore = FakeSettingsStore()).uiState.value.isDebugEnabled)
    }

    @Test
    fun theStateStartsWithTheSavedSettings() {
        val store = FakeSettingsStore(AiSettings(apiKey = "key", model = "model"))
        val state = SettingsViewModel(isDebugEnabled = false, settingsStore = store).uiState.value
        assertEquals("key", state.apiKey)
        assertEquals("model", state.model)
    }

    @Test
    fun savingTrimsTheTextAndFallsBackToTheDefaultModel() {
        val store = FakeSettingsStore()
        val viewModel = SettingsViewModel(isDebugEnabled = false, settingsStore = store)
        viewModel.onApiKeyChange("  key \n")
        viewModel.onModelChange("  ")
        viewModel.onSave()

        assertEquals(AiSettings(apiKey = "key", model = AiSettings.DEFAULT_MODEL), store.settings)
        assertTrue(viewModel.uiState.value.isSaved)
    }
}
