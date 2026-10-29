package com.lukeneedham.lyrictutor.di

import com.lukeneedham.lyrictutor.ui.SettingsViewModel
import com.lukeneedham.common.DebugKit
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    viewModel { SettingsViewModel(isDebugEnabled = DebugKit.enabled) }
}
