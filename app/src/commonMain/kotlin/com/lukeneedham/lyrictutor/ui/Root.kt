package com.lukeneedham.lyrictutor.ui

import androidx.compose.runtime.Composable
import com.lukeneedham.lyrictutor.ui.theme.AppTheme
import com.lukeneedham.lyrictutor.ui.theme.toDebugTheme
import com.lukeneedham.common.ui.ProvideDebugSettings

/** The one composable `MainActivity` calls: everything the UI sits in, then the navigation. */
@Composable
fun Root() {
    AppTheme {
        ProvideDebugSettings(theme = AppTheme.current.toDebugTheme()) {
            AppNavigation()
        }
    }
}
